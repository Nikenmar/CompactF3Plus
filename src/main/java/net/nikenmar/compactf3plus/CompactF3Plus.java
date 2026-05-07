package net.nikenmar.compactf3plus;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class CompactF3Plus implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CompactF3PlusConfig.load();
        HudRenderer.initialize();
    }


    static final class HudRenderer {
        private static boolean compactHudEnabled = false;
        // True when the active HUD session was opened by pressing F3 (replaceF3 mode).
        // F8 and enabledByDefault do not set this — they open the HUD in "plain" mode
        // without the 3D gizmo even if showGizmo is on.
        private static boolean compactHudOpenedViaF3 = false;
        private static boolean wasDebugShowing = false;
        // While replaceF3 is on we OWN the THREE_DIMENSIONAL_CROSSHAIR debug entry
        // unconditionally (ALWAYS_ON when our gizmo is wanted, NEVER otherwise).
        // setStatus persists to disk, so we only call it on transitions. Initial
        // value `true` forces a setStatus(NEVER) on the first frame after world entry,
        // which clears any stale ALWAYS_ON left over from a previous session.
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
        // Fabric has no event for category registration — we use the deprecated
        // KeyMapping.Category.register(Identifier) since it's still the only way to
        // add the category to the SORT_ORDER list that vanilla iterates.
        @SuppressWarnings("deprecation")
        private static final KeyMapping.Category COMPACT_F3_CATEGORY =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("compactf3plus", "main"));
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
            }
            HudLine line = new HudLine();
            lines.add(line);
            currentLineIndex++;
            return line;
        }

        static void initialize() {
            KeyMappingHelper.registerKeyMapping(TOGGLE_HUD);

            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                compactHudEnabled = CompactF3PlusConfig.enabledByDefault;
                compactHudOpenedViaF3 = false;
                wasDebugShowing = false;
            });

            // 26.1+ replaced HudRenderCallback with HudElement / HudElementRegistry.
            // addFirst inserts BEFORE any vanilla element — ideal for our state machine
            // that needs to run before vanilla picks up the F3 toggle this frame.
            // addLast inserts AFTER everything else — where we draw our overlay.
            HudElementRegistry.addFirst(
                    Identifier.fromNamespaceAndPath("compactf3plus", "state_machine"),
                    (graphics, delta) -> onPreHudExtract());
            HudElementRegistry.addLast(
                    Identifier.fromNamespaceAndPath("compactf3plus", "overlay"),
                    HudRenderer::onRenderHud);
        }

        // State machine runs before any vanilla HUD element draws this frame.
        // Mirror of the NeoForge branch's RenderGuiEvent.Pre listener.
        static void onPreHudExtract() {
            Minecraft mc = Minecraft.getInstance();
            if (CompactF3PlusConfig.replaceF3) {
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
            if (!CompactF3PlusConfig.replaceF3)
                return;
            boolean wantGizmo = CompactF3PlusConfig.showGizmo
                    && compactHudEnabled
                    && compactHudOpenedViaF3;
            if (wantGizmo != lastGizmoApplied) {
                mc.debugEntries.setStatus(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR,
                        wantGizmo ? DebugScreenEntryStatus.ALWAYS_ON : DebugScreenEntryStatus.NEVER);
                lastGizmoApplied = wantGizmo;
            }
        }

        private static void onRenderHud(GuiGraphicsExtractor guiGraphics, DeltaTracker delta) {
            Minecraft mc = Minecraft.getInstance();
            while (TOGGLE_HUD.consumeClick()) {
                compactHudEnabled = !compactHudEnabled;
                compactHudOpenedViaF3 = false;
            }
            LocalPlayer player = mc.player;
            if (player == null || mc.options.hideGui)
                return;

            // replaceF3=on path: state machine is driven by onPreHudExtract. Just bail
            // if compact HUD isn't active for this frame.
            // replaceF3=off path: don't render our HUD while vanilla F3 is open.
            boolean debugShowing = mc.debugEntries.isOverlayVisible();
            if (CompactF3PlusConfig.replaceF3) {
                if (!compactHudEnabled)
                    return;
            } else {
                wasDebugShowing = debugShowing;
                if (!compactHudEnabled || debugShowing)
                    return;
            }

            Font font = mc.font;
            boolean useColors = CompactF3PlusConfig.colorIndicators;
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
            if (CompactF3PlusConfig.showFps) {
                float msPerFrame = 1000f / fps;

                if (useColors) {
                    int fpsColor;
                    if (fpsHistory.size() >= 10 && avgFps > 0) {
                        float ratio = (float) fps / (float) avgFps;
                        if (ratio >= 0.80f) {
                            fpsColor = 0x55FF55;
                        } else if (ratio >= 0.50f) {
                            fpsColor = 0xFFFF55;
                        } else {
                            fpsColor = 0xFF5555;
                        }
                    } else {
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
            boolean showSys = CompactF3PlusConfig.showSystem;
            boolean showLag = CompactF3PlusConfig.showLag;
            boolean showTps = CompactF3PlusConfig.showTps;

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
                        double stutterThreshold = Math.max(expectedMs * 2.0, 16.6);
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
            if (CompactF3PlusConfig.showCoords) {
                nextLine().addSegment("XYZ: " + (Math.round(player.getX() * 10) / 10.0) + ", "
                        + (Math.round(player.getY() * 10) / 10.0) + ", "
                        + (Math.round(player.getZ() * 10) / 10.0));
            }

            // Subchunk / Slime
            if (CompactF3PlusConfig.showSubchunk) {
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
                    } catch (Exception ignored) {
                    }
                }
                nextLine().addSegment(subchunkLine);
            }

            // Local Difficulty — 26.1: only on ServerLevel (singleplayer-only).
            if (CompactF3PlusConfig.showLocalDifficulty) {
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
            if (CompactF3PlusConfig.showEntities) {
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
            boolean showSes = CompactF3PlusConfig.showSession;
            boolean showPing = CompactF3PlusConfig.showPing;
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
            if (CompactF3PlusConfig.showSpeed) {
                Vec3 now = player.position();
                Vec3 prev = new Vec3(player.xo, player.yo, player.zo);
                double dx = now.x - prev.x;
                double dy = now.y - prev.y;
                double dz = now.z - prev.z;
                double speed = now.distanceTo(prev) * 20.0;
                double speedHorizontal = Math.sqrt(dx * dx + dz * dz) * 20.0;
                double speedVertical = dy * 20.0;

                if (CompactF3PlusConfig.detailedSpeed) {
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
            if (CompactF3PlusConfig.showFacing) {
                float yaw = player.getYRot() % 360;
                if (yaw < 0)
                    yaw += 360;
                String[] dirs = { "South", "Southwest", "West", "Northwest", "North", "Northeast", "East",
                        "Southeast" };
                String direction = dirs[Math.round(yaw / 45f) % 8];
                nextLine().addSegment("Facing: " + direction + " (" + (Math.round(yaw * 10) / 10.0) + "°)");
            }

            // Pitch
            if (CompactF3PlusConfig.showPitch) {
                float pitch = player.getXRot();
                nextLine().addSegment("Pitch: " + (Math.round(pitch * 10) / 10.0) + "°");
            }

            // Time + Day
            boolean bTime = CompactF3PlusConfig.showTime;
            boolean bDay = CompactF3PlusConfig.showDay;
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
            if (CompactF3PlusConfig.showLight) {
                BlockPos blockPos = player.blockPosition();
                int blockLight = player.level().getBrightness(LightLayer.BLOCK, blockPos);
                int skyLight = player.level().getBrightness(LightLayer.SKY, blockPos);
                nextLine().addSegment("Light: " + blockLight + " block | " + skyLight + " sky");
            }

            // Biome
            if (CompactF3PlusConfig.showBiome) {
                ResourceKey<Biome> biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);
                String biome = biomeKey != null ? biomeKey.identifier().toString() : "unknown";
                nextLine().addSegment("Biome: " + biome);
            }

            // Dimension
            if (CompactF3PlusConfig.showDimension) {
                String dimension = player.level().dimension().identifier().toString();
                nextLine().addSegment("Dimension: " + dimension);
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

            int opacitySetting = CompactF3PlusConfig.backgroundOpacity;
            int alphaInt = (int) ((opacitySetting / 100.0f) * 255.0f);
            int bgColor = (alphaInt << 24) | 0x000000;

            guiGraphics.fill(
                    drawX - padding,
                    drawY - padding,
                    drawX + maxWidth + padding,
                    drawY + currentLineIndex * lineHeight + padding,
                    bgColor);

            boolean drawShadow = CompactF3PlusConfig.textShadow;
            for (int i = 0; i < currentLineIndex; i++) {
                HudLine line = lines.get(i);
                int x = drawX;
                for (int j = 0; j < line.currentSegmentIndex; j++) {
                    TextSegment seg = line.segments.get(j);
                    // GuiGraphicsExtractor#text silently skips alpha=0 — force opaque.
                    guiGraphics.text(font, seg.text, x, drawY, seg.color | 0xFF000000, drawShadow);
                    x += font.width(seg.text);
                }
                drawY += lineHeight;
            }
        }
    }
}
