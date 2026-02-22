package net.nikenmar.compactf3plus;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class CompactF3PlusConfigScreen extends Screen {
    private final Screen parent;
    private final List<ConfigEntry> entries = new ArrayList<>();
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SPACING = 24;
    private static final int CONTENT_TOP = 40;

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

    @Override
    protected void init() {
        entries.clear();

        entries.add(new HeaderEntry("HUD Sections"));
        entries.add(new ToggleEntry("Show FPS", CompactF3PlusConfig.showFps));
        entries.add(new ToggleEntry("Show RAM", CompactF3PlusConfig.showSystem));
        entries.add(new ToggleEntry("Show Lag", CompactF3PlusConfig.showLag));
        entries.add(new ToggleEntry("Show TPS", CompactF3PlusConfig.showTps));
        entries.add(new ToggleEntry("Show Coordinates", CompactF3PlusConfig.showCoords));
        entries.add(new ToggleEntry("Show Subchunk/Slime", CompactF3PlusConfig.showSubchunk));
        entries.add(new ToggleEntry("Show Local Difficulty", CompactF3PlusConfig.showLocalDifficulty));
        entries.add(new ToggleEntry("Show Entities Count", CompactF3PlusConfig.showEntities));
        entries.add(new ToggleEntry("Show Session", CompactF3PlusConfig.showSession));
        entries.add(new ToggleEntry("Show Ping", CompactF3PlusConfig.showPing));
        entries.add(new ToggleEntry("Show Speed", CompactF3PlusConfig.showSpeed));
        entries.add(new ToggleEntry("Show Facing", CompactF3PlusConfig.showFacing));
        entries.add(new ToggleEntry("Show Pitch (Angle)", CompactF3PlusConfig.showPitch));
        entries.add(new ToggleEntry("Show Time", CompactF3PlusConfig.showTime));
        entries.add(new ToggleEntry("Show Day", CompactF3PlusConfig.showDay));
        entries.add(new ToggleEntry("Show Light", CompactF3PlusConfig.showLight));
        entries.add(new ToggleEntry("Show Biome", CompactF3PlusConfig.showBiome));
        entries.add(new ToggleEntry("Show Dimension", CompactF3PlusConfig.showDimension));
        entries.add(new HeaderEntry("Other"));
        entries.add(new ToggleEntry("Replace Default F3", CompactF3PlusConfig.replaceF3));
        entries.add(new ToggleEntry("Show Gizmo (if Replace F3)", CompactF3PlusConfig.showGizmo));
        entries.add(new ToggleEntry("Color Indicators (FPS/TPS)", CompactF3PlusConfig.colorIndicators));
        entries.add(new ToggleEntry("Text Shadow", CompactF3PlusConfig.textShadow));
        entries.add(new ToggleEntry("Detailed Speed", CompactF3PlusConfig.detailedSpeed));
        entries.add(new CycleOpacityEntry("Background Opacity", CompactF3PlusConfig.backgroundOpacity));

        layoutButtons();
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
                        Component.literal(toggle.label + ": " + (toggle.value.get() ? "ON" : "OFF")),
                        btn -> {
                            toggle.value.set(!toggle.value.get());
                            CompactF3PlusConfig.SPEC.save();
                            btn.setMessage(Component.literal(
                                    toggle.label + ": " + (toggle.value.get() ? "ON" : "OFF")));
                        })
                        .bounds(centerX, y, btnWidth, btnHeight)
                        .build());
            } else if (entry instanceof CycleOpacityEntry opacity) {
                addRenderableWidget(new AbstractSliderButton(centerX, y, btnWidth, btnHeight,
                        Component.literal(opacity.label + ": " + opacity.value.get() + "%"),
                        opacity.value.get() / 100.0D) {

                    @Override
                    protected void updateMessage() {
                        this.setMessage(Component.literal(opacity.label + ": " + opacity.value.get() + "%"));
                    }

                    @Override
                    protected void applyValue() {
                        int newValue = (int) Math.round(this.value * 100.0D);
                        opacity.value.set(newValue);
                        CompactF3PlusConfig.SPEC.save();
                    }
                });
            }
        }

        addRenderableWidget(Button.builder(Component.literal("Reset to Default"), btn -> {
            CompactF3PlusConfig.resetToDefaults();
            layoutButtons(); // Refresh the screen buttons to reflect the default values
        })
                .bounds(width / 2 - 155, height - 28, 150, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> onClose())
                .bounds(width / 2 + 5, height - 28, 150, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) instanceof HeaderEntry header) {
                int y = CONTENT_TOP + i * SPACING - scrollOffset + 6;
                if (y >= CONTENT_TOP && y <= height - 50) {
                    guiGraphics.drawCenteredString(font, header.title, width / 2, y, 0xAAAAAA);
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && getMaxScroll() > 0) {
            int trackX = width / 2 + 110;
            if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH
                    && mouseY >= CONTENT_TOP && mouseY <= height - 50) {
                draggingScrollbar = true;
                scrollToMouse(mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0)
            draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollToMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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

    private record ToggleEntry(String label, ModConfigSpec.BooleanValue value) implements ConfigEntry {
    }

    private record CycleOpacityEntry(String label, ModConfigSpec.IntValue value) implements ConfigEntry {
    }
}
