package net.nikenmar.compactf3plus.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.nikenmar.compactf3plus.CompactF3Plus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class GuiMixin {
    @Unique
    private boolean compactf3plus$toggledDebugForCrosshair = false;

    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void compactf3plus$beforeRenderCrosshair(DrawContext guiGraphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (CompactF3Plus.shouldHideDebugCrosshair()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getDebugHud().shouldShowDebugHud()) {
                mc.getDebugHud().toggleDebugHud();
                compactf3plus$toggledDebugForCrosshair = true;
            }
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void compactf3plus$afterRenderCrosshair(DrawContext guiGraphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (compactf3plus$toggledDebugForCrosshair) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!mc.getDebugHud().shouldShowDebugHud()) {
                mc.getDebugHud().toggleDebugHud();
            }
            compactf3plus$toggledDebugForCrosshair = false;
        }
    }
}
