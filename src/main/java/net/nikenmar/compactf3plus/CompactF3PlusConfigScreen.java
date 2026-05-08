package net.nikenmar.compactf3plus;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public final class CompactF3PlusConfigScreen {
    private CompactF3PlusConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(new LiteralText("Compact F3 Plus Settings"));
        builder.setSavingRunnable(CompactF3PlusConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory sections = builder.getOrCreateCategory(new LiteralText("HUD Sections"));
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show FPS"), CompactF3PlusConfig.showFps)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showFps = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show RAM"), CompactF3PlusConfig.showSystem)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showSystem = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Lag"), CompactF3PlusConfig.showLag)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showLag = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show TPS"), CompactF3PlusConfig.showTps)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.showTps = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Coordinates"), CompactF3PlusConfig.showCoords)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showCoords = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Subchunk/Slime"), CompactF3PlusConfig.showSubchunk)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.showSubchunk = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Local Difficulty"), CompactF3PlusConfig.showLocalDifficulty)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.showLocalDifficulty = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Entities Count"), CompactF3PlusConfig.showEntities)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.showEntities = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Session"), CompactF3PlusConfig.showSession)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showSession = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Ping"), CompactF3PlusConfig.showPing)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showPing = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Speed"), CompactF3PlusConfig.showSpeed)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showSpeed = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Facing"), CompactF3PlusConfig.showFacing)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showFacing = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Pitch"), CompactF3PlusConfig.showPitch)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.showPitch = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Time"), CompactF3PlusConfig.showTime)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showTime = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Day"), CompactF3PlusConfig.showDay)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showDay = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Light"), CompactF3PlusConfig.showLight)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showLight = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Biome"), CompactF3PlusConfig.showBiome)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.showBiome = value)
                .build());
        sections.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Dimension"), CompactF3PlusConfig.showDimension)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.showDimension = value)
                .build());

        ConfigCategory visuals = builder.getOrCreateCategory(new LiteralText("Visuals"));
        visuals.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Replace Default F3"), CompactF3PlusConfig.replaceF3)
                .setDefaultValue(true)
                .setSaveConsumer(value -> CompactF3PlusConfig.replaceF3 = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Show Gizmo (if Replace F3)"), CompactF3PlusConfig.showGizmo)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.showGizmo = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Enabled by Default"), CompactF3PlusConfig.enabledByDefault)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.enabledByDefault = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Color Indicators (FPS/TPS)"), CompactF3PlusConfig.colorIndicators)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.colorIndicators = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Text Shadow"), CompactF3PlusConfig.textShadow)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.textShadow = value)
                .build());
        visuals.addEntry(entryBuilder.startBooleanToggle(new LiteralText("Detailed Speed"), CompactF3PlusConfig.detailedSpeed)
                .setDefaultValue(false)
                .setSaveConsumer(value -> CompactF3PlusConfig.detailedSpeed = value)
                .build());
        visuals.addEntry(entryBuilder.startIntSlider(new LiteralText("Background Opacity"), CompactF3PlusConfig.backgroundOpacity, 0, 100)
                .setDefaultValue(25)
                .setSaveConsumer(value -> CompactF3PlusConfig.backgroundOpacity = value)
                .build());

        return builder.build();
    }
}
