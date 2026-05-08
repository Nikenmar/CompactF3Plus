package net.nikenmar.compactf3plus;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Mod("compactf3plus")
public class CompactF3Plus {
    public CompactF3Plus(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, CompactF3PlusConfig.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new CompactF3PlusConfigScreen(parent));
        modBus.addListener(HudRenderer::onRegisterKeyMappings);
        // RenderGuiEvent.Pre fires before vanilla draws the HUD — we detect the F3
        // toggle and force the overlay back off here so vanilla never gets a chance
        // to render its own debug screen this frame (no 1-frame flicker).
        NeoForge.EVENT_BUS.addListener(HudRenderer::onRenderGuiPre);
        NeoForge.EVENT_BUS.addListener(HudRenderer::onRenderGui);
        // showGizmo is currently inert on this branch (vanilla 3D gizmo requires a
        // CameraRenderState we don't yet plumb through) — flag retained for forward
        // compatibility once we render our own gizmo.
        NeoForge.EVENT_BUS.addListener(HudRenderer::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(HudRenderer::onSoundPlay);
    }

    private static final class HudRenderer {
        private static boolean compactHudEnabled = false;
        // True when the active HUD session was opened by pressing F3 (replaceF3 mode).
        // F8 and enabledByDefault do not set this — they open the HUD in "plain" mode
        // without the 3D gizmo even if showGizmo is on.
        private static boolean compactHudOpenedViaF3 = false;
        private static boolean wasDebugShowing = false;
        private static SoundInstance currentMusicInstance = null;
        private static String currentMusicTrackName = "";

        // While replaceF3 is on we OWN the THREE_DIMENSIONAL_CROSSHAIR debug entry
        // unconditionally (ALWAYS_ON when our gizmo is wanted, NEVER otherwise).
        // setStatus persists to disk, so we only call it on transitions. Initial
        // value `true` forces a setStatus(NEVER) on the first frame after world entry,
        // which clears any stale ALWAYS_ON left over from a previous session crash.
        // Trade-off: while replaceF3=on, the user cannot independently keep the
        // vanilla 3D crosshair always-on — that's by design (replaceF3=off if they want).
        private static boolean lastGizmoApplied = true;
        private static final int AVG_FPS_SECONDS = 60;
        private static final LinkedList<Integer> fpsHistory = new LinkedList<>();
        private static long lastFpsSampleTime = 0;

        private static final int STUTTER_HISTORY_SIZE = 600;
        private static final double[] frameTimesBuf = new double[STUTTER_HISTORY_SIZE];
        private static int frameTimeIdx = 0;
        private static int framesCollected = 0;
        private static long lastFrameTimeNano = System.nanoTime();

        private static final long sessionStartTime = System.currentTimeMillis();
        // Constructed directly via the record constructor; registered with NeoForge
        // through RegisterKeyMappingsEvent#registerCategory in onRegisterKeyMappings
        // (KeyMapping.Category.register(Identifier) is deprecated in 26.1).
        private static final KeyMapping.Category COMPACT_F3_CATEGORY =
                new KeyMapping.Category(Identifier.fromNamespaceAndPath("compactf3plus", "main"));
        private static final KeyMapping TOGGLE_HUD = new KeyMapping(
                "key.compactf3plus.toggleHud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                COMPACT_F3_CATEGORY);

        // Reusable Object Pools
        private static final List<HudLine> lines = new ArrayList<>();
        private static int currentLineIndex = 0;

        private static class TextSegment {
            String text;
            int color;

            TextSegment(String text, int color) {
                this.text = text;
                this.color = color;
            }

            void set(String text, int color) {
                this.text = text;
                this.color = color;
            }
        }

        private static class HudLine {
            final List<TextSegment> segments = new ArrayList<>();
            int currentSegmentIndex = 0;

            void reset() {
                currentSegmentIndex = 0;
            }

            void addSegment(String text, int color) {
                if (currentSegmentIndex < segments.size()) {
                    segments.get(currentSegmentIndex).set(text, color);
                } else {
                    segments.add(new TextSegment(text, color));
                }
                currentSegmentIndex++;
            }

            void addSegment(String text) {
                addSegment(text, 0xFFFFFF);
            }
        }

        private static HudLine nextLine() {
            if (currentLineIndex < lines.size()) {
                HudLine line = lines.get(currentLineIndex);
                line.reset();
                currentLineIndex++;
                return line;
            } else {
                HudLine line = new HudLine();
                lines.add(line);
                currentLineIndex++;
                return line;
            }
        }

        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.registerCategory(COMPACT_F3_CATEGORY);
            event.register(TOGGLE_HUD);
        }

        public static void onSoundPlay(PlaySoundEvent event) {
            if (event.getSound() != null && (event.getSound().getSource() == SoundSource.MUSIC || event.getSound().getSource() == SoundSource.RECORDS)) {
                currentMusicInstance = event.getSound();
                currentMusicTrackName = "Unknown";
            }
        }

        private static String formatTrackName(String rawPath) {
            if (rawPath == null) return "Unknown";
            String[] parts = rawPath.split("[/.]");
            String fileName = parts[parts.length - 1];

            StringBuilder sb = new StringBuilder();
            for (String word : fileName.split("_")) {
                if (!word.isEmpty()) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) sb.append(word.substring(1));
                }
            }
            return sb.toString();
        }

        public static void onPlayerLogin(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            compactHudEnabled = CompactF3PlusConfig.enabledByDefault.get();
            compactHudOpenedViaF3 = false;
        }

        public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
            // Run the F3 toggle detection BEFORE any HUD rendering this frame.
            // Setting overlay visibility false here means vanilla's debug overlay
            // never renders on the same frame the F3 key was processed.
            Minecraft mc = Minecraft.getInstance();
            if (CompactF3PlusConfig.replaceF3.get()) {
                boolean debugShowing = mc.debugEntries.isOverlayVisible();
                if (debugShowing != wasDebugShowing) {
                    compactHudEnabled = !compactHudEnabled;
                    // True if F3 just opened the HUD; false if F3 just closed it.
                    compactHudOpenedViaF3 = compactHudEnabled;
                }
                if (debugShowing) {
                    mc.debugEntries.setOverlayVisible(false);
                }
                wasDebugShowing = false;
            }
            updateGizmoState(mc);
        }

        // Drives the vanilla 3D-crosshair debug entry. Vanilla GameRenderer renders
        // the gizmo when DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR is enabled,
        // independently from the main F3 overlay — so we can keep F3 hidden while the
        // gizmo shows. setStatus persists to disk, so we only call it on transitions.
        private static void updateGizmoState(Minecraft mc) {
            if (!CompactF3PlusConfig.replaceF3.get())
                return;
            boolean wantGizmo = CompactF3PlusConfig.showGizmo.get()
                    && compactHudEnabled
                    && compactHudOpenedViaF3;
            if (wantGizmo != lastGizmoApplied) {
                mc.debugEntries.setStatus(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR,
                        wantGizmo ? DebugScreenEntryStatus.ALWAYS_ON : DebugScreenEntryStatus.NEVER);
                lastGizmoApplied = wantGizmo;
            }
        }

        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (TOGGLE_HUD.consumeClick()) {
                compactHudEnabled = !compactHudEnabled;
                compactHudOpenedViaF3 = false;
            }
            LocalPlayer player = mc.player;
            if (player == null || mc.options.hideGui)
                return;

            // replaceF3=on path: state machine is driven by onRenderGuiPre. Just bail
            // if compact HUD isn't active for this frame.
            // replaceF3=off path: don't render our HUD while vanilla F3 is open.
            boolean debugShowing = mc.debugEntries.isOverlayVisible();
            if (CompactF3PlusConfig.replaceF3.get()) {
                if (!compactHudEnabled)
                    return;
            } else {
                wasDebugShowing = debugShowing;
                if (!compactHudEnabled || debugShowing)
                    return;
            }

            Font font = mc.font;
            boolean useColors = CompactF3PlusConfig.colorIndicators.get();
            currentLineIndex = 0;

            int fps = mc.getFps();
            long now2 = System.currentTimeMillis();
            if (now2 - lastFpsSampleTime >= 1000) {
                fpsHistory.add(fps);
                if (fpsHistory.size() > AVG_FPS_SECONDS)
                    fpsHistory.removeFirst();
                lastFpsSampleTime = now2;
            }
            int avgFps = 0;
            for (int f : fpsHistory)
                avgFps += f;
            avgFps = fpsHistory.isEmpty() ? fps : avgFps / fpsHistory.size();

            long nowNano = System.nanoTime();
            double frameDeltaMs = (nowNano - lastFrameTimeNano) / 1_000_000.0;
            lastFrameTimeNano = nowNano;
            if (frameDeltaMs > 0 && frameDeltaMs < 1000) {
                frameTimesBuf[frameTimeIdx] = frameDeltaMs;
                frameTimeIdx = (frameTimeIdx + 1) % STUTTER_HISTORY_SIZE;
                if (framesCollected < STUTTER_HISTORY_SIZE)
                    framesCollected++;
            }

            // FPS
            if (CompactF3PlusConfig.showFps.get()) {
                float msPerFrame = 1000f / fps;

                // We only need a minimum of say, 10 seconds of history to start calculating an
                // average effectively.
                // It will continue building up to AVG_FPS_SECONDS (60 seconds).
                if (useColors) {
                    int fpsColor;
                    if (fpsHistory.size() >= 10 && avgFps > 0) {
                        float ratio = (float) fps / (float) avgFps;
                        // green if fps is at least 80% of average
                        if (ratio >= 0.80f) {
                            fpsColor = 0x55FF55;
                        }
                        // yellow if fps is between 50% and 80% of average
                        else if (ratio >= 0.50f) {
                            fpsColor = 0xFFFF55;
                        }
                        // red if fps is below 50% of average
                        else {
                            fpsColor = 0xFF5555;
                        }
                    } else {
                        // fallback while buffer is filling
                        if (fps > 60)
                            fpsColor = 0x55FF55;
                        else if (fps >= 30)
                            fpsColor = 0xFFFF55;
                        else
                            fpsColor = 0xFF5555;
                    }

                    HudLine line = nextLine();
                    line.addSegment("FPS: ", 0xFFFFFF);
                    line.addSegment(String.valueOf(fps), fpsColor);
                    line.addSegment(" (" + avgFps + " avg) " + (Math.round(msPerFrame * 10) / 10.0) + " ms", 0xFFFFFF);
                } else {
                    nextLine().addSegment(
                            "FPS: " + fps + " (" + avgFps + " avg) " + (Math.round(msPerFrame * 10) / 10.0) + " ms");
                }
            }

            // System (RAM / Lag / TPS)
            boolean showSys = CompactF3PlusConfig.showSystem.get();
            boolean showLag = CompactF3PlusConfig.showLag.get();
            boolean showTps = CompactF3PlusConfig.showTps.get();

            if (showSys || showLag || showTps) {
                List<TextSegment> sysSegs = new ArrayList<>();
                String sysStr = "";

                if (showSys) {
                    Runtime rt = Runtime.getRuntime();
                    long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                    long maxMB = rt.maxMemory() / 1024 / 1024;

                    sysSegs.add(new TextSegment("RAM: " + usedMB + "/" + maxMB + " MB", 0xFFFFFF));
                    sysStr += "RAM: " + usedMB + "/" + maxMB + " MB";
                }

                if (showLag) {
                    int stutters = 0;
                    int framesToCheck = Math.min(framesCollected, Math.max(10, avgFps));
                    if (framesToCheck > 0 && avgFps > 0) {
                        double expectedMs = 1000.0 / avgFps;
                        double stutterThreshold = Math.max(expectedMs * 2.0, 16.6); // at least 16.6ms to be a stutter
                        int startIdx = frameTimeIdx - framesToCheck;
                        if (startIdx < 0)
                            startIdx += STUTTER_HISTORY_SIZE;
                        for (int i = 0; i < framesToCheck; i++) {
                            int idx = (startIdx + i) % STUTTER_HISTORY_SIZE;
                            if (frameTimesBuf[idx] > stutterThreshold)
                                stutters++;
                        }
                    }
                    double stutterRate = framesToCheck > 0 ? (stutters / (double) framesToCheck) * 100.0 : 0;
                    String stutterVal = (Math.round(stutterRate * 10) / 10.0) + "%";
                    int stutterColor = 0x55FF55;
                    if (stutterRate >= 5.0)
                        stutterColor = 0xFFFF55;
                    if (stutterRate >= 10.0)
                        stutterColor = 0xFF5555;

                    if (!sysSegs.isEmpty()) {
                        sysSegs.add(new TextSegment(" | ", 0xFFFFFF));
                        sysStr += " | ";
                    }
                    sysSegs.add(new TextSegment("Lag: ", 0xFFFFFF));
                    sysSegs.add(new TextSegment(stutterVal, useColors ? stutterColor : 0xFFFFFF));
                    sysStr += "Lag: " + stutterVal;
                }

                if (showTps) {
                    IntegratedServer server = mc.getSingleplayerServer();
                    if (server != null) {
                        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
                        double tps = Math.min(20.0, 1000.0 / mspt);
                        int tpsColor = 0x55FF55;
                        if (tps < 19.0)
                            tpsColor = 0xFFFF55;
                        if (tps < 15.0)
                            tpsColor = 0xFF5555;

                        if (!sysSegs.isEmpty()) {
                            sysSegs.add(new TextSegment(" | ", 0xFFFFFF));
                            sysStr += " | ";
                        }
                        sysSegs.add(new TextSegment("TPS: ", 0xFFFFFF));
                        sysSegs.add(new TextSegment(String.valueOf(Math.round(tps * 10) / 10.0),
                                useColors ? tpsColor : 0xFFFFFF));
                        sysStr += "TPS: " + (Math.round(tps * 10) / 10.0);
                    }
                }

                if (!sysSegs.isEmpty()) {
                    if (useColors) {
                        HudLine line = nextLine();
                        for (TextSegment seg : sysSegs) {
                            line.addSegment(seg.text, seg.color);
                        }
                    } else {
                        nextLine().addSegment(sysStr);
                    }
                }
            }

            // Coordinates
            if (CompactF3PlusConfig.showCoords.get()) {
                nextLine().addSegment("XYZ: " + (Math.round(player.getX() * 10) / 10.0) + ", "
                        + (Math.round(player.getY() * 10) / 10.0) + ", "
                        + (Math.round(player.getZ() * 10) / 10.0));
            }

            // Subchunk / Slime
            if (CompactF3PlusConfig.showSubchunk.get()) {
                BlockPos pos = player.blockPosition();
                int cx = pos.getX() >> 4;
                int cy = pos.getY() >> 4;
                int cz = pos.getZ() >> 4;
                String subchunkLine = "Chunk: " + cx + " " + cy + " " + cz + " | Subchunk: " + (pos.getX() & 15) + " "
                        + (pos.getY() & 15) + " " + (pos.getZ() & 15);

                IntegratedServer server = mc.getSingleplayerServer();
                if (server != null) {
                    try {
                        ServerLevel overworld = server.overworld();
                        long seed = overworld != null ? overworld.getSeed() : 0L;
                        long l = seed + (long) (cx * cx * 4987142) + (long) (cx * 5947611) + (long) (cz * cz) * 4392871L
                                + (long) (cz * 389711) ^ 987234911L;
                        java.util.Random rnd = new java.util.Random(l);
                        boolean isSlime = rnd.nextInt(10) == 0;
                        subchunkLine += " | Slime Chunk: " + (isSlime ? "Yes" : "No");
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                nextLine().addSegment(subchunkLine);
            }

            // Local Difficulty — 26.1: getCurrentDifficultyAt was moved to ServerLevel
            // only. We can compute it on the integrated server (singleplayer); on
            // dedicated servers the client has no way to derive it, so we skip the line.
            if (CompactF3PlusConfig.showLocalDifficulty.get()) {
                IntegratedServer ldServer = mc.getSingleplayerServer();
                if (ldServer != null) {
                    ServerLevel sl = ldServer.getLevel(player.level().dimension());
                    if (sl != null) {
                        net.minecraft.world.DifficultyInstance diff = sl
                                .getCurrentDifficultyAt(player.blockPosition());
                        float effective = diff.getEffectiveDifficulty();
                        float special = diff.getSpecialMultiplier();
                        nextLine().addSegment("Local Diff: " + (Math.round(effective * 100) / 100.0) + " | "
                                + (Math.round(special * 100) / 100.0));
                    }
                }
            }

            // Entities
            if (CompactF3PlusConfig.showEntities.get()) {
                String debugEntities = mc.levelRenderer.getEntityStatistics();
                String eCount = debugEntities;
                int commaIdx = debugEntities.indexOf(',');
                if (commaIdx != -1) {
                    eCount = debugEntities.substring(0, commaIdx);
                }
                eCount = eCount.replace("E: ", "");
                nextLine().addSegment("Entities: " + eCount);
            }

            // Session + Ping
            boolean showSes = CompactF3PlusConfig.showSession.get();
            boolean showPing = CompactF3PlusConfig.showPing.get();
            if (showSes || showPing) {
                String sessionLine = "";
                if (showSes) {
                    long sessionMs = System.currentTimeMillis() - sessionStartTime;
                    long sessionSec = sessionMs / 1000;
                    long sH = sessionSec / 3600;
                    long sM = (sessionSec % 3600) / 60;
                    long sS = sessionSec % 60;
                    sessionLine += "Session: " + (sH < 10 ? "0" : "") + sH + ":" + (sM < 10 ? "0" : "") + sM + ":"
                            + (sS < 10 ? "0" : "") + sS;
                }

                if (showPing) {
                    PlayerInfo playerInfo = mc.getConnection() != null
                            ? mc.getConnection().getPlayerInfo(player.getUUID())
                            : null;
                    if (playerInfo != null && !mc.isLocalServer()) {
                        if (!sessionLine.isEmpty())
                            sessionLine += " | ";
                        sessionLine += "Ping: " + playerInfo.getLatency() + " ms";
                    }
                }

                if (!sessionLine.isEmpty()) {
                    nextLine().addSegment(sessionLine);
                }
            }

            // Speed
            if (CompactF3PlusConfig.showSpeed.get()) {
                Vec3 now = player.position();
                Vec3 prev = new Vec3(player.xo, player.yo, player.zo);
                double dx = now.x - prev.x;
                double dy = now.y - prev.y;
                double dz = now.z - prev.z;
                double speed = now.distanceTo(prev) * 20.0;
                double speedHorizontal = Math.sqrt(dx * dx + dz * dz) * 20.0;
                double speedVertical = dy * 20.0;

                if (CompactF3PlusConfig.detailedSpeed.get()) {
                    double speedKmh = speed * 3.6;
                    double speedKmhHorizontal = speedHorizontal * 3.6;
                    double speedKmhVertical = speedVertical * 3.6;

                    nextLine().addSegment("Speed:");
                    nextLine().addSegment(" - Horizontal: " + (Math.round(speedKmhHorizontal * 100) / 100.0) + " km/h ("
                            + (Math.round(speedHorizontal * 100) / 100.0) + " m/s)");
                    nextLine().addSegment(" - Vertical: " + (Math.round(speedKmhVertical * 100) / 100.0) + " km/h ("
                            + (Math.round(speedVertical * 100) / 100.0) + " m/s)");
                    nextLine().addSegment(" - Total Speed: " + (Math.round(speedKmh * 100) / 100.0) + " km/h ("
                            + (Math.round(speed * 100) / 100.0) + " m/s)");
                } else {
                    nextLine().addSegment("Speed: " + (Math.round(speed * 10) / 10.0) + " m/s (H: "
                            + (Math.round(speedHorizontal * 10) / 10.0) + " | V: "
                            + (Math.round(speedVertical * 10) / 10.0) + ")");
                }
            }

            // Facing
            if (CompactF3PlusConfig.showFacing.get()) {
                float yaw = player.getYRot() % 360;
                if (yaw < 0)
                    yaw += 360;
                String[] dirs = { "South", "Southwest", "West", "Northwest", "North", "Northeast", "East",
                        "Southeast" };
                String direction = dirs[Math.round(yaw / 45f) % 8];
                nextLine().addSegment("Facing: " + direction + " (" + (Math.round(yaw * 10) / 10.0) + "\u00B0)");
            }

            // Pitch
            if (CompactF3PlusConfig.showPitch.get()) {
                float pitch = player.getXRot();
                nextLine().addSegment("Pitch: " + (Math.round(pitch * 10) / 10.0) + "\u00B0");
            }

            // Time + Day
            boolean bTime = CompactF3PlusConfig.showTime.get();
            boolean bDay = CompactF3PlusConfig.showDay.get();
            if (bTime || bDay) {
                long totalTicks = player.level().getOverworldClockTime();
                String timeLine = "";
                if (bTime) {
                    long ticks = totalTicks % 24000;
                    int hour = (int) ((ticks / 1000 + 6) % 24);
                    int minute = (int) (ticks % 1000 * 60 / 1000);
                    timeLine += "Time: " + (hour < 10 ? "0" : "") + hour + ":" + (minute < 10 ? "0" : "") + minute;
                }
                if (bDay) {
                    long day = totalTicks / 24000;
                    if (!timeLine.isEmpty())
                        timeLine += " | ";
                    timeLine += "Day: " + day;
                }
                if (!timeLine.isEmpty()) {
                    nextLine().addSegment(timeLine);
                }
            }

            // Light
            if (CompactF3PlusConfig.showLight.get()) {
                BlockPos blockPos = player.blockPosition();
                int blockLight = player.level().getBrightness(LightLayer.BLOCK, blockPos);
                int skyLight = player.level().getBrightness(LightLayer.SKY, blockPos);
                nextLine().addSegment("Light: " + blockLight + " block | " + skyLight + " sky");
            }

            // Biome
            if (CompactF3PlusConfig.showBiome.get()) {
                ResourceKey<Biome> biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);
                String biome = biomeKey != null ? biomeKey.identifier().toString() : "unknown";
                nextLine().addSegment("Biome: " + biome);
            }

            if (CompactF3PlusConfig.showDimension.get()) {
                String dimension = player.level().dimension().identifier().toString();
                nextLine().addSegment("Dimension: " + dimension);
            }


            // Music Track
            if (CompactF3PlusConfig.showMusicTrack.get() && currentMusicInstance != null) {
                if (mc.getSoundManager().isActive(currentMusicInstance)) {
                    // Try to refine name from resolved sound if currently unknown or just event ID
                    try {
                        if (currentMusicInstance.getSound() != null && currentMusicInstance.getSound().getLocation() != null) {
                            String actualPath = currentMusicInstance.getSound().getLocation().getPath();
                            // If the actual path is different/more specific than what we have, update it
                            String refined = formatTrackName(actualPath);
                            if (!refined.equals("Unknown")) {
                                currentMusicTrackName = refined;
                            }
                        } else if (currentMusicTrackName.equals("Unknown")) {
                            currentMusicTrackName = formatTrackName(currentMusicInstance.getIdentifier().getPath());
                        }
                    } catch (Exception e) {}

                    nextLine().addSegment("Music: " + currentMusicTrackName);
                } else {
                    currentMusicInstance = null;
                    currentMusicTrackName = "";
                }
            }

            // Durability
            if (CompactF3PlusConfig.showDurability.get()) {
                String durLine = "";
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.isDamageableItem() && mainHand.isDamaged()) {
                    durLine += (mainHand.getMaxDamage() - mainHand.getDamageValue()) + "/" + mainHand.getMaxDamage();
                }
                ItemStack offHand = player.getOffhandItem();
                if (offHand.isDamageableItem() && offHand.isDamaged()) {
                    if (!durLine.isEmpty()) durLine += " | ";
                    durLine += (offHand.getMaxDamage() - offHand.getDamageValue()) + "/" + offHand.getMaxDamage();
                }
                boolean hasArmor = false;
                net.minecraft.world.entity.EquipmentSlot[] slots = new net.minecraft.world.entity.EquipmentSlot[]{
                    net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST,
                    net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET
                };
                for (net.minecraft.world.entity.EquipmentSlot slot : slots) {
                    ItemStack armor = player.getItemBySlot(slot);
                    if (armor != null && armor.isDamageableItem() && armor.isDamaged()) {
                        hasArmor = true;
                        break;
                    }
                }
                if (hasArmor) {
                    if (!durLine.isEmpty()) durLine += " | ";
                    durLine += "Armor Damaged";
                }
                if (!durLine.isEmpty()) {
                    nextLine().addSegment("Durability: " + durLine);
                }
            }

            // Crop Growth
            if (CompactF3PlusConfig.showCropGrowth.get() && mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos targetPos = ((BlockHitResult) mc.hitResult).getBlockPos();
                BlockState state = player.level().getBlockState(targetPos);
                Property<?> ageProp = state.getProperties().stream().filter(p -> p.getName().equals("age")).findFirst().orElse(null);
                if (ageProp instanceof IntegerProperty intProp) {
                    int age = state.getValue(intProp);
                    int maxAge = intProp.getPossibleValues().stream().max(Integer::compareTo).orElse(age);
                    int percent = (int) (((float) age / maxAge) * 100);
                    nextLine().addSegment("Crop Age: " + age + "/" + maxAge + " (" + percent + "%)");
                }
            }

            if (currentLineIndex == 0)
                return;

            // Draw
            int drawX = 10;
            int drawY = 10;
            int lineHeight = 10;

            int maxWidth = 0;
            for (int i = 0; i < currentLineIndex; i++) {
                HudLine line = lines.get(i);
                int lineWidth = 0;
                for (int j = 0; j < line.currentSegmentIndex; j++) {
                    lineWidth += font.width(line.segments.get(j).text);
                }
                maxWidth = Math.max(maxWidth, lineWidth);
            }
            int padding = 4;

            int opacitySetting = CompactF3PlusConfig.backgroundOpacity.get();
            int alphaInt = (int) ((opacitySetting / 100.0f) * 255.0f);
            int bgColor = (alphaInt << 24) | 0x000000;

            event.getGuiGraphics().fill(
                    drawX - padding,
                    drawY - padding,
                    drawX + maxWidth + padding,
                    drawY + currentLineIndex * lineHeight + padding,
                    bgColor);

            boolean drawShadow = CompactF3PlusConfig.textShadow.get();
            for (int i = 0; i < currentLineIndex; i++) {
                HudLine line = lines.get(i);
                int x = drawX;
                for (int j = 0; j < line.currentSegmentIndex; j++) {
                    TextSegment seg = line.segments.get(j);
                    // 26.1: GuiGraphicsExtractor#text silently skips alpha=0 colors,
                    // so opaque alpha must be present in the high byte.
                    event.getGuiGraphics().text(font, seg.text, x, drawY, seg.color | 0xFF000000, drawShadow);
                    x += font.width(seg.text);
                }
                drawY += lineHeight;
            }
        }
    }
}
