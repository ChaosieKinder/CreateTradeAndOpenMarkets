package com.errantmc.markets.economy;

import com.errantmc.markets.Config;
import com.errantmc.markets.domain.DailySettlement;
import com.errantmc.markets.domain.DailySettlement.Membership;
import com.errantmc.markets.npcinteractions.trading.TradeAttachments;
import com.errantmc.markets.npcinteractions.trading.VillagerBalance;
import net.minecraft.world.entity.npc.Villager;

import java.util.Optional;

/**
 * Settles a villager's days lazily (economy §7): nothing ticks. When a villager is next traded
 * with, the days since its last settlement, capped, are settled at once by
 * {@link DailySettlement}: the treasury's income, the wage, upkeep and restocking.
 */
public final class VillageEconomy {
    /** Never settled: its first settlement only starts the clock. */
    public static final long UNSET = Long.MIN_VALUE;

    private VillageEconomy() {}

    public static void settle(Villager villager) {
        long today = villager.level().getDayTime() / 24000L;
        long last = villager.getData(TradeAttachments.SETTLED_DAY);
        // A villager's first day, and time set backwards, settle nothing: the clock just starts over
        if (last == UNSET || today < last) {
            villager.setData(TradeAttachments.SETTLED_DAY, today);
            return;
        }
        if (today == last) return; // restocking is paid with the day's settlement, not as it is owed

        long owed = villager.getData(TradeAttachments.RESTOCK_OWED);
        long days = Math.min(today - last, Config.MAX_SETTLEMENT_DAYS.get());
        Optional<Account> treasury = Tills.treasury(villager);
        Membership membership = treasury.isEmpty() ? Membership.NONE
                : Tills.tradesFromTreasury(villager) ? Membership.BANKER : Membership.VILLAGER;
        long till = VillagerBalance.get(villager);
        DailySettlement s = DailySettlement.settle(till, treasury.map(Account::balance).orElse(0L), membership,
                rates(villager), days, owed);

        VillagerBalance.set(villager, s.till());
        treasury.ifPresent(t -> t.set(s.treasury()));
        if (villager.getServer() != null) {
            MoneyLedger.created(villager.getServer(), s.created());
            MoneyLedger.destroyed(villager.getServer(), s.destroyed());
        }
        villager.setData(TradeAttachments.RESTOCK_OWED, 0L); // what the till could not cover is forgiven
        villager.setData(TradeAttachments.SETTLED_DAY, today);
    }

    /** Records what restocking a sale will cost: a share of what the goods (less any money in them) sold for. */
    public static void oweRestock(Villager villager, long goodsValue) {
        long cost = DailySettlement.percent(Math.max(0, goodsValue), Config.RESTOCK_COST_PERCENT.get());
        if (cost > 0) villager.setData(TradeAttachments.RESTOCK_OWED, villager.getData(TradeAttachments.RESTOCK_OWED) + cost);
    }

    static DailySettlement.Rates rates(Villager villager) {
        int levelsAboveNovice = Math.max(0, villager.getVillagerData().getLevel() - 1);
        return new DailySettlement.Rates(
                Config.TREASURY_INCOME_PER_VILLAGER.get(),
                Config.TREASURY_CEILING.get(),
                Config.WAGE.get() + levelsAboveNovice * Config.WAGE_PER_LEVEL.get(),
                Config.UPKEEP_PERCENT.get(),
                Config.UPKEEP_FLOOR.get());
    }
}
