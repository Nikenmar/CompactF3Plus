package net.nikenmar.compactf3plus;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
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
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

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
        NeoForge.EVENT_BUS.addListener(HudRenderer::onRenderGui);
        NeoForge.EVENT_BUS.addListener(HudRenderer::onRenderGuiLayerPre);
        NeoForge.EVENT_BUS.addListener(HudRenderer::onRenderGuiLayerPost);
    }

    private static final class HudRenderer {
        private static boolean compactHudEnabled = false;
        private static boolean wasDebugShowing = false;
        private static final int AVG_FPS_SECONDS = 60;
        private static final LinkedList<Integer> fpsHistory = new LinkedList<>();
        private static long lastFpsSampleTime = 0;

        private static final int STUTTER_HISTORY_SIZE = 600;
        private static final double[] frameTimesBuf = new double[STUTTER_HISTORY_SIZE];
        private static int frameTimeIdx = 0;
        private static int framesCollected = 0;
        private static long lastFrameTimeNano = System.nanoTime();

        private static final long sessionStartTime = System.currentTimeMillis();
        private static final KeyMapping TOGGLE_HUD = new KeyMapping(
                "key.compactf3plus.toggleHud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "key.categories.compactf3plus");

        private record TextSegment(String text, int color) {
        }

        private record HudLine(List<TextSegment> segments) {
            HudLine(String text) {
                this(List.of(new TextSegment(text, 0xFFFFFF)));
            }
        }

        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_HUD);
        }

        private static boolean toggledForCrosshair = false;

        public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
            if (!CompactF3PlusConfig.replaceF3.get())
                return;

            if (event.getName().equals(VanillaGuiLayers.DEBUG_OVERLAY)) {
                event.setCanceled(true);
            }

            if (!CompactF3PlusConfig.showGizmo.get()
                    && event.getName().equals(VanillaGuiLayers.CROSSHAIR)
                    && Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {

                // Temporarily disable the debug overlay state before the crosshair layer
                // renders
                Minecraft.getInstance().getDebugOverlay().toggleOverlay();
                toggledForCrosshair = true;
            }
        }

        public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
            if (toggledForCrosshair && event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
                // Restore the debug overlay state after the crosshair layer finishes rendering
                Minecraft.getInstance().getDebugOverlay().toggleOverlay();
                toggledForCrosshair = false;
            }
        }

        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (TOGGLE_HUD.consumeClick())
                compactHudEnabled = !compactHudEnabled;
            LocalPlayer player = mc.player;
            if (player == null || mc.options.hideGui)
                return;

            boolean debugShowing = mc.getDebugOverlay().showDebugScreen();
            if (CompactF3PlusConfig.replaceF3.get()) {
                if (debugShowing != wasDebugShowing) {
                    compactHudEnabled = !compactHudEnabled;
                    wasDebugShowing = debugShowing;
                }

                if (!compactHudEnabled && debugShowing) {
                    mc.getDebugOverlay().toggleOverlay();
                    wasDebugShowing = false;
                }

                if (!compactHudEnabled)
                    return;
            } else {
                wasDebugShowing = debugShowing;
                if (!compactHudEnabled || debugShowing)
                    return;
            }

            Font font = mc.font;
            boolean useColors = CompactF3PlusConfig.colorIndicators.get();
            List<HudLine> lines = new ArrayList<>();

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
                    lines.add(new HudLine(List.of(
                            new TextSegment("FPS: ", 0xFFFFFF),
                            new TextSegment(String.valueOf(fps), fpsColor),
                            new TextSegment(" (" + avgFps + " avg) " + (Math.round(msPerFrame * 10) / 10.0) + " ms",
                                    0xFFFFFF))));
                } else {
                    lines.add(new HudLine(
                            "FPS: " + fps + " (" + avgFps + " avg) " + (Math.round(msPerFrame * 10) / 10.0) + " ms"));
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
                        lines.add(new HudLine(sysSegs));
                    } else {
                        lines.add(new HudLine(sysStr));
                    }
                }
            }

            // Coordinates
            if (CompactF3PlusConfig.showCoords.get()) {
                lines.add(new HudLine("XYZ: " + (Math.round(player.getX() * 10) / 10.0) + ", "
                        + (Math.round(player.getY() * 10) / 10.0) + ", "
                        + (Math.round(player.getZ() * 10) / 10.0)));
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
                        long seed = server.getWorldData().worldGenOptions().seed();
                        long l = seed + (long) (cx * cx * 4987142) + (long) (cx * 5947611) + (long) (cz * cz) * 4392871L
                                + (long) (cz * 389711) ^ 987234911L;
                        java.util.Random rnd = new java.util.Random(l);
                        boolean isSlime = rnd.nextInt(10) == 0;
                        subchunkLine += " | Slime Chunk: " + (isSlime ? "Yes" : "No");
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                lines.add(new HudLine(subchunkLine));
            }

            // Local Difficulty
            if (CompactF3PlusConfig.showLocalDifficulty.get()) {
                net.minecraft.world.DifficultyInstance diff = player.level()
                        .getCurrentDifficultyAt(player.blockPosition());
                float effective = diff.getEffectiveDifficulty();
                float special = diff.getSpecialMultiplier();
                lines.add(new HudLine("Local Diff: " + (Math.round(effective * 100) / 100.0) + " | "
                        + (Math.round(special * 100) / 100.0)));
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
                lines.add(new HudLine("Entities: " + eCount));
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
                    lines.add(new HudLine(sessionLine));
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

                    lines.add(new HudLine("Speed:"));
                    lines.add(new HudLine(" - Horizontal: " + (Math.round(speedKmhHorizontal * 100) / 100.0) + " km/h ("
                            + (Math.round(speedHorizontal * 100) / 100.0) + " m/s)"));
                    lines.add(new HudLine(" - Vertical: " + (Math.round(speedKmhVertical * 100) / 100.0) + " km/h ("
                            + (Math.round(speedVertical * 100) / 100.0) + " m/s)"));
                    lines.add(new HudLine(" - Total Speed: " + (Math.round(speedKmh * 100) / 100.0) + " km/h ("
                            + (Math.round(speed * 100) / 100.0) + " m/s)"));
                } else {
                    lines.add(new HudLine("Speed: " + (Math.round(speed * 10) / 10.0) + " m/s (H: "
                            + (Math.round(speedHorizontal * 10) / 10.0) + " | V: "
                            + (Math.round(speedVertical * 10) / 10.0) + ")"));
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
                lines.add(new HudLine("Facing: " + direction + " (" + (Math.round(yaw * 10) / 10.0) + "\u00B0)"));
            }

            // Pitch
            if (CompactF3PlusConfig.showPitch.get()) {
                float pitch = player.getXRot();
                lines.add(new HudLine("Pitch: " + (Math.round(pitch * 10) / 10.0) + "\u00B0"));
            }

            // Time + Day
            boolean bTime = CompactF3PlusConfig.showTime.get();
            boolean bDay = CompactF3PlusConfig.showDay.get();
            if (bTime || bDay) {
                long totalTicks = player.level().getDayTime();
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
                    lines.add(new HudLine(timeLine));
                }
            }

            // Light
            if (CompactF3PlusConfig.showLight.get()) {
                BlockPos blockPos = player.blockPosition();
                int blockLight = player.level().getBrightness(LightLayer.BLOCK, blockPos);
                int skyLight = player.level().getBrightness(LightLayer.SKY, blockPos);
                lines.add(new HudLine("Light: " + blockLight + " block | " + skyLight + " sky"));
            }

            // Biome
            if (CompactF3PlusConfig.showBiome.get()) {
                ResourceKey<Biome> biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);
                String biome = biomeKey != null ? biomeKey.location().toString() : "unknown";
                lines.add(new HudLine("Biome: " + biome));
            }

            // Dimension
            if (CompactF3PlusConfig.showDimension.get()) {
                String dimension = player.level().dimension().location().toString();
                lines.add(new HudLine("Dimension: " + dimension));
            }

            if (lines.isEmpty())
                return;

            // Draw
            int drawX = 10;
            int drawY = 10;
            int lineHeight = 10;

            int maxWidth = 0;
            for (HudLine line : lines) {
                int lineWidth = 0;
                for (TextSegment seg : line.segments) {
                    lineWidth += font.width(seg.text);
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
                    drawY + lines.size() * lineHeight + padding,
                    bgColor);

            boolean drawShadow = CompactF3PlusConfig.textShadow.get();
            for (HudLine line : lines) {
                int x = drawX;
                for (TextSegment seg : line.segments) {
                    event.getGuiGraphics().drawString(font, seg.text, x, drawY, seg.color, drawShadow);
                    x += font.width(seg.text);
                }
                drawY += lineHeight;
            }
        }
    }
}
