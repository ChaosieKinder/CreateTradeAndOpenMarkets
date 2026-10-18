package com.errantmc.markets.economy;

import com.errantmc.markets.item.PlayerMoney;
import com.errantmc.markets.npcinteractions.trading.TradeAttachments;
import com.errantmc.markets.npcinteractions.trading.VillagerBalance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * The money that can be counted right now: what online players carry (loose coins and their
 * wallet), the tills of loaded villagers, and every treasury. Money in chests, coin piles, offline
 * players and unloaded villagers is not counted, so the total is a floor, and its change is
 * explained by the {@link MoneyLedger} only while the same things stay loaded.
 */
public record MoneySupply(int players, long loose, long wallets, int villagers, long tills, int treasuries, long treasury) {

    public long total() {
        return this.loose + this.wallets + this.tills + this.treasury;
    }

    public static MoneySupply measure(MinecraftServer server) {
        int players = 0;
        long loose = 0, wallets = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            players++;
            loose += PlayerMoney.loose(player).totalValue();
            wallets += PlayerMoney.wallet(player).totalValue();
        }
        int villagers = 0;
        long tills = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Villager villager : level.getEntities(EntityTypeTest.forClass(Villager.class), v -> true)) {
                villagers++;
                long balance = villager.getData(TradeAttachments.BALANCE); // read, so counting gives no one a starting balance
                if (balance != VillagerBalance.UNSET) tills += balance;
            }
        }
        var all = Treasuries.get(server).all();
        long treasury = all.values().stream().mapToLong(Long::longValue).sum();
        return new MoneySupply(players, loose, wallets, villagers, tills, all.size(), treasury);
    }
}
