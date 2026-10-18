package com.errantmc.markets.economy;

import com.errantmc.markets.Config;
import com.errantmc.markets.domain.DailySettlement;
import com.errantmc.markets.npcinteractions.trading.VillagerBalance;
import com.errantmc.markets.villager.TradeVillagers;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import java.util.Optional;

/**
 * Where a villager's money is, on the server. A village is the meeting point (the bell) its
 * villagers remember; a villager with none belongs to no village, keeps its own till and pays no
 * fees. A banker in a village trades from the village's treasury itself: it is the one who
 * manages it (economy §7).
 */
public final class Tills {
    private Tills() {}

    public static Optional<GlobalPos> village(Villager villager) {
        return villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT);
    }

    /** The treasury of the villager's village, if it has one. */
    public static Optional<Account> treasury(Villager villager) {
        if (villager.getServer() == null) return Optional.empty();
        Treasuries treasuries = Treasuries.get(villager.getServer());
        return village(villager).map(treasuries::account);
    }

    /** What the villager trades from: the treasury for a village's banker, else its own balance. */
    public static Account till(Villager villager) {
        if (isBanker(villager)) {
            Optional<Account> treasury = treasury(villager);
            if (treasury.isPresent()) return treasury.get();
        }
        return VillagerBalance.account(villager);
    }

    /** Whether {@link #till} is the village treasury. */
    public static boolean tradesFromTreasury(Villager villager) {
        return isBanker(villager) && village(villager).isPresent();
    }

    /**
     * What a trade of {@code price} owes the village: a share of it, rounded down. Nothing in no
     * village, and nothing for the banker, whose till is the treasury already.
     */
    public static long fee(Villager villager, long price) {
        if (village(villager).isEmpty() || tradesFromTreasury(villager)) return 0;
        return DailySettlement.percent(price, Config.TRADE_FEE_PERCENT.get());
    }

    private static boolean isBanker(Villager villager) {
        return villager.getVillagerData().getProfession() == TradeVillagers.BANKER_PROFESSION.value();
    }
}
