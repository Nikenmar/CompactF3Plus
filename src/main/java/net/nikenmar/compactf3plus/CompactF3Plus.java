package net.nikenmar.compactf3plus;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Mod("compactf3plus")
public class CompactF3Plus {
    private static final KeyBinding TOGGLE_HUD = new KeyBinding(
            "key.compactf3plus.toggleHud",
            GLFW.GLFW_KEY_F8,
            "key.categories.compactf3plus");

    public CompactF3Plus() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CompactF3PlusConfig.SPEC);
        ModLoadingContext.get().registerExtensionPoint(
                ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, screen) -> new CompactF3PlusConfigScreen(screen));

        modBus.addListener(CompactF3Plus::onClientSetup);

        MinecraftForge.EVENT_BUS.addListener(HudRenderer::onRenderOverlayPre);
        MinecraftForge.EVENT_BUS.addListener(HudRenderer::onRenderOverlayPost);
        MinecraftForge.EVENT_BUS.addListener(HudRenderer::onPlayerLogin);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        ClientRegistry.registerKeyBinding(TOGGLE_HUD);
    }

    private static final class HudRenderer {
        private static boolean compactHudEnabled = false;
        private static boolean wasDebugShowing = false;
        private static boolean toggledForCrosshair = false;

        private static final int AVG_FPS_SECONDS = 60;
        private static final LinkedList<Integer> fpsHistory = new LinkedList<Integer>();
        private static long lastFpsSampleTime = 0;

        private static final int STUTTER_HISTORY_SIZE = 600;
        private static final double[] frameTimesBuf = new double[STUTTER_HISTORY_SIZE];
        private static int frameTimeIdx = 0;
        private static int framesCollected = 0;
        private static long lastFrameTimeNano = System.nanoTime();

        private static long sessionStartTime = System.currentTimeMillis();

        private static final List<HudLine> lines = new ArrayList<HudLine>();
        private static int currentLineIndex = 0;

        public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggedInEvent event) {
            compactHudEnabled = CompactF3PlusConfig.enabledByDefault.get();
            wasDebugShowing = false;
            sessionStartTime = System.currentTimeMillis();
        }

        public static void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            ClientPlayerEntity player = mc.player;
            if (player == null || mc.gameSettings.hideGUI) {
                return;
            }

            if (!CompactF3PlusConfig.replaceF3.get()) {
                return;
            }

            if (event.getType() == RenderGameOverlayEvent.ElementType.DEBUG) {
                event.setCanceled(true);
                return;
            }

            if (!CompactF3PlusConfig.showGizmo.get()
                    && event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS
                    && mc.gameSettings.showDebugInfo) {
                mc.gameSettings.showDebugInfo = false;
                toggledForCrosshair = true;
            }
        }

        public static void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();

            if (toggledForCrosshair && event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
                mc.gameSettings.showDebugInfo = true;
                toggledForCrosshair = false;
            }

            if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
                return;
            }

            if (TOGGLE_HUD.isPressed()) {
                compactHudEnabled = !compactHudEnabled;
            }

            ClientPlayerEntity player = mc.player;
            if (player == null || mc.gameSettings.hideGUI) {
                return;
            }

            boolean debugShowing = mc.gameSettings.showDebugInfo;
            if (CompactF3PlusConfig.replaceF3.get()) {
                if (debugShowing != wasDebugShowing) {
                    compactHudEnabled = !compactHudEnabled;
                    wasDebugShowing = debugShowing;
                }

                if (!compactHudEnabled && debugShowing) {
                    mc.gameSettings.showDebugInfo = false;
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

            FontRenderer font = mc.fontRenderer;
            boolean useColors = CompactF3PlusConfig.colorIndicators.get();
            currentLineIndex = 0;

            int fps = getCurrentFps(mc);
            long nowMillis = System.currentTimeMillis();
            if (nowMillis - lastFpsSampleTime >= 1000) {
                fpsHistory.add(Integer.valueOf(fps));
                if (fpsHistory.size() > AVG_FPS_SECONDS) {
                    fpsHistory.removeFirst();
                }
                lastFpsSampleTime = nowMillis;
            }

            int avgFps = 0;
            for (int i = 0; i < fpsHistory.size(); i++) {
                avgFps += fpsHistory.get(i).intValue();
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

            if (CompactF3PlusConfig.showFps.get()) {
                float msPerFrame = fps > 0 ? (1000f / fps) : 0f;

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
                        if (fps > 60) {
                            fpsColor = 0x55FF55;
                        } else if (fps >= 30) {
                            fpsColor = 0xFFFF55;
                        } else {
                            fpsColor = 0xFF5555;
                        }
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

            boolean showSys = CompactF3PlusConfig.showSystem.get();
            boolean showLag = CompactF3PlusConfig.showLag.get();
            boolean showTps = CompactF3PlusConfig.showTps.get();

            if (showSys || showLag || showTps) {
                List<TextSegment> sysSegs = new ArrayList<TextSegment>();
                StringBuilder sysStr = new StringBuilder();

                if (showSys) {
                    Runtime rt = Runtime.getRuntime();
                    long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                    long maxMB = rt.maxMemory() / 1024 / 1024;
                    sysSegs.add(new TextSegment("RAM: " + usedMB + "/" + maxMB + " MB", 0xFFFFFF));
                    sysStr.append("RAM: ").append(usedMB).append("/").append(maxMB).append(" MB");
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
                        sysStr.append(" | ");
                    }
                    sysSegs.add(new TextSegment("Lag: ", 0xFFFFFF));
                    sysSegs.add(new TextSegment(stutterVal, useColors ? stutterColor : 0xFFFFFF));
                    sysStr.append("Lag: ").append(stutterVal);
                }

                if (showTps && mc.isIntegratedServerRunning()) {
                    float mspt = mc.getIntegratedServer().getTickTime();
                    double tps = mspt > 0.0 ? Math.min(20.0, 1000.0 / mspt) : 20.0;
                    int tpsColor = 0x55FF55;
                    if (tps < 19.0) {
                        tpsColor = 0xFFFF55;
                    }
                    if (tps < 15.0) {
                        tpsColor = 0xFF5555;
                    }

                    if (!sysSegs.isEmpty()) {
                        sysSegs.add(new TextSegment(" | ", 0xFFFFFF));
                        sysStr.append(" | ");
                    }
                    sysSegs.add(new TextSegment("TPS: ", 0xFFFFFF));
                    sysSegs.add(new TextSegment(String.valueOf(Math.round(tps * 10) / 10.0), useColors ? tpsColor : 0xFFFFFF));
                    sysStr.append("TPS: ").append(Math.round(tps * 10) / 10.0);
                }

                if (!sysSegs.isEmpty()) {
                    if (useColors) {
                        HudLine line = nextLine();
                        for (int i = 0; i < sysSegs.size(); i++) {
                            TextSegment seg = sysSegs.get(i);
                            line.addSegment(seg.text, seg.color);
                        }
                    } else {
                        nextLine().addSegment(sysStr.toString());
                    }
                }
            }

            if (CompactF3PlusConfig.showCoords.get()) {
                nextLine().addSegment("XYZ: " + (Math.round(player.getPosX() * 10) / 10.0) + ", "
                        + (Math.round(player.getPosY() * 10) / 10.0) + ", "
                        + (Math.round(player.getPosZ() * 10) / 10.0));
            }

            if (CompactF3PlusConfig.showSubchunk.get()) {
                BlockPos pos = player.getPosition();
                int cx = pos.getX() >> 4;
                int cy = pos.getY() >> 4;
                int cz = pos.getZ() >> 4;
                String subchunkLine = "Chunk: " + cx + " " + cy + " " + cz + " | Subchunk: " + (pos.getX() & 15) + " "
                        + (pos.getY() & 15) + " " + (pos.getZ() & 15);
                nextLine().addSegment(subchunkLine);
            }

            if (CompactF3PlusConfig.showLocalDifficulty.get()) {
                net.minecraft.world.DifficultyInstance diff = player.world.getDifficultyForLocation(player.getPosition());
                float effective = diff.getAdditionalDifficulty();
                float clamped = diff.getClampedAdditionalDifficulty();
                nextLine().addSegment("Local Diff: " + (Math.round(effective * 100) / 100.0) + " | "
                        + (Math.round(clamped * 100) / 100.0));
            }

            if (CompactF3PlusConfig.showEntities.get()) {
                String debugEntities = mc.worldRenderer.getDebugInfoEntities();
                String eCount = debugEntities;
                int commaIdx = debugEntities.indexOf(',');
                if (commaIdx != -1) {
                    eCount = debugEntities.substring(0, commaIdx);
                }
                eCount = eCount.replace("E: ", "");
                nextLine().addSegment("Entities: " + eCount);
            }

            boolean showSes = CompactF3PlusConfig.showSession.get();
            boolean showPing = CompactF3PlusConfig.showPing.get();
            if (showSes || showPing) {
                StringBuilder sessionLine = new StringBuilder();
                if (showSes) {
                    long sessionMs = System.currentTimeMillis() - sessionStartTime;
                    long sessionSec = sessionMs / 1000;
                    long sH = sessionSec / 3600;
                    long sM = (sessionSec % 3600) / 60;
                    long sS = sessionSec % 60;
                    sessionLine.append("Session: ")
                            .append(sH < 10 ? "0" : "").append(sH).append(":")
                            .append(sM < 10 ? "0" : "").append(sM).append(":")
                            .append(sS < 10 ? "0" : "").append(sS);
                }

                if (showPing) {
                    NetworkPlayerInfo playerInfo = mc.getConnection() != null
                            ? mc.getConnection().getPlayerInfo(player.getUniqueID())
                            : null;
                    if (playerInfo != null && !mc.isSingleplayer()) {
                        if (sessionLine.length() > 0) {
                            sessionLine.append(" | ");
                        }
                        sessionLine.append("Ping: ").append(playerInfo.getResponseTime()).append(" ms");
                    }
                }

                if (sessionLine.length() > 0) {
                    nextLine().addSegment(sessionLine.toString());
                }
            }

            if (CompactF3PlusConfig.showSpeed.get()) {
                Vector3d now = player.getPositionVec();
                Vector3d prev = new Vector3d(player.prevPosX, player.prevPosY, player.prevPosZ);
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

            if (CompactF3PlusConfig.showFacing.get()) {
                float yaw = player.rotationYaw % 360;
                if (yaw < 0) {
                    yaw += 360;
                }
                String[] dirs = {"South", "Southwest", "West", "Northwest", "North", "Northeast", "East", "Southeast"};
                String direction = dirs[Math.round(yaw / 45f) % 8];
                nextLine().addSegment("Facing: " + direction + " (" + (Math.round(yaw * 10) / 10.0) + "\u00B0)");
            }

            if (CompactF3PlusConfig.showPitch.get()) {
                float pitch = player.rotationPitch;
                nextLine().addSegment("Pitch: " + (Math.round(pitch * 10) / 10.0) + "\u00B0");
            }

            boolean bTime = CompactF3PlusConfig.showTime.get();
            boolean bDay = CompactF3PlusConfig.showDay.get();
            if (bTime || bDay) {
                long totalTicks = player.world.getDayTime();
                StringBuilder timeLine = new StringBuilder();
                if (bTime) {
                    long ticks = totalTicks % 24000;
                    int hour = (int) ((ticks / 1000 + 6) % 24);
                    int minute = (int) (ticks % 1000 * 60 / 1000);
                    timeLine.append("Time: ")
                            .append(hour < 10 ? "0" : "").append(hour).append(":")
                            .append(minute < 10 ? "0" : "").append(minute);
                }
                if (bDay) {
                    long day = totalTicks / 24000;
                    if (timeLine.length() > 0) {
                        timeLine.append(" | ");
                    }
                    timeLine.append("Day: ").append(day);
                }
                if (timeLine.length() > 0) {
                    nextLine().addSegment(timeLine.toString());
                }
            }

            if (CompactF3PlusConfig.showLight.get()) {
                BlockPos blockPos = player.getPosition();
                int blockLight = player.world.getLightFor(LightType.BLOCK, blockPos);
                int skyLight = player.world.getLightFor(LightType.SKY, blockPos);
                nextLine().addSegment("Light: " + blockLight + " block | " + skyLight + " sky");
            }

            if (CompactF3PlusConfig.showBiome.get()) {
                Biome biomeObj = player.world.getBiome(player.getPosition());
                ResourceLocation biomeId = biomeObj.getRegistryName();
                String biome = biomeId != null ? biomeId.toString() : "unknown";
                nextLine().addSegment("Biome: " + biome);
            }

            if (CompactF3PlusConfig.showDimension.get()) {
                RegistryKey<World> dimensionKey = player.world.getDimensionKey();
                String dimension = dimensionKey != null ? dimensionKey.getLocation().toString() : "unknown";
                nextLine().addSegment("Dimension: " + dimension);
            }

            if (currentLineIndex == 0) {
                return;
            }

            MatrixStack matrix = event.getMatrixStack();
            int drawX = 10;
            int drawY = 10;
            int lineHeight = 10;
            int maxWidth = 0;

            for (int i = 0; i < currentLineIndex; i++) {
                HudLine line = lines.get(i);
                int lineWidth = 0;
                for (int j = 0; j < line.currentSegmentIndex; j++) {
                    lineWidth += font.getStringWidth(line.segments.get(j).text);
                }
                maxWidth = Math.max(maxWidth, lineWidth);
            }

            int padding = 4;
            int opacitySetting = CompactF3PlusConfig.backgroundOpacity.get();
            int alphaInt = (int) ((opacitySetting / 100.0f) * 255.0f);
            int bgColor = (alphaInt << 24) | 0x000000;

            AbstractGui.fill(
                    matrix,
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
                    if (drawShadow) {
                        font.drawStringWithShadow(matrix, seg.text, x, drawY, seg.color);
                    } else {
                        font.drawString(matrix, seg.text, x, drawY, seg.color);
                    }
                    x += font.getStringWidth(seg.text);
                }
                drawY += lineHeight;
            }
        }

        private static int getCurrentFps(Minecraft mc) {
            String fpsString = mc.debug;
            if (fpsString == null || fpsString.isEmpty()) {
                return 0;
            }

            int separator = fpsString.indexOf(' ');
            String fpsPart = separator >= 0 ? fpsString.substring(0, separator) : fpsString;
            try {
                return Integer.parseInt(fpsPart);
            } catch (NumberFormatException ignored) {
                return 0;
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
            final List<TextSegment> segments = new ArrayList<TextSegment>();
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
    }
}
