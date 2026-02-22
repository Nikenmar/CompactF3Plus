package net.nikenmar.compactf3plus;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CompactF3PlusConfig {
        public static final ForgeConfigSpec SPEC;

        public static final ForgeConfigSpec.BooleanValue showFps;
        public static final ForgeConfigSpec.BooleanValue showSystem;
        public static final ForgeConfigSpec.BooleanValue showLag;
        public static final ForgeConfigSpec.BooleanValue showTps;
        public static final ForgeConfigSpec.BooleanValue showCoords;
        public static final ForgeConfigSpec.BooleanValue showSubchunk;
        public static final ForgeConfigSpec.BooleanValue showLocalDifficulty;
        public static final ForgeConfigSpec.BooleanValue showEntities;
        public static final ForgeConfigSpec.BooleanValue showSession;
        public static final ForgeConfigSpec.BooleanValue showPing;
        public static final ForgeConfigSpec.BooleanValue showSpeed;
        public static final ForgeConfigSpec.BooleanValue detailedSpeed;
        public static final ForgeConfigSpec.BooleanValue showFacing;
        public static final ForgeConfigSpec.BooleanValue showPitch;
        public static final ForgeConfigSpec.BooleanValue showTime;
        public static final ForgeConfigSpec.BooleanValue showDay;
        public static final ForgeConfigSpec.BooleanValue showLight;
        public static final ForgeConfigSpec.BooleanValue showBiome;
        public static final ForgeConfigSpec.BooleanValue showDimension;
        public static final ForgeConfigSpec.BooleanValue colorIndicators;
        public static final ForgeConfigSpec.BooleanValue textShadow;
        public static final ForgeConfigSpec.BooleanValue replaceF3;
        public static final ForgeConfigSpec.BooleanValue showGizmo;
        public static final ForgeConfigSpec.BooleanValue enabledByDefault;
        public static final ForgeConfigSpec.IntValue backgroundOpacity;

        static {
                ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

                builder.comment("HUD Section Toggles").push("sections");

                showFps = builder.comment("Show FPS line").define("showFps", true);
                showSystem = builder.comment("Show RAM line").define("showSystem", true);
                showLag = builder.comment("Show Lag (Stutters) line").define("showLag", true);
                showTps = builder.comment("Show TPS line").define("showTps", false);
                showCoords = builder.comment("Show XYZ coordinates line").define("showCoords", true);
                showSubchunk = builder.comment("Show chunk/slime info line").define("showSubchunk", false);
                showLocalDifficulty = builder.comment("Show local difficulty line").define("showLocalDifficulty",
                                false);
                showEntities = builder.comment("Show entities count line").define("showEntities", false);
                showSession = builder.comment("Show session time line").define("showSession", true);
                showPing = builder.comment("Show ping line").define("showPing", true);
                showSpeed = builder.comment("Show speed section").define("showSpeed", true);
                showFacing = builder.comment("Show facing direction line").define("showFacing", true);
                showPitch = builder.comment("Show head pitch (vertical angle) line").define("showPitch", false);
                showTime = builder.comment("Show in-game time line").define("showTime", true);
                showDay = builder.comment("Show in-game day line").define("showDay", true);
                showLight = builder.comment("Show light level line").define("showLight", true);
                showBiome = builder.comment("Show biome line").define("showBiome", true);
                showDimension = builder.comment("Show dimension line").define("showDimension", false);

                builder.pop();

                builder.comment("Visual Settings").push("visuals");

                colorIndicators = builder
                                .comment("Color-code FPS and TPS values (green/yellow/red)")
                                .define("colorIndicators", false);

                textShadow = builder
                                .comment("Render text with shadow")
                                .define("textShadow", false);

                detailedSpeed = builder
                                .comment("Show detailed speed information")
                                .define("detailedSpeed", false);

                replaceF3 = builder
                                .comment("Replace the default F3 debug screen with the Compact F3 Plus overlay")
                                .define("replaceF3", true);

                showGizmo = builder
                                .comment("Show the XYZ axis gizmo when F3 is replaced (only works when replaceF3 is true)")
                                .define("showGizmo", false);

                enabledByDefault = builder
                                .comment("Enable the Compact HUD by default when joining a world")
                                .define("enabledByDefault", false);

                backgroundOpacity = builder
                                .comment("Background opacity percentage (0-100)")
                                .defineInRange("backgroundOpacity", 25, 0, 100);

                builder.pop();

                SPEC = builder.build();
        }

        public static void resetToDefaults() {
                showFps.set(true);
                showSystem.set(true);
                showLag.set(true);
                showTps.set(false);
                showCoords.set(true);
                showSubchunk.set(false);
                showLocalDifficulty.set(false);
                showEntities.set(false);
                showSession.set(true);
                showPing.set(true);
                showSpeed.set(true);
                showFacing.set(true);
                showPitch.set(false);
                showTime.set(true);
                showDay.set(true);
                showLight.set(true);
                showBiome.set(true);
                showDimension.set(false);

                colorIndicators.set(false);
                textShadow.set(false);
                detailedSpeed.set(false);
                replaceF3.set(true);
                showGizmo.set(false);
                enabledByDefault.set(false);
                backgroundOpacity.set(25);
                SPEC.save();
        }

        private CompactF3PlusConfig() {
        }
}
