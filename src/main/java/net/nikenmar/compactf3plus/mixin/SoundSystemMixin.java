package net.nikenmar.compactf3plus.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.nikenmar.compactf3plus.CompactF3Plus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Fabric has no PlaySoundEvent equivalent, so the music track line needs this hook.
// The NeoForge branches use net.neoforged.neoforge.client.event.sound.PlaySoundEvent
// instead and carry no mixin at all.
@Mixin(SoundSystem.class)
public class SoundSystemMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"))
    private void compactf3plus$onPlay(SoundInstance instance, CallbackInfo ci) {
        CompactF3Plus.onSoundPlay(instance);
    }
}
