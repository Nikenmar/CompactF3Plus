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

    // Pre-hook for the whole HUD: run the F3 state machine and CLEAR the vanilla
    // debug flag before anything else draws. Cancelling the debug HUD render (what
    // this branch used to do) left shouldShowDebugHud() true for the entire frame,
    // so every mod that hides its own HUD while F3 is open went blank whenever our
    // compact HUD was opened with F3.
    @Inject(method = "render", at = @At("HEAD"))
    private void compactf3plus$beforeHud(DrawContext guiGraphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        CompactF3Plus.onPreRenderHud();
    }

    // The 3D gizmo is drawn by the vanilla crosshair render, gated on the same flag
    // we just cleared — so switch it back on for exactly that call when the gizmo is
    // wanted. This is the inverse of what the mixin used to do.
    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void compactf3plus$beforeRenderCrosshair(DrawContext guiGraphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (CompactF3Plus.wantGizmo()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!mc.getDebugHud().shouldShowDebugHud()) {
                mc.getDebugHud().toggleDebugHud();
                compactf3plus$toggledDebugForCrosshair = true;
            }
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void compactf3plus$afterRenderCrosshair(DrawContext guiGraphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
        if (compactf3plus$toggledDebugForCrosshair) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getDebugHud().shouldShowDebugHud()) {
                mc.getDebugHud().toggleDebugHud();
            }
            compactf3plus$toggledDebugForCrosshair = false;
        }
    }
}
