package net.nikenmar.compactf3plus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;

public class CompactF3PlusFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CompactF3PlusConfig.load();

        KeyMappingHelper.registerKeyMapping(CompactHudRenderer.TOGGLE_HUD);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CompactHudRenderer.onPlayerJoin();
        });

        HudElementRegistry.addFirst(
                Identifier.fromNamespaceAndPath("compactf3plus", "state_machine"),
                (graphics, delta) -> CompactHudRenderer.onPreHudExtract());
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("compactf3plus", "overlay"),
                CompactHudRenderer::onRenderHud);
    }
}
