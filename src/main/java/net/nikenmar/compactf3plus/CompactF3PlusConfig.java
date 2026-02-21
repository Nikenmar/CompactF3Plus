package net.nikenmar.compactf3plus;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CompactF3PlusConfig {
        public static final ModConfigSpec SPEC;

        public static final ModConfigSpec.BooleanValue showFps;
        public static final ModConfigSpec.BooleanValue showSystem;
        public static final ModConfigSpec.BooleanValue showCoords;
        public static final ModConfigSpec.BooleanValue showSession;
        public static final ModConfigSpec.BooleanValue showSpeed;
        public static final ModConfigSpec.BooleanValue showFacing;
        public static final ModConfigSpec.BooleanValue showTime;
        public static final ModConfigSpec.BooleanValue showLight;
        public static final ModConfigSpec.BooleanValue showBiome;
        public static final ModConfigSpec.BooleanValue colorIndicators;
        public static final ModConfigSpec.BooleanValue replaceF3;
        public static final ModConfigSpec.BooleanValue showGizmo;

        static {
                ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

                builder.comment("HUD Section Toggles").push("sections");

                showFps = builder.comment("Show FPS line").define("showFps", true);
                showSystem = builder.comment("Show RAM/TPS line").define("showSystem", true);
                showCoords = builder.comment("Show XYZ coordinates line").define("showCoords", true);
                showSession = builder.comment("Show session time and ping line").define("showSession", true);
                showSpeed = builder.comment("Show speed section").define("showSpeed", true);
                showFacing = builder.comment("Show facing direction line").define("showFacing", true);
                showTime = builder.comment("Show in-game time and day line").define("showTime", true);
                showLight = builder.comment("Show light level line").define("showLight", true);
                showBiome = builder.comment("Show biome line").define("showBiome", true);

                builder.pop();

                builder.comment("Visual Settings").push("visuals");

                colorIndicators = builder
                                .comment("Color-code FPS and TPS values (green/yellow/red)")
                                .define("colorIndicators", false);

                replaceF3 = builder
                                .comment("Replace the default F3 debug screen with the Compact F3 Plus overlay")
                                .define("replaceF3", true);

                showGizmo = builder
                                .comment("Show the XYZ axis gizmo when F3 is replaced (only works when replaceF3 is true)")
                                .define("showGizmo", false);

                builder.pop();

                SPEC = builder.build();
        }

        private CompactF3PlusConfig() {
        }
}
