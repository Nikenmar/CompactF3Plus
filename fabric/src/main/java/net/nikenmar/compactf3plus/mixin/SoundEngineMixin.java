package net.nikenmar.compactf3plus.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.nikenmar.compactf3plus.CompactHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Inject(method = "play", at = @At("HEAD"))
    private void compactf3plus$onPlay(SoundInstance instance, CallbackInfoReturnable<?> cir) {
        CompactHudRenderer.onSoundPlay(instance);
    }
}
