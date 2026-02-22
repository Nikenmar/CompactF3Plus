package net.nikenmar.compactf3plus;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class CompactF3PlusConfigScreen extends Screen {
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SPACING = 24;
    private static final int CONTENT_TOP = 40;

    private final Screen parent;
    private final List<ConfigEntry> entries = new ArrayList<ConfigEntry>();
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;

    public CompactF3PlusConfigScreen(Screen parent) {
        super(new StringTextComponent("Compact F3 Plus Settings"));
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
        entries.add(new ToggleEntry("Enabled by Default", CompactF3PlusConfig.enabledByDefault));
        entries.add(new ToggleEntry("Color Indicators (FPS/TPS)", CompactF3PlusConfig.colorIndicators));
        entries.add(new ToggleEntry("Text Shadow", CompactF3PlusConfig.textShadow));
        entries.add(new ToggleEntry("Detailed Speed", CompactF3PlusConfig.detailedSpeed));
        entries.add(new IntCycleEntry("Background Opacity", CompactF3PlusConfig.backgroundOpacity, 5, 0, 100, "%"));

        scrollOffset = Math.max(0, Math.min(getMaxScroll(), scrollOffset));
        layoutButtons();
    }

    private int getContentHeight() {
        return entries.size() * SPACING;
    }

    private int getViewHeight() {
        return Math.max(1, this.height - 90);
    }

    private int getMaxScroll() {
        return Math.max(0, getContentHeight() - getViewHeight());
    }

    private void layoutButtons() {
        this.buttons.clear();
        this.children.clear();

        int btnWidth = 200;
        int btnHeight = 20;
        int centerX = this.width / 2 - btnWidth / 2;

        for (int i = 0; i < entries.size(); i++) {
            int y = CONTENT_TOP + i * SPACING - scrollOffset;
            if (y < CONTENT_TOP - btnHeight || y > this.height - 50) {
                continue;
            }

            ConfigEntry entry = entries.get(i);
            if (entry instanceof ToggleEntry) {
                ToggleEntry toggle = (ToggleEntry) entry;
                Button button = new Button(
                        centerX, y, btnWidth, btnHeight,
                        new StringTextComponent(toggle.label + ": " + (toggle.value.get() ? "ON" : "OFF")),
                        btn -> {
                            toggle.value.set(!toggle.value.get());
                            CompactF3PlusConfig.SPEC.save();
                            btn.setMessage(new StringTextComponent(toggle.label + ": " + (toggle.value.get() ? "ON" : "OFF")));
                        });
                this.addButton(button);
            } else if (entry instanceof IntCycleEntry) {
                IntCycleEntry cycle = (IntCycleEntry) entry;
                Button button = new Button(
                        centerX, y, btnWidth, btnHeight,
                        new StringTextComponent(cycle.label + ": " + cycle.value.get() + cycle.suffix),
                        btn -> {
                            int current = cycle.value.get();
                            int next = current + cycle.step;
                            if (next > cycle.max) {
                                next = cycle.min;
                            }
                            cycle.value.set(next);
                            CompactF3PlusConfig.SPEC.save();
                            btn.setMessage(new StringTextComponent(cycle.label + ": " + cycle.value.get() + cycle.suffix));
                        });
                this.addButton(button);
            }
        }

        this.addButton(new Button(this.width / 2 - 155, this.height - 28, 150, 20, new StringTextComponent("Reset to Default"), btn -> {
            CompactF3PlusConfig.resetToDefaults();
            layoutButtons();
        }));

        this.addButton(new Button(this.width / 2 + 5, this.height - 28, 150, 20, new StringTextComponent("Done"), btn -> closeScreen()));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTick);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) instanceof HeaderEntry) {
                HeaderEntry header = (HeaderEntry) entries.get(i);
                int y = CONTENT_TOP + i * SPACING - scrollOffset + 6;
                if (y >= CONTENT_TOP && y <= this.height - 50) {
                    drawCenteredString(matrixStack, this.font, header.title, this.width / 2, y, 0xAAAAAA);
                }
            }
        }

        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            int trackX = this.width / 2 + 110;
            int trackTop = CONTENT_TOP;
            int trackBottom = this.height - 50;
            int trackHeight = trackBottom - trackTop;

            fill(matrixStack, trackX, trackTop, trackX + SCROLLBAR_WIDTH, trackBottom, 0x40FFFFFF);

            int thumbHeight = Math.max(15, trackHeight * getViewHeight() / getContentHeight());
            int thumbY = trackTop + (int) ((float) scrollOffset / maxScroll * (trackHeight - thumbHeight));
            fill(matrixStack, trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xAAFFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int nextScroll = Math.max(0, Math.min(getMaxScroll(), scrollOffset - (int) (delta * 10)));
        if (nextScroll != scrollOffset) {
            scrollOffset = nextScroll;
            layoutButtons();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && getMaxScroll() > 0) {
            int trackX = this.width / 2 + 110;
            if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_WIDTH
                    && mouseY >= CONTENT_TOP && mouseY <= this.height - 50) {
                draggingScrollbar = true;
                scrollToMouse(mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
        }
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
        int trackHeight = this.height - 50 - trackTop;
        if (trackHeight <= 0) {
            return;
        }

        float ratio = (float) (mouseY - trackTop) / trackHeight;
        int nextScroll = Math.max(0, Math.min(getMaxScroll(), (int) (ratio * getMaxScroll())));
        if (nextScroll != scrollOffset) {
            scrollOffset = nextScroll;
            layoutButtons();
        }
    }

    @Override
    public void closeScreen() {
        this.minecraft.displayGuiScreen(parent);
    }

    private interface ConfigEntry {
    }

    private static final class HeaderEntry implements ConfigEntry {
        private final StringTextComponent title;

        private HeaderEntry(String title) {
            this.title = new StringTextComponent(title);
        }
    }

    private static final class ToggleEntry implements ConfigEntry {
        private final String label;
        private final ForgeConfigSpec.BooleanValue value;

        private ToggleEntry(String label, ForgeConfigSpec.BooleanValue value) {
            this.label = label;
            this.value = value;
        }
    }

    private static final class IntCycleEntry implements ConfigEntry {
        private final String label;
        private final ForgeConfigSpec.IntValue value;
        private final int step;
        private final int min;
        private final int max;
        private final String suffix;

        private IntCycleEntry(String label, ForgeConfigSpec.IntValue value, int step, int min, int max, String suffix) {
            this.label = label;
            this.value = value;
            this.step = step;
            this.min = min;
            this.max = max;
            this.suffix = suffix;
        }
    }
}
