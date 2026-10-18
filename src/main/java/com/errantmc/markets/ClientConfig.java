package com.errantmc.markets;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Settings that only affect the player's own client. */
public final class ClientConfig {
    private ClientConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue REPLACE_TRADING_SCREEN = BUILDER
            .comment("Work in progress: replace the villager trading screen with the new shop screen.",
                     "Its layout is unfinished, so it is off by default.")
            .define("replaceTradingScreen", false);

    static final ModConfigSpec SPEC = BUILDER.build();
}
