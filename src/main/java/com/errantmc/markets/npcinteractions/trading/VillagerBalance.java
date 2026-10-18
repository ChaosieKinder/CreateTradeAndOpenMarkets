package com.errantmc.markets.npcinteractions.trading;

import com.errantmc.markets.Config;
import net.minecraft.world.entity.npc.Villager;

/**
 * A villager's till: the money it trades with, in copper bits (author's decision, 18 Oct: a balance,
 * not coins, so it can always make change). It pays players only from what it holds, and what
 * players pay it goes into it, so trading moves money rather than creating or destroying it
 * (docs/design/economy.md).
 */
public final class VillagerBalance {
    /** Never traded yet: the starting balance is given on first use. */
    public static final long UNSET = -1;

    private VillagerBalance() {}

    public static long get(Villager villager) {
        long balance = villager.getData(TradeAttachments.BALANCE);
        if (balance == UNSET) {
            balance = starting(villager);
            villager.setData(TradeAttachments.BALANCE, balance);
        }
        return balance;
    }

    /** Novice gets the base; each level above adds the per-level amount. */
    public static long starting(Villager villager) {
        int levelsAboveNovice = Math.max(0, villager.getVillagerData().getLevel() - 1);
        return Config.STARTING_BALANCE.get() + levelsAboveNovice * Config.STARTING_BALANCE_PER_LEVEL.get();
    }

    public static void deposit(Villager villager, long copperBits) {
        if (copperBits < 0) throw new IllegalArgumentException("negative deposit " + copperBits);
        villager.setData(TradeAttachments.BALANCE, get(villager) + copperBits);
    }

    /** Takes {@code copperBits} if the villager has them; otherwise takes nothing and says so. */
    public static boolean withdraw(Villager villager, long copperBits) {
        if (copperBits < 0) throw new IllegalArgumentException("negative withdrawal " + copperBits);
        long balance = get(villager);
        if (balance < copperBits) return false;
        villager.setData(TradeAttachments.BALANCE, balance - copperBits);
        return true;
    }
}
