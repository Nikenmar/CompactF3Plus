package net.username65735.compactf3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;

@Mod("compactf3")
public class CompactF3 {
    public CompactF3(IEventBus modBus) {
        modBus.addListener(HudRenderer::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(HudRenderer::onRenderGui);
    }

    private static final class HudRenderer {
        private static boolean compactHudEnabled = true;
        private static final KeyMapping TOGGLE_HUD = new KeyMapping(
                "key.compactf3.toggleHud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "key.categories.compactf3"
        );

        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_HUD);
        }

        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (TOGGLE_HUD.consumeClick()) compactHudEnabled = !compactHudEnabled;
            LocalPlayer player = mc.player;
            if (player == null || !compactHudEnabled || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;

            PoseStack poseStack = event.getGuiGraphics().pose();
            Font font = mc.font;

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

            int fps = mc.getFps();
            float msPerFrame = 1000f / fps;

            float yaw = player.getYRot() % 360;
            if (yaw < 0) yaw += 360;
            String[] dirs = {"South","Southwest","West","Northwest","North","Northeast","East","Southeast"};
            String direction = dirs[Math.round(yaw / 45f) % 8];

            long ticks = player.level().getDayTime() % 24000;
            int hour = (int) ((ticks / 1000 + 6) % 24);
            int minute = (int) (ticks % 1000 * 60 / 1000);

            ResourceKey<Biome> biomeKey = player.level().getBiome(player.blockPosition()).unwrapKey().orElse(null);
            String biome = biomeKey != null ? biomeKey.location().toString() : "unknown";

            String[] lines = new String[]{
                    String.format("FPS: %d (%.1f ms)", fps, msPerFrame),
                    String.format("XYZ: %.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ()),
                    "Speed:",
                    String.format(" - Horizontal: %.2f km/h (%.2f m/s)", speedKmhHorizontal, speedHorizontal),
                    String.format(" - Vertical: %.2f km/h (%.2f m/s)", speedKmhVertical, speedVertical),
                    String.format(" - Total Speed: %.2f km/h (%.2f m/s)", speedKmh, speed),
                    String.format("Facing: %s (%.1f°)", direction, yaw),
                    String.format("Time: %02d:%02d", hour, minute),
                    String.format("Biome: %s", biome)
            };

            int drawX = 10;
            int drawY = 10;
            int lineHeight = 10;

            int maxWidth = 0;
            for (String line : lines) {
                maxWidth = Math.max(maxWidth, font.width(line));
            }
            int padding = 4;

            event.getGuiGraphics().fill(
                    drawX - padding,
                    drawY - padding,
                    drawX + maxWidth + padding,
                    drawY + lines.length * lineHeight + padding,
                    0x40000000
            );

            for (String line : lines) {
                event.getGuiGraphics().drawString(font, line, drawX, drawY, 0xFFFFFF, false);
                drawY += lineHeight;
            }
        }
    }
}