package net.nikenmar.compactf3plus;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class CompactF3PlusConfigScreen {
    private CompactF3PlusConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Compact F3 Plus Settings"));
        final boolean[] resetRequested = {false};
        builder.setSavingRunnable(() -> {
            if (resetRequested[0]) {
                CompactF3PlusConfig.resetToDefaults();
                resetRequested[0] = false;
            } else {
                CompactF3PlusConfig.save();
            }
        });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory sections = builder.getOrCreateCategory(Text.literal("HUD Sections"));
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show FPS"), CompactF3PlusConfig.showFps)
                .setSaveConsumer(value -> CompactF3PlusConfig.showFps = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show RAM"), CompactF3PlusConfig.showSystem)
                .setSaveConsumer(value -> CompactF3PlusConfig.showSystem = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Lag"), CompactF3PlusConfig.showLag)
                .setSaveConsumer(value -> CompactF3PlusConfig.showLag = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show TPS"), CompactF3PlusConfig.showTps)
                .setSaveConsumer(value -> CompactF3PlusConfig.showTps = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Coordinates"), CompactF3PlusConfig.showCoords)
                .setSaveConsumer(value -> CompactF3PlusConfig.showCoords = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Subchunk/Slime"), CompactF3PlusConfig.showSubchunk)
                .setSaveConsumer(value -> CompactF3PlusConfig.showSubchunk = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Local Difficulty"), CompactF3PlusConfig.showLocalDifficulty)
                .setSaveConsumer(value -> CompactF3PlusConfig.showLocalDifficulty = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Entities Count"), CompactF3PlusConfig.showEntities)
                .setSaveConsumer(value -> CompactF3PlusConfig.showEntities = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Session"), CompactF3PlusConfig.showSession)
                .setSaveConsumer(value -> CompactF3PlusConfig.showSession = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Ping"), CompactF3PlusConfig.showPing)
                .setSaveConsumer(value -> CompactF3PlusConfig.showPing = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Speed"), CompactF3PlusConfig.showSpeed)
                .setSaveConsumer(value -> CompactF3PlusConfig.showSpeed = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Facing"), CompactF3PlusConfig.showFacing)
                .setSaveConsumer(value -> CompactF3PlusConfig.showFacing = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Pitch"), CompactF3PlusConfig.showPitch)
                .setSaveConsumer(value -> CompactF3PlusConfig.showPitch = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Time"), CompactF3PlusConfig.showTime)
                .setSaveConsumer(value -> CompactF3PlusConfig.showTime = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Day"), CompactF3PlusConfig.showDay)
                .setSaveConsumer(value -> CompactF3PlusConfig.showDay = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Light"), CompactF3PlusConfig.showLight)
                .setSaveConsumer(value -> CompactF3PlusConfig.showLight = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Biome"), CompactF3PlusConfig.showBiome)
                .setSaveConsumer(value -> CompactF3PlusConfig.showBiome = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Dimension"), CompactF3PlusConfig.showDimension)
                .setSaveConsumer(value -> CompactF3PlusConfig.showDimension = value)
                .build());

        ConfigCategory visuals = builder.getOrCreateCategory(Text.literal("Visuals"));
        visuals.addEntry(entryBuilder.startBooleanToggle(Text.literal("Replace Default F3"), CompactF3PlusConfig.replaceF3)
                .setSaveConsumer(value -> CompactF3PlusConfig.replaceF3 = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Gizmo (if Replace F3)"), CompactF3PlusConfig.showGizmo)
                .setSaveConsumer(value -> CompactF3PlusConfig.showGizmo = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enabled by Default"), CompactF3PlusConfig.enabledByDefault)
                .setSaveConsumer(value -> CompactF3PlusConfig.enabledByDefault = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(Text.literal("Color Indicators (FPS/TPS)"), CompactF3PlusConfig.colorIndicators)
                .setSaveConsumer(value -> CompactF3PlusConfig.colorIndicators = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(Text.literal("Text Shadow"), CompactF3PlusConfig.textShadow)
                .setSaveConsumer(value -> CompactF3PlusConfig.textShadow = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(Text.literal("Detailed Speed"), CompactF3PlusConfig.detailedSpeed)
                .setSaveConsumer(value -> CompactF3PlusConfig.detailedSpeed = value)
                .build());
        visuals.addEntry(entryBuilder.startIntSlider(Text.literal("Background Opacity"), CompactF3PlusConfig.backgroundOpacity, 0, 100)
                .setSaveConsumer(value -> CompactF3PlusConfig.backgroundOpacity = value)
                .build());

        ConfigCategory actions = builder.getOrCreateCategory(Text.literal("Actions"));
        actions.addEntry(entryBuilder.startTextDescription(Text.literal("Use the button below to reset all values.")).build());
        actions.addEntry(entryBuilder.startBooleanToggle(Text.literal("Reset to Default"), false)
                .setYesNoTextSupplier(value -> value ? Text.literal("Confirm") : Text.literal("Click to arm"))
                .setSaveConsumer(value -> resetRequested[0] = value)
                .build());

        return builder.build();
    }
}
