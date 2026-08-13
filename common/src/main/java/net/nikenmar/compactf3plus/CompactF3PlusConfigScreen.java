package net.nikenmar.compactf3plus;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class CompactF3PlusConfigScreen extends Screen {
    private final Screen parent;
    private final List<ConfigEntry> entries = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SPACING = 24;
    private static final int CONTENT_TOP = 40;
    // Index order must match CompactF3PlusConfig.hudAnchor.
    private static final String[] ANCHOR_NAMES = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};

    private int getContentHeight() {
        return entries.size() * SPACING;
    }

    private int getViewHeight() {
        return height - 90;
    }

    private int getMaxScroll() {
        return Math.max(0, getContentHeight() - getViewHeight());
    }

    public CompactF3PlusConfigScreen(Screen parent) {
        super(Component.literal("Compact F3 Plus Settings"));
        this.parent = parent;
    }

    public static CompactF3PlusConfigScreen create(Screen parent) {
        return new CompactF3PlusConfigScreen(parent);
    }

    @Override
    protected void init() {
        entries.clear();

        entries.add(new HeaderEntry("HUD Sections"));
        entries.add(toggle("Show FPS",            () -> CompactF3PlusConfig.showFps,            v -> CompactF3PlusConfig.showFps = v));
        entries.add(toggle("Show RAM",            () -> CompactF3PlusConfig.showSystem,         v -> CompactF3PlusConfig.showSystem = v));
        entries.add(toggle("Show Lag",            () -> CompactF3PlusConfig.showLag,            v -> CompactF3PlusConfig.showLag = v));
        entries.add(toggle("Show TPS",            () -> CompactF3PlusConfig.showTps,            v -> CompactF3PlusConfig.showTps = v));
        entries.add(toggle("Show Coordinates",    () -> CompactF3PlusConfig.showCoords,         v -> CompactF3PlusConfig.showCoords = v));
        entries.add(toggle("Show Subchunk/Slime", () -> CompactF3PlusConfig.showSubchunk,       v -> CompactF3PlusConfig.showSubchunk = v));
        entries.add(toggle("Show Local Difficulty", () -> CompactF3PlusConfig.showLocalDifficulty, v -> CompactF3PlusConfig.showLocalDifficulty = v));
        entries.add(toggle("Show Entities Count", () -> CompactF3PlusConfig.showEntities,       v -> CompactF3PlusConfig.showEntities = v));
        entries.add(toggle("Show Session",        () -> CompactF3PlusConfig.showSession,        v -> CompactF3PlusConfig.showSession = v));
        entries.add(toggle("Show Ping",           () -> CompactF3PlusConfig.showPing,           v -> CompactF3PlusConfig.showPing = v));
        entries.add(toggle("Show Speed",          () -> CompactF3PlusConfig.showSpeed,          v -> CompactF3PlusConfig.showSpeed = v));
        entries.add(toggle("Show Facing",         () -> CompactF3PlusConfig.showFacing,         v -> CompactF3PlusConfig.showFacing = v));
        entries.add(toggle("Show Pitch (Angle)",  () -> CompactF3PlusConfig.showPitch,          v -> CompactF3PlusConfig.showPitch = v));
        entries.add(toggle("Show Time",           () -> CompactF3PlusConfig.showTime,           v -> CompactF3PlusConfig.showTime = v));
        entries.add(toggle("Show Day",            () -> CompactF3PlusConfig.showDay,            v -> CompactF3PlusConfig.showDay = v));
        entries.add(toggle("Show Light",          () -> CompactF3PlusConfig.showLight,          v -> CompactF3PlusConfig.showLight = v));
        entries.add(toggle("Show Biome",          () -> CompactF3PlusConfig.showBiome,          v -> CompactF3PlusConfig.showBiome = v));
        entries.add(toggle("Show Dimension",      () -> CompactF3PlusConfig.showDimension,      v -> CompactF3PlusConfig.showDimension = v));
        entries.add(toggle("Show Durability",     () -> CompactF3PlusConfig.showDurability,     v -> CompactF3PlusConfig.showDurability = v));
        entries.add(toggle("Show Crop Growth",    () -> CompactF3PlusConfig.showCropGrowth,     v -> CompactF3PlusConfig.showCropGrowth = v));
        entries.add(toggle("Show Music Track",    () -> CompactF3PlusConfig.showMusicTrack,     v -> CompactF3PlusConfig.showMusicTrack = v));
        entries.add(toggle("Show Targeted Block", () -> CompactF3PlusConfig.showTargetBlock,    v -> CompactF3PlusConfig.showTargetBlock = v));
        entries.add(toggle("Show Targeted Fluid", () -> CompactF3PlusConfig.showTargetFluid,    v -> CompactF3PlusConfig.showTargetFluid = v));
        entries.add(toggle("Show Targeted Entity",() -> CompactF3PlusConfig.showTargetEntity,   v -> CompactF3PlusConfig.showTargetEntity = v));
        entries.add(toggle("Show Block Properties",() -> CompactF3PlusConfig.showTargetProperties, v -> CompactF3PlusConfig.showTargetProperties = v));

        entries.add(new HeaderEntry("Position"));
        entries.add(new CycleEntry("Anchor", () -> CompactF3PlusConfig.hudAnchor,
                v -> CompactF3PlusConfig.hudAnchor = v, ANCHOR_NAMES));
        entries.add(new IntEntry("Offset X", () -> CompactF3PlusConfig.hudOffsetX,
                v -> CompactF3PlusConfig.hudOffsetX = v, 0, CompactF3PlusConfig.MAX_HUD_OFFSET, "px"));
        entries.add(new IntEntry("Offset Y", () -> CompactF3PlusConfig.hudOffsetY,
                v -> CompactF3PlusConfig.hudOffsetY = v, 0, CompactF3PlusConfig.MAX_HUD_OFFSET, "px"));

        entries.add(new HeaderEntry("Other"));
        entries.add(toggle("Replace Default F3",          () -> CompactF3PlusConfig.replaceF3,         v -> CompactF3PlusConfig.replaceF3 = v));
        entries.add(toggle("Show Gizmo (if Replace F3)",  () -> CompactF3PlusConfig.showGizmo,         v -> CompactF3PlusConfig.showGizmo = v));
        entries.add(toggle("Enabled by Default",          () -> CompactF3PlusConfig.enabledByDefault,  v -> CompactF3PlusConfig.enabledByDefault = v));
        entries.add(toggle("Color Indicators (FPS/TPS)",  () -> CompactF3PlusConfig.colorIndicators,   v -> CompactF3PlusConfig.colorIndicators = v));
        entries.add(toggle("Text Shadow",                 () -> CompactF3PlusConfig.textShadow,        v -> CompactF3PlusConfig.textShadow = v));
        entries.add(toggle("Detailed Speed",              () -> CompactF3PlusConfig.detailedSpeed,     v -> CompactF3PlusConfig.detailedSpeed = v));
        entries.add(new IntEntry("Background Opacity", () -> CompactF3PlusConfig.backgroundOpacity,
                v -> CompactF3PlusConfig.backgroundOpacity = v, 0, 100, "%"));

        layoutButtons();
    }

    private static ToggleEntry toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
        return new ToggleEntry(label, get, set);
    }

    private void layoutButtons() {
        clearWidgets();

        int btnWidth = 200;
        int btnHeight = 20;
        int centerX = width / 2 - btnWidth / 2;

        for (int i = 0; i < entries.size(); i++) {
            int y = CONTENT_TOP + i * SPACING - scrollOffset;
            if (y < CONTENT_TOP - btnHeight || y > height - 50)
                continue;

            ConfigEntry entry = entries.get(i);
            if (entry instanceof ToggleEntry toggle) {
                addRenderableWidget(Button.builder(
                        Component.literal(toggle.label + ": " + (toggle.get.getAsBoolean() ? "ON" : "OFF")),
                        btn -> {
                            boolean next = !toggle.get.getAsBoolean();
                            toggle.set.accept(next);
                            CompactF3PlusConfig.save();
                            btn.setMessage(Component.literal(
                                    toggle.label + ": " + (next ? "ON" : "OFF")));
                        })
                        .bounds(centerX, y, btnWidth, btnHeight)
                        .build());
            } else if (entry instanceof CycleEntry cycle) {
                addRenderableWidget(Button.builder(
                        Component.literal(cycle.label + ": " + cycle.options[cycle.get.getAsInt()]),
                        btn -> {
                            int next = (cycle.get.getAsInt() + 1) % cycle.options.length;
                            cycle.set.accept(next);
                            CompactF3PlusConfig.save();
                            btn.setMessage(Component.literal(cycle.label + ": " + cycle.options[next]));
                        })
                        .bounds(centerX, y, btnWidth, btnHeight)
                        .build());
            } else if (entry instanceof IntEntry slider) {
                addRenderableWidget(new AbstractSliderButton(centerX, y, btnWidth, btnHeight,
                        Component.literal(slider.label + ": " + slider.get.getAsInt() + slider.suffix),
                        slider.normalize(slider.get.getAsInt())) {

                    @Override
                    protected void updateMessage() {
                        this.setMessage(Component.literal(
                                slider.label + ": " + slider.get.getAsInt() + slider.suffix));
                    }

                    @Override
                    protected void applyValue() {
                        slider.set.accept(slider.denormalize(this.value));
                        CompactF3PlusConfig.save();
                    }
                });
            }
        }

        addRenderableWidget(Button.builder(Component.literal("Reset to Default"), btn -> {
            CompactF3PlusConfig.resetToDefaults();
            layoutButtons();
        })
                .bounds(width / 2 - 155, height - 28, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> onClose())
                .bounds(width / 2 + 5, height - 28, 150, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        // 26.1: GuiGraphicsExtractor#text drops alpha=0 colors silently — use ARGB.
        guiGraphics.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) instanceof HeaderEntry header) {
                int y = CONTENT_TOP + i * SPACING - scrollOffset + 6;
                if (y >= CONTENT_TOP && y <= height - 50) {
                    guiGraphics.centeredText(font, header.title, width / 2, y, 0xFFAAAAAA);
                }
            }
        }

        // Scrollbar
        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            int trackX = width / 2 + 110;
            int trackTop = CONTENT_TOP;
            int trackBottom = height - 50;
            int trackHeight = trackBottom - trackTop;

            guiGraphics.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackBottom, 0x40FFFFFF);

            int thumbHeight = Math.max(15, trackHeight * getViewHeight() / getContentHeight());
            int thumbY = trackTop + (int) ((float) scrollOffset / maxScroll * (trackHeight - thumbHeight));
            guiGraphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xAAFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Math.max(0, Math.min(getMaxScroll(), scrollOffset - (int) (scrollY * 10)));
        rebuildWidgets();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && getMaxScroll() > 0) {
            int trackX = width / 2 + 110;
            double mouseX = event.x();
            double mouseY = event.y();
            if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH
                    && mouseY >= CONTENT_TOP && mouseY <= height - 50) {
                draggingScrollbar = true;
                scrollToMouse(mouseY);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0)
            draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollToMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private void scrollToMouse(double mouseY) {
        int trackTop = CONTENT_TOP;
        int trackHeight = height - 50 - trackTop;
        float ratio = (float) (mouseY - trackTop) / trackHeight;
        scrollOffset = Math.max(0, Math.min(getMaxScroll(), (int) (ratio * getMaxScroll())));
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private interface ConfigEntry {
    }

    private record HeaderEntry(String title) implements ConfigEntry {
    }

    private record ToggleEntry(String label, BooleanSupplier get, Consumer<Boolean> set) implements ConfigEntry {
    }

    private record CycleEntry(String label, IntSupplier get, IntConsumer set, String[] options)
            implements ConfigEntry {
    }

    private record IntEntry(String label, IntSupplier get, IntConsumer set, int min, int max, String suffix)
            implements ConfigEntry {

        double normalize(int value) {
            return (double) (value - min) / (max - min);
        }

        int denormalize(double sliderValue) {
            return min + (int) Math.round(sliderValue * (max - min));
        }
    }
}
