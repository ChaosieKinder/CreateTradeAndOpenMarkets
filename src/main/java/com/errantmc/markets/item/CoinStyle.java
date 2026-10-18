package com.errantmc.markets.item;

import com.errantmc.markets.domain.CoinDenomination;
import net.minecraft.ChatFormatting;

/** How each denomination looks in text: the colour of its metal. */
public final class CoinStyle {
    private CoinStyle() {}

    public static ChatFormatting colour(CoinDenomination denomination) {
        return switch (denomination) {
            case COPPER_BIT -> ChatFormatting.DARK_RED;
            case COPPER_SHARD -> ChatFormatting.RED;
            case SILVER_BIT -> ChatFormatting.DARK_GRAY;
            case SILVER_CROWN -> ChatFormatting.GRAY;
            case GOLD_MARK -> ChatFormatting.YELLOW;
            case GOLD_REGAL -> ChatFormatting.GOLD;
            case EMERALD_SUN -> ChatFormatting.DARK_GREEN;
            case EMERALD_THRONE -> ChatFormatting.GREEN;
            case NONE -> ChatFormatting.WHITE;
        };
    }
}
