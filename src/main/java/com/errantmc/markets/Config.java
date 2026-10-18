package com.errantmc.markets;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    static {
        BUILDER.push("identityCard");
    }

    public static final ModConfigSpec.IntValue IDENTITY_CARD_SYNC_INTERVAL_TICKS = BUILDER
            .comment(
                    "Number of ticks between updates of identity card player information to clients.",
                    "Default is 1200 ticks (approximately 60 seconds)."
            )
            .defineInRange("syncIntervalTicks", 1200, 20, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
        BUILDER.push("trading");
    }

    public static final ModConfigSpec.IntValue EMERALD_VALUE = BUILDER
            .comment(
                    "What one emerald in a vanilla trade is worth, in copper bits, when a villager without a",
                    "shop definition has its vanilla offers converted. 100 copper bits is one silver crown ($1.00)."
            )
            .defineInRange("emeraldValue", 100, 1, 100_000);

    public static final ModConfigSpec.IntValue BITS_PER_VILLAGER_XP = BUILDER
            .comment(
                    "Copper bits of trade per point of villager (and player) experience. Experience scales",
                    "with what a trade is worth, not with how many trades are made. 50 makes a one-emerald",
                    "vanilla trade worth 2 points, as most novice trades are."
            )
            .defineInRange("bitsPerVillagerXp", 50, 1, 1_000_000);

    static {
        BUILDER.pop();
        BUILDER.push("economy");
    }

    public static final ModConfigSpec.LongValue STARTING_BALANCE = BUILDER
            .comment(
                    "What a villager has in its till the first time it trades, in copper bits, at novice level.",
                    "A villager pays players only from its till, so this is how much it can buy before it",
                    "has sold something back. 1000 copper bits is $10.00."
            )
            .defineInRange("startingBalance", 1000L, 0L, Long.MAX_VALUE / 2);

    public static final ModConfigSpec.LongValue STARTING_BALANCE_PER_LEVEL = BUILDER
            .comment("Added to the starting balance for each level above novice (a master gets four times this more).")
            .defineInRange("startingBalancePerLevel", 1000L, 0L, Long.MAX_VALUE / 8);

    public static final ModConfigSpec.LongValue TREASURY_STARTING_BALANCE = BUILDER
            .comment(
                    "What a village's treasury holds when it is first used, in copper bits. A village is the",
                    "bell its villagers meet at; its banker trades from the treasury. 10000 copper bits is $100.00."
            )
            .defineInRange("treasuryStartingBalance", 10000L, 0L, Long.MAX_VALUE / 2);

    public static final ModConfigSpec.IntValue TRADE_FEE_PERCENT = BUILDER
            .comment(
                    "The share of every trade's price a villager pays into its village's treasury, rounded down.",
                    "A villager in no village pays none, nor does the banker, who trades from the treasury.",
                    "When a villager buys from a player, it must afford the price and the fee."
            )
            .defineInRange("tradeFeePercent", 5, 0, 100);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
