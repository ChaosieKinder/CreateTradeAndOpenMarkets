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

    public static final ModConfigSpec.LongValue TREASURY_INCOME_PER_VILLAGER = BUILDER
            .comment(
                    "New money: what a village's treasury earns each day for each of its villagers, in copper bits.",
                    "This is where money enters the economy; everything else moves it around."
            )
            .defineInRange("treasuryIncomePerVillager", 100L, 0L, Long.MAX_VALUE / 1024);

    public static final ModConfigSpec.LongValue TREASURY_CEILING = BUILDER
            .comment("A treasury stops earning once it holds this much, in copper bits. 100000 is $1,000.00.")
            .defineInRange("treasuryCeiling", 100000L, 0L, Long.MAX_VALUE / 2);

    public static final ModConfigSpec.LongValue WAGE = BUILDER
            .comment("A novice villager's daily wage, paid from its village's treasury while the treasury has it.")
            .defineInRange("wage", 100L, 0L, Long.MAX_VALUE / 1024);

    public static final ModConfigSpec.LongValue WAGE_PER_LEVEL = BUILDER
            .comment("Added to the daily wage for each level above novice.")
            .defineInRange("wagePerLevel", 50L, 0L, Long.MAX_VALUE / 1024);

    public static final ModConfigSpec.IntValue UPKEEP_PERCENT = BUILDER
            .comment("The share of a villager's balance above the upkeep floor it pays into the treasury each day.")
            .defineInRange("upkeepPercent", 10, 0, 100);

    public static final ModConfigSpec.LongValue UPKEEP_FLOOR = BUILDER
            .comment("A villager keeps this much, in copper bits, free of upkeep.")
            .defineInRange("upkeepFloor", 2000L, 0L, Long.MAX_VALUE / 2);

    public static final ModConfigSpec.IntValue RESTOCK_COST_PERCENT = BUILDER
            .comment(
                    "What restocking a good costs a villager, as a share of the price it sold it for. Paid at",
                    "its next day's settlement, and destroyed: money leaving the economy."
            )
            .defineInRange("restockCostPercent", 50, 0, 100);

    public static final ModConfigSpec.IntValue MAX_SETTLEMENT_DAYS = BUILDER
            .comment(
                    "Days are settled when a villager is next traded with. At most this many are settled at",
                    "once, so a village left alone for a year does not pay a year of wages."
            )
            .defineInRange("maxSettlementDays", 3, 1, 365);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
