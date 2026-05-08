package net.nikenmar.compactf3plus;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Loader;

import java.io.File;

public class CompactF3PlusConfig {
    public static Configuration config;

    public static boolean showFps = true;
    public static boolean showSystem = true;
    public static boolean showLag = true;
    public static boolean showTps = false;
    public static boolean showCoords = true;
    public static boolean showSubchunk = false;
    public static boolean showLocalDifficulty = false;
    public static boolean showEntities = false;
    public static boolean showSession = true;
    public static boolean showPing = true;
    public static boolean showSpeed = true;
    public static boolean detailedSpeed = false;
    public static boolean showFacing = true;
    public static boolean showPitch = false;
    public static boolean showTime = true;
    public static boolean showDay = true;
    public static boolean showLight = true;
    public static boolean showBiome = true;
    public static boolean showDimension = false;

    public static boolean colorIndicators = false;
    public static boolean textShadow = false;
    public static boolean replaceF3 = true;
    public static boolean showGizmo = false;
    public static boolean enabledByDefault = false;
    public static int backgroundOpacity = 25;

    public static void init() {
        if (config == null) {
            config = new Configuration(new File(Loader.instance().getConfigDir(), "compactf3plus.cfg"));
            config.load();
        }
        syncConfig();
    }

    public static void syncConfig() {
        try {

            String cSections = "HUD Sections";
            config.setCategoryComment(cSections, "Toggle which lines and sections are visible on the HUD.");

            showFps = config.getBoolean("Show FPS", cSections, true, "Show FPS line");
            showSystem = config.getBoolean("Show RAM", cSections, true, "Show RAM line");
            showLag = config.getBoolean("Show Lag", cSections, true, "Show Lag (Stutters) line");
            showTps = config.getBoolean("Show TPS", cSections, false, "Show TPS line");
            showCoords = config.getBoolean("Show Coordinates", cSections, true, "Show XYZ coordinates line");
            showSubchunk = config.getBoolean("Show Chunk/Slime", cSections, false, "Show chunk/slime info line");
            showLocalDifficulty = config.getBoolean("Show Local Difficulty", cSections, false, "Show local difficulty line");
            showEntities = config.getBoolean("Show Entities Count", cSections, false, "Show entities count line");
            showSession = config.getBoolean("Show Session", cSections, true, "Show session time line");
            showPing = config.getBoolean("Show Ping", cSections, true, "Show ping line");
            showSpeed = config.getBoolean("Show Speed", cSections, true, "Show speed section");
            detailedSpeed = config.getBoolean("Detailed Speed", cSections, false, "Show detailed speed information");
            showFacing = config.getBoolean("Show Facing", cSections, true, "Show facing direction line");
            showPitch = config.getBoolean("Show Pitch", cSections, false, "Show head pitch (vertical angle) line");
            showTime = config.getBoolean("Show Time", cSections, true, "Show in-game time line");
            showDay = config.getBoolean("Show Day", cSections, true, "Show in-game day line");
            showLight = config.getBoolean("Show Light", cSections, true, "Show light level line");
            showBiome = config.getBoolean("Show Biome", cSections, true, "Show biome line");
            showDimension = config.getBoolean("Show Dimension", cSections, false, "Show dimension line");

            String cVisuals = "Visuals";
            config.setCategoryComment(cVisuals, "Visual settings for the HUD.");

            colorIndicators = config.getBoolean("Color Indicators", cVisuals, false, "Color-code FPS and TPS values");
            textShadow = config.getBoolean("Text Shadow", cVisuals, false, "Render text with shadow");
            replaceF3 = config.getBoolean("Replace Default F3", cVisuals, true, "Replace the default F3 debug screen");
            showGizmo = config.getBoolean("Show Gizmo", cVisuals, false, "Show the XYZ axis gizmo when F3 is replaced");
            enabledByDefault = config.getBoolean("Enabled by Default", cVisuals, false, "Enable HUD by default");
            backgroundOpacity = config.getInt("Background Opacity", cVisuals, 25, 0, 100, "Background opacity percentage (0-100)");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}
