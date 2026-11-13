package com.fapcraftport.sexmod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SKIP_ADULT_WARNING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SKIP_ADULT_WARNING = builder
                .comment("Set to true after confirming you are 18+ with \"don't ask again\" ticked.")
                .define("skipAdultWarning", false);
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
