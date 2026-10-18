package com.errantmc.markets.npcinteractions.trading;

import com.errantmc.markets.Config;
import com.errantmc.markets.domain.Payment;
import com.errantmc.markets.domain.TradeXp;
import com.errantmc.markets.economy.Account;
import com.errantmc.markets.economy.Tills;
import com.errantmc.markets.item.PlayerMoney;
import com.errantmc.markets.npcinteractions.tabs.buy.BuyShopTab;
import com.errantmc.markets.npcinteractions.tabs.sell.SellShopTab;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Buying from and selling to a villager's shop, on the server. Every check comes before anything
 * moves, so a refused trade changes nothing. Money moves between the player, the villager's till
 * ({@link Tills}) and its village's treasury; a trade neither creates nor destroys it.
 */
public final class ShopTrades {
    private static final Logger LOG = LogUtils.getLogger();

    private ShopTrades() {}

    public enum Result {
        DONE, NO_SUCH_ENTRY, OUT_OF_STOCK, MISSING_ITEMS, CANNOT_AFFORD, VILLAGER_CANNOT_AFFORD;

        /** What the player is told when the trade is refused. */
        public String messageKey() {
            return "message.erranttrade.shop." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static Result buy(ServerPlayer player, Villager villager, BuyShopTab tab, int index) {
        if (index < 0 || index >= tab.entries().size()) return Result.NO_SUCH_ENTRY;
        ShopEntry entry = tab.entries().get(index);
        @Nullable MerchantOffer offer = vanillaOffer(villager, entry);
        if (!entry.inStock() || (offer != null && offer.isOutOfStock())) return Result.OUT_OF_STOCK;
        Predicate<ItemStack> extra = matcher(entry.extraCost(), offer);
        if (!has(player, extra, entry.extraCost().getCount())) return Result.MISSING_ITEMS;
        var planned = Payment.plan(PlayerMoney.loose(player), PlayerMoney.wallet(player), entry.price());
        if (planned.isEmpty()) return Result.CANNOT_AFFORD;
        // The till gains the price, less any money in the goods (the banker sells coins), less the fee
        Ledger ledger = Ledger.of(villager, entry.price() - PlayerMoney.worth(entry.item()), entry.price());
        if (!ledger.affordable()) return Result.VILLAGER_CANNOT_AFFORD;

        Payment payment = planned.get();
        PlayerMoney.takeLoose(player, payment.fromLoose());
        PlayerMoney.takeFromWallet(player, payment.fromWallet());
        PlayerMoney.give(player, payment.change());
        take(player, extra, entry.extraCost().getCount());
        PlayerMoney.giveItem(player, entry.item().copy());
        ledger.post();
        entry.decreaseStock(1);
        if (offer != null) offer.increaseUses();
        progress(player, villager, entry.price(), entry.item());
        return Result.DONE;
    }

    public static Result sell(ServerPlayer player, Villager villager, SellShopTab tab, int index) {
        if (index < 0 || index >= tab.entries().size()) return Result.NO_SUCH_ENTRY;
        ShopEntry entry = tab.entries().get(index);
        @Nullable MerchantOffer offer = vanillaOffer(villager, entry);
        if (!entry.inStock() || (offer != null && offer.isOutOfStock())) return Result.OUT_OF_STOCK;
        Predicate<ItemStack> wanted = matcher(entry.item(), offer);
        Predicate<ItemStack> extra = matcher(entry.extraCost(), offer);
        if (!has(player, wanted, entry.item().getCount()) || !has(player, extra, entry.extraCost().getCount())) {
            return Result.MISSING_ITEMS;
        }
        // The villager pays from its till, and the fee; it buys only while it can afford to (economy §3)
        Ledger ledger = Ledger.of(villager, PlayerMoney.worth(entry.item()) - entry.price(), entry.price());
        if (!ledger.affordable()) return Result.VILLAGER_CANNOT_AFFORD;

        take(player, wanted, entry.item().getCount());
        take(player, extra, entry.extraCost().getCount());
        PlayerMoney.give(player, Payment.change(entry.price()));
        ledger.post();
        entry.decreaseStock(1);
        tab.recordBuyback(entry.item(), 1);
        if (offer != null) offer.increaseUses();
        progress(player, villager, entry.price(), entry.item());
        return Result.DONE;
    }

    /**
     * A trade's effect on the villager's money: {@code change} to its till, then the fee from the
     * till to the village treasury. Checked before anything moves, posted after.
     */
    private record Ledger(Account till, Optional<Account> treasury, long change, long fee) {
        static Ledger of(Villager villager, long change, long price) {
            long fee = Tills.fee(villager, price);
            return new Ledger(Tills.till(villager), fee > 0 ? Tills.treasury(villager) : Optional.empty(), change, fee);
        }

        boolean affordable() {
            return this.till.balance() + this.change - this.fee >= 0;
        }

        void post() {
            this.till.set(this.till.balance() + this.change - this.fee);
            this.treasury.ifPresent(t -> t.set(t.balance() + this.fee));
        }
    }

    /** The live vanilla offer an entry was converted from, if it still exists. */
    @Nullable
    private static MerchantOffer vanillaOffer(Villager villager, ShopEntry entry) {
        if (!entry.fromVanillaOffer()) return null;
        var offers = villager.getOffers();
        return entry.offerIndex() < offers.size() ? offers.get(entry.offerIndex()) : null;
    }

    /**
     * Which stacks count as {@code wanted}. For a converted vanilla offer, the offer's own cost
     * decides (it may require particular components); otherwise the same item and components.
     */
    private static Predicate<ItemStack> matcher(ItemStack wanted, @Nullable MerchantOffer offer) {
        if (wanted.isEmpty()) return s -> false;
        if (offer != null) {
            for (ItemCost cost : costs(offer)) {
                if (cost.item().value() == wanted.getItem()) return cost::test;
            }
        }
        return s -> ItemStack.isSameItemSameComponents(wanted, s);
    }

    private static List<ItemCost> costs(MerchantOffer offer) {
        List<ItemCost> out = new ArrayList<>(2);
        out.add(offer.getItemCostA());
        offer.getItemCostB().ifPresent(out::add);
        return out;
    }

    private static boolean has(ServerPlayer player, Predicate<ItemStack> matches, int count) {
        if (count <= 0) return true;
        int found = 0;
        for (ItemStack s : carried(player)) if (matches.test(s)) found += s.getCount();
        return found >= count;
    }

    private static void take(ServerPlayer player, Predicate<ItemStack> matches, int count) {
        for (ItemStack s : carried(player)) {
            if (count <= 0) return;
            if (matches.test(s)) {
                int n = Math.min(count, s.getCount());
                s.shrink(n);
                count -= n;
            }
        }
    }

    private static List<ItemStack> carried(ServerPlayer player) {
        List<ItemStack> out = new ArrayList<>(player.getInventory().items);
        out.addAll(player.getInventory().offhand);
        return out;
    }

    /**
     * Villager progression, scaled by the trade's worth (TradeXp; author's decision, 18 Oct). The
     * trade is reported to the villager as vanilla's are, so it gains experience, levels up when
     * the screen closes, and remembers the player favourably. The player's experience is the same
     * value-scaled amount, not vanilla's fixed orbs per trade, which many tiny trades could farm.
     */
    private static void progress(ServerPlayer player, Villager villager, long value, ItemStack traded) {
        TradeXp award = TradeXp.award(value, villager.getData(TradeAttachments.XP_REMAINDER), Config.BITS_PER_VILLAGER_XP.get());
        villager.setData(TradeAttachments.XP_REMAINDER, award.remainder());
        villager.notifyTrade(reportingOffer(villager, traded, award.xp()));
        if (award.xp() > 0 && villager.level() instanceof ServerLevel level) {
            ExperienceOrb.award(level, villager.position().add(0, 0.5, 0), award.xp());
        }
    }

    /**
     * An offer that tells the villager what it earned. Built through the codec because no public
     * constructor can turn off {@code rewardExp}, vanilla's fixed orbs per trade.
     */
    private static MerchantOffer reportingOffer(Villager villager, ItemStack traded, int xp) {
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD), traded.copy(), 1, xp, 0f);
        var ops = villager.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag encoded = MerchantOffer.CODEC.encodeStart(ops, offer).getOrThrow();
        if (encoded instanceof CompoundTag tag) {
            tag.putBoolean("rewardExp", false);
            return MerchantOffer.CODEC.parse(ops, tag).resultOrPartial(err -> LOG.error("Reporting offer: {}", err)).orElse(offer);
        }
        return offer;
    }
}
