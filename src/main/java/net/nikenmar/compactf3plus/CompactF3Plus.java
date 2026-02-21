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

            // FPS
            if (CompactF3PlusConfig.showFps.get()) {
                int fps = mc.getFps();
                float msPerFrame = 1000f / fps;

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

                if (useColors) {
                    int fpsColor;
                    if (fps > 60)
                        fpsColor = 0x55FF55;
                    else if (fps >= 30)
                        fpsColor = 0xFFFF55;
                    else
                        fpsColor = 0xFF5555;
                    lines.add(new HudLine(List.of(
                            new TextSegment("FPS: ", 0xFFFFFF),
                            new TextSegment(String.valueOf(fps), fpsColor),
                            new TextSegment(String.format(" (%d avg) %.1f ms", avgFps, msPerFrame), 0xFFFFFF))));
                } else {
                    lines.add(new HudLine(
                            String.format("FPS: %d (%d avg) %.1f ms", fps, avgFps, msPerFrame)));
                }
            }

            // System (RAM / TPS)
            if (CompactF3PlusConfig.showSystem.get()) {
                Runtime rt = Runtime.getRuntime();
                long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
                long maxMB = rt.maxMemory() / 1024 / 1024;

                IntegratedServer server = mc.getSingleplayerServer();
                if (server != null) {
                    double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
                    double tps = Math.min(20.0, 1000.0 / mspt);
                    if (useColors) {
                        int tpsColor;
                        if (tps >= 19.0)
                            tpsColor = 0x55FF55;
                        else if (tps >= 15.0)
                            tpsColor = 0xFFFF55;
                        else
                            tpsColor = 0xFF5555;
                        lines.add(new HudLine(List.of(
                                new TextSegment(String.format("RAM: %d/%d MB | TPS: ", usedMB, maxMB), 0xFFFFFF),
                                new TextSegment(String.format("%.1f", tps), tpsColor))));
                    } else {
                        lines.add(new HudLine(
                                String.format("RAM: %d/%d MB | TPS: %.1f", usedMB, maxMB, tps)));
                    }
                } else {
                    lines.add(new HudLine(String.format("RAM: %d/%d MB", usedMB, maxMB)));
                }
            }

            // Coordinates
            if (CompactF3PlusConfig.showCoords.get()) {
                lines.add(new HudLine(
                        String.format("XYZ: %.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ())));
            }

            // Session + Ping
            if (CompactF3PlusConfig.showSession.get()) {
                long sessionMs = System.currentTimeMillis() - sessionStartTime;
                long sessionSec = sessionMs / 1000;
                long sH = sessionSec / 3600;
                long sM = (sessionSec % 3600) / 60;
                long sS = sessionSec % 60;

                PlayerInfo playerInfo = mc.getConnection() != null
                        ? mc.getConnection().getPlayerInfo(player.getUUID())
                        : null;
                String sessionLine;
                if (playerInfo != null && !mc.isLocalServer()) {
                    sessionLine = String.format("Session: %02d:%02d:%02d | Ping: %d ms", sH, sM, sS,
                            playerInfo.getLatency());
                } else {
                    sessionLine = String.format("Session: %02d:%02d:%02d", sH, sM, sS);
                }
                lines.add(new HudLine(sessionLine));
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
                double speedKmh = speed * 3.6;
                double speedKmhHorizontal = speedHorizontal * 3.6;
                double speedKmhVertical = speedVertical * 3.6;

                lines.add(new HudLine("Speed:"));
                lines.add(new HudLine(
                        String.format(" - Horizontal: %.2f km/h (%.2f m/s)", speedKmhHorizontal, speedHorizontal)));
                lines.add(new HudLine(
                        String.format(" - Vertical: %.2f km/h (%.2f m/s)", speedKmhVertical, speedVertical)));
                lines.add(new HudLine(
                        String.format(" - Total Speed: %.2f km/h (%.2f m/s)", speedKmh, speed)));
            }

            // Facing
            if (CompactF3PlusConfig.showFacing.get()) {
                float yaw = player.getYRot() % 360;
                if (yaw < 0)
                    yaw += 360;
                String[] dirs = { "South", "Southwest", "West", "Northwest", "North", "Northeast", "East",
                        "Southeast" };
                String direction = dirs[Math.round(yaw / 45f) % 8];
                lines.add(new HudLine(String.format("Facing: %s (%.1f\u00B0)", direction, yaw)));
            }

            // Time + Day
            if (CompactF3PlusConfig.showTime.get()) {
                long totalTicks = player.level().getDayTime();
                long ticks = totalTicks % 24000;
                int hour = (int) ((ticks / 1000 + 6) % 24);
                int minute = (int) (ticks % 1000 * 60 / 1000);
                long day = totalTicks / 24000;
                lines.add(new HudLine(String.format("Time: %02d:%02d | Day: %d", hour, minute, day)));
            }

            // Light
            if (CompactF3PlusConfig.showLight.get()) {
                BlockPos blockPos = player.blockPosition();
                int blockLight = player.level().getBrightness(LightLayer.BLOCK, blockPos);
                int skyLight = player.level().getBrightness(LightLayer.SKY, blockPos);
                lines.add(new HudLine(String.format("Light: %d block | %d sky", blockLight, skyLight)));
            }

            // Biome
            if (CompactF3PlusConfig.showBiome.get()) {
                ResourceKey<Biome> biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);
                String biome = biomeKey != null ? biomeKey.location().toString() : "unknown";
                lines.add(new HudLine(String.format("Biome: %s", biome)));
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

            event.getGuiGraphics().fill(
                    drawX - padding,
                    drawY - padding,
                    drawX + maxWidth + padding,
                    drawY + lines.size() * lineHeight + padding,
                    0x40000000);

            for (HudLine line : lines) {
                int x = drawX;
                for (TextSegment seg : line.segments) {
                    event.getGuiGraphics().drawString(font, seg.text, x, drawY, seg.color, false);
                    x += font.width(seg.text);
                }
                drawY += lineHeight;
            }
        }
    }
}
