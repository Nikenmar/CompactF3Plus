package net.nikenmar.compactf3plus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CompactF3PlusConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("compactf3plus.json");

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

    private CompactF3PlusConfig() {
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                save();
                return;
            }

            showFps = getBoolean(root, "showFps", showFps);
            showSystem = getBoolean(root, "showSystem", showSystem);
            showLag = getBoolean(root, "showLag", showLag);
            showTps = getBoolean(root, "showTps", showTps);
            showCoords = getBoolean(root, "showCoords", showCoords);
            showSubchunk = getBoolean(root, "showSubchunk", showSubchunk);
            showLocalDifficulty = getBoolean(root, "showLocalDifficulty", showLocalDifficulty);
            showEntities = getBoolean(root, "showEntities", showEntities);
            showSession = getBoolean(root, "showSession", showSession);
            showPing = getBoolean(root, "showPing", showPing);
            showSpeed = getBoolean(root, "showSpeed", showSpeed);
            detailedSpeed = getBoolean(root, "detailedSpeed", detailedSpeed);
            showFacing = getBoolean(root, "showFacing", showFacing);
            showPitch = getBoolean(root, "showPitch", showPitch);
            showTime = getBoolean(root, "showTime", showTime);
            showDay = getBoolean(root, "showDay", showDay);
            showLight = getBoolean(root, "showLight", showLight);
            showBiome = getBoolean(root, "showBiome", showBiome);
            showDimension = getBoolean(root, "showDimension", showDimension);

            colorIndicators = getBoolean(root, "colorIndicators", colorIndicators);
            textShadow = getBoolean(root, "textShadow", textShadow);
            replaceF3 = getBoolean(root, "replaceF3", replaceF3);
            showGizmo = getBoolean(root, "showGizmo", showGizmo);
            enabledByDefault = getBoolean(root, "enabledByDefault", enabledByDefault);
            backgroundOpacity = clamp(getInt(root, "backgroundOpacity", backgroundOpacity), 0, 100);
        } catch (IOException ignored) {
            // Use in-memory defaults if file cannot be read.
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("showFps", showFps);
        root.addProperty("showSystem", showSystem);
        root.addProperty("showLag", showLag);
        root.addProperty("showTps", showTps);
        root.addProperty("showCoords", showCoords);
        root.addProperty("showSubchunk", showSubchunk);
        root.addProperty("showLocalDifficulty", showLocalDifficulty);
        root.addProperty("showEntities", showEntities);
        root.addProperty("showSession", showSession);
        root.addProperty("showPing", showPing);
        root.addProperty("showSpeed", showSpeed);
        root.addProperty("detailedSpeed", detailedSpeed);
        root.addProperty("showFacing", showFacing);
        root.addProperty("showPitch", showPitch);
        root.addProperty("showTime", showTime);
        root.addProperty("showDay", showDay);
        root.addProperty("showLight", showLight);
        root.addProperty("showBiome", showBiome);
        root.addProperty("showDimension", showDimension);

        root.addProperty("colorIndicators", colorIndicators);
        root.addProperty("textShadow", textShadow);
        root.addProperty("replaceF3", replaceF3);
        root.addProperty("showGizmo", showGizmo);
        root.addProperty("enabledByDefault", enabledByDefault);
        root.addProperty("backgroundOpacity", backgroundOpacity);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ignored) {
            // Ignore save failures.
        }
    }

    public static void resetToDefaults() {
        showFps = true;
        showSystem = true;
        showLag = true;
        showTps = false;
        showCoords = true;
        showSubchunk = false;
        showLocalDifficulty = false;
        showEntities = false;
        showSession = true;
        showPing = true;
        showSpeed = true;
        detailedSpeed = false;
        showFacing = true;
        showPitch = false;
        showTime = true;
        showDay = true;
        showLight = true;
        showBiome = true;
        showDimension = false;

        colorIndicators = false;
        textShadow = false;
        replaceF3 = true;
        showGizmo = false;
        enabledByDefault = false;
        backgroundOpacity = 25;
        save();
    }

    private static boolean getBoolean(JsonObject root, String key, boolean fallback) {
        return root.has(key) ? root.get(key).getAsBoolean() : fallback;
    }

    private static int getInt(JsonObject root, String key, int fallback) {
        return root.has(key) ? root.get(key).getAsInt() : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
