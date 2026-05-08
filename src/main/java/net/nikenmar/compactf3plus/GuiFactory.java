package net.nikenmar.compactf3plus;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return CompactF3PlusConfigGui.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class CompactF3PlusConfigGui extends GuiConfig {
        @SuppressWarnings("unchecked")
        public CompactF3PlusConfigGui(GuiScreen parent) {
            super(parent,
                    getAllElements(),
                    "compactf3plus",
                    false,
                    false,
                    "Compact F3 Plus Configuration");
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static List<IConfigElement> getAllElements() {
            List<IConfigElement> elements = new ArrayList<IConfigElement>();
            for (String categoryName : CompactF3PlusConfig.config.getCategoryNames()) {
                ConfigCategory category = CompactF3PlusConfig.config.getCategory(categoryName);
                elements.addAll(new ConfigElement(category).getChildElements());
            }
            return elements;
        }
    }
}
