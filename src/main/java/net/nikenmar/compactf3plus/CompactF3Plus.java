package net.nikenmar.compactf3plus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class CompactF3Plus implements ClientModInitializer {
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
        private static final KeyBinding TOGGLE_HUD = new KeyBinding(
                "key.compactf3plus.toggleHud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "key.categories.compactf3plus");

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

        private static void initialize() {
            KeyBindingHelper.registerKeyBinding(TOGGLE_HUD);

            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                compactHudEnabled = CompactF3PlusConfig.enabledByDefault;
                wasDebugShowing = false;
            });

            HudRenderCallback.EVENT.register(HudRenderer::onRenderHud);
        }

        private static boolean shouldCancelVanillaDebugOverlay() {
            if (!CompactF3PlusConfig.replaceF3) {
                return false;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) {
                return false;
            }

            // Cancel vanilla debug immediately when F3 state is on.
            // This avoids one-frame flashes before our own state sync runs in HudRenderCallback.
            return mc.getDebugHud().shouldShowDebugHud();
        }

        private static boolean shouldHideDebugCrosshair() {
            return shouldCancelVanillaDebugOverlay() && !CompactF3PlusConfig.showGizmo;
        }

        private static void onRenderHud(DrawContext guiGraphics, RenderTickCounter ignored) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (TOGGLE_HUD.wasPressed()) {
                compactHudEnabled = !compactHudEnabled;
            }

            ClientPlayerEntity player = mc.player;
            if (player == null || mc.options.hudHidden) {
                return;
            }

            boolean debugShowing = mc.getDebugHud().shouldShowDebugHud();
            if (CompactF3PlusConfig.replaceF3) {
                if (debugShowing != wasDebugShowing) {
                    compactHudEnabled = !compactHudEnabled;
                    wasDebugShowing = debugShowing;
                }

                if (!compactHudEnabled && debugShowing) {
                    mc.getDebugHud().toggleDebugHud();
                    wasDebugShowing = false;
                }

                if (!compactHudEnabled) {
                    return;
                }
            } else {
                wasDebugShowing = debugShowing;
                if (!compactHudEnabled || debugShowing) {
                    return;
                }
            }

            TextRenderer font = mc.textRenderer;
            boolean useColors = CompactF3PlusConfig.colorIndicators;
            currentLineIndex = 0;

            int fps = mc.getCurrentFps();
            long now2 = System.currentTimeMillis();
            if (now2 - lastFpsSampleTime >= 1000) {
                fpsHistory.add(fps);
                if (fpsHistory.size() > AVG_FPS_SECONDS) {
                    fpsHistory.removeFirst();
                }
                lastFpsSampleTime = now2;
            }
            int avgFps = 0;
            for (int f : fpsHistory) {
                avgFps += f;
            }
            avgFps = fpsHistory.isEmpty() ? fps : avgFps / fpsHistory.size();

            long nowNano = System.nanoTime();
            double frameDeltaMs = (nowNano - lastFrameTimeNano) / 1_000_000.0;
            lastFrameTimeNano = nowNano;
            if (frameDeltaMs > 0 && frameDeltaMs < 1000) {
                frameTimesBuf[frameTimeIdx] = frameDeltaMs;
                frameTimeIdx = (frameTimeIdx + 1) % STUTTER_HISTORY_SIZE;
                if (framesCollected < STUTTER_HISTORY_SIZE) {
                    framesCollected++;
                }
            }

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
                    } else if (fps > 60) {
                        fpsColor = 0x55FF55;
                    } else if (fps >= 30) {
                        fpsColor = 0xFFFF55;
                    } else {
                        fpsColor = 0xFF5555;
                    }

                    HudLine line = nextLine();
                    line.addSegment("FPS: ", 0xFFFFFF);
                    line.addSegment(String.valueOf(fps), fpsColor);
                    line.addSegment(" (" + avgFps + " avg) " + (Math.round(msPerFrame * 10) / 10.0) + " ms", 0xFFFFFF);
                } else {
                    nextLine().addSegment("FPS: " + fps + " (" + avgFps + " avg) " + (Math.round(msPerFrame * 10) / 10.0) + " ms");
                }
            }

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
                        if (startIdx < 0) {
                            startIdx += STUTTER_HISTORY_SIZE;
                        }
                        for (int i = 0; i < framesToCheck; i++) {
                            int idx = (startIdx + i) % STUTTER_HISTORY_SIZE;
                            if (frameTimesBuf[idx] > stutterThreshold) {
                                stutters++;
                            }
                        }
                    }
                    double stutterRate = framesToCheck > 0 ? (stutters / (double) framesToCheck) * 100.0 : 0;
                    String stutterVal = (Math.round(stutterRate * 10) / 10.0) + "%";
                    int stutterColor = 0x55FF55;
                    if (stutterRate >= 5.0) {
                        stutterColor = 0xFFFF55;
                    }
                    if (stutterRate >= 10.0) {
                        stutterColor = 0xFF5555;
                    }

                    if (!sysSegs.isEmpty()) {
                        sysSegs.add(new TextSegment(" | ", 0xFFFFFF));
                        sysStr += " | ";
                    }
                    sysSegs.add(new TextSegment("Lag: ", 0xFFFFFF));
                    sysSegs.add(new TextSegment(stutterVal, useColors ? stutterColor : 0xFFFFFF));
                    sysStr += "Lag: " + stutterVal;
                }

                if (showTps) {
                    IntegratedServer server = mc.getServer();
                    if (server != null) {
                        double mspt = server.getAverageNanosPerTick() / 1_000_000.0;
                        double tps = Math.min(20.0, 1000.0 / mspt);
                        int tpsColor = 0x55FF55;
                        if (tps < 19.0) {
                            tpsColor = 0xFFFF55;
                        }
                        if (tps < 15.0) {
                            tpsColor = 0xFF5555;
                        }

                        if (!sysSegs.isEmpty()) {
                            sysSegs.add(new TextSegment(" | ", 0xFFFFFF));
                            sysStr += " | ";
                        }
                        sysSegs.add(new TextSegment("TPS: ", 0xFFFFFF));
                        sysSegs.add(new TextSegment(String.valueOf(Math.round(tps * 10) / 10.0), useColors ? tpsColor : 0xFFFFFF));
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

            if (CompactF3PlusConfig.showCoords) {
                nextLine().addSegment("XYZ: " + (Math.round(player.getX() * 10) / 10.0) + ", "
                        + (Math.round(player.getY() * 10) / 10.0) + ", "
                        + (Math.round(player.getZ() * 10) / 10.0));
            }

            if (CompactF3PlusConfig.showSubchunk) {
                BlockPos pos = player.getBlockPos();
                int cx = pos.getX() >> 4;
                int cy = pos.getY() >> 4;
                int cz = pos.getZ() >> 4;
                String subchunkLine = "Chunk: " + cx + " " + cy + " " + cz + " | Subchunk: " + (pos.getX() & 15) + " "
                        + (pos.getY() & 15) + " " + (pos.getZ() & 15);

                IntegratedServer server = mc.getServer();
                if (server != null) {
                    try {
                        long seed = server.getSaveProperties().getGeneratorOptions().getSeed();
                        long l = seed + (long) (cx * cx * 4987142) + (long) (cx * 5947611) + (long) (cz * cz) * 4392871L
                                + (long) (cz * 389711) ^ 987234911L;
                        java.util.Random rnd = new java.util.Random(l);
                        boolean isSlime = rnd.nextInt(10) == 0;
                        subchunkLine += " | Slime Chunk: " + (isSlime ? "Yes" : "No");
                    } catch (Exception ignored2) {
                    }
                }
                nextLine().addSegment(subchunkLine);
            }

            if (CompactF3PlusConfig.showLocalDifficulty) {
                net.minecraft.world.LocalDifficulty diff = player.getWorld().getLocalDifficulty(player.getBlockPos());
                float effective = diff.getLocalDifficulty();
                float special = diff.getClampedLocalDifficulty();
                nextLine().addSegment("Local Diff: " + (Math.round(effective * 100) / 100.0) + " | "
                        + (Math.round(special * 100) / 100.0));
            }

            if (CompactF3PlusConfig.showEntities) {
                String debugEntities = mc.worldRenderer.getEntitiesDebugString();
                String eCount = debugEntities;
                int commaIdx = debugEntities.indexOf(',');
                if (commaIdx != -1) {
                    eCount = debugEntities.substring(0, commaIdx);
                }
                eCount = eCount.replace("E: ", "");
                nextLine().addSegment("Entities: " + eCount);
            }

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
                    PlayerListEntry playerInfo = mc.getNetworkHandler() != null
                            ? mc.getNetworkHandler().getPlayerListEntry(player.getUuid())
                            : null;
                    if (playerInfo != null && !mc.isInSingleplayer()) {
                        if (!sessionLine.isEmpty()) {
                            sessionLine += " | ";
                        }
                        sessionLine += "Ping: " + playerInfo.getLatency() + " ms";
                    }
                }

                if (!sessionLine.isEmpty()) {
                    nextLine().addSegment(sessionLine);
                }
            }

            if (CompactF3PlusConfig.showSpeed) {
                Vec3d now = player.getPos();
                Vec3d prev = new Vec3d(player.prevX, player.prevY, player.prevZ);
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

            if (CompactF3PlusConfig.showFacing) {
                float yaw = player.getYaw() % 360;
                if (yaw < 0) {
                    yaw += 360;
                }
                String[] dirs = { "South", "Southwest", "West", "Northwest", "North", "Northeast", "East", "Southeast" };
                String direction = dirs[Math.round(yaw / 45f) % 8];
                nextLine().addSegment("Facing: " + direction + " (" + (Math.round(yaw * 10) / 10.0) + "\u00B0)");
            }

            if (CompactF3PlusConfig.showPitch) {
                float pitch = player.getPitch();
                nextLine().addSegment("Pitch: " + (Math.round(pitch * 10) / 10.0) + "\u00B0");
            }

            boolean bTime = CompactF3PlusConfig.showTime;
            boolean bDay = CompactF3PlusConfig.showDay;
            if (bTime || bDay) {
                long totalTicks = player.getWorld().getTimeOfDay();
                String timeLine = "";
                if (bTime) {
                    long ticks = totalTicks % 24000;
                    int hour = (int) ((ticks / 1000 + 6) % 24);
                    int minute = (int) (ticks % 1000 * 60 / 1000);
                    timeLine += "Time: " + (hour < 10 ? "0" : "") + hour + ":" + (minute < 10 ? "0" : "") + minute;
                }
                if (bDay) {
                    long day = totalTicks / 24000;
                    if (!timeLine.isEmpty()) {
                        timeLine += " | ";
                    }
                    timeLine += "Day: " + day;
                }
                if (!timeLine.isEmpty()) {
                    nextLine().addSegment(timeLine);
                }
            }

            if (CompactF3PlusConfig.showLight) {
                BlockPos blockPos = player.getBlockPos();
                int blockLight = player.getWorld().getLightLevel(LightType.BLOCK, blockPos);
                int skyLight = player.getWorld().getLightLevel(LightType.SKY, blockPos);
                nextLine().addSegment("Light: " + blockLight + " block | " + skyLight + " sky");
            }

            if (CompactF3PlusConfig.showBiome) {
                RegistryKey<Biome> biomeKey = player.getWorld().getBiome(player.getBlockPos()).getKey().orElse(null);
                String biome = biomeKey != null ? biomeKey.getValue().toString() : "unknown";
                nextLine().addSegment("Biome: " + biome);
            }

            if (CompactF3PlusConfig.showDimension) {
                String dimension = player.getWorld().getRegistryKey().getValue().toString();
                nextLine().addSegment("Dimension: " + dimension);
            }

            if (currentLineIndex == 0) {
                return;
            }

            int drawX = 10;
            int drawY = 10;
            int lineHeight = 10;

            int maxWidth = 0;
            for (int i = 0; i < currentLineIndex; i++) {
                HudLine line = lines.get(i);
                int lineWidth = 0;
                for (int j = 0; j < line.currentSegmentIndex; j++) {
                    lineWidth += font.getWidth(line.segments.get(j).text);
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
                    guiGraphics.drawText(font, seg.text, x, drawY, seg.color, drawShadow);
                    x += font.getWidth(seg.text);
                }
                drawY += lineHeight;
            }
        }
    }

    @Override
    public void onInitializeClient() {
        CompactF3PlusConfig.load();
        HudRenderer.initialize();
    }

    public static boolean shouldCancelVanillaDebugOverlay() {
        return HudRenderer.shouldCancelVanillaDebugOverlay();
    }

    public static boolean shouldHideDebugCrosshair() {
        return HudRenderer.shouldHideDebugCrosshair();
    }
}
