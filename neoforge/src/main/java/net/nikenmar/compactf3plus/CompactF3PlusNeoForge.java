package net.nikenmar.compactf3plus;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("compactf3plus")
public class CompactF3PlusNeoForge {

    public CompactF3PlusNeoForge(IEventBus modBus, ModContainer modContainer) {
        CompactF3PlusConfig.load();

        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> new CompactF3PlusConfigScreen(parent));

        modBus.addListener(this::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPreRenderGui);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);
        NeoForge.EVENT_BUS.addListener(this::onPlaySound);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CompactHudRenderer.TOGGLE_HUD);
    }

    private void onPlayerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        CompactHudRenderer.onPlayerJoin();
    }

    private void onPreRenderGui(RenderGuiEvent.Pre event) {
        CompactHudRenderer.onPreHudExtract();
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        CompactHudRenderer.onRenderHud(event.getGuiGraphics(), event.getPartialTick());
    }

    private void onPlaySound(PlaySoundEvent event) {
        CompactHudRenderer.onSoundPlay(event.getSound());
    }
}
