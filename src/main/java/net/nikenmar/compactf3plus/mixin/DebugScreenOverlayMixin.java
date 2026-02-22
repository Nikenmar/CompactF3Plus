package net.nikenmar.compactf3plus.mixin;

import net.minecraft.client.gui.hud.DebugHud;
import net.nikenmar.compactf3plus.CompactF3Plus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugHud.class)
public abstract class DebugScreenOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void compactf3plus$cancelDebugOverlay(CallbackInfo ci) {
        if (CompactF3Plus.shouldCancelVanillaDebugOverlay()) {
            ci.cancel();
        }
    }
}
