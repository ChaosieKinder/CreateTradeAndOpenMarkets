package net.errantchaosie.tradeandopenmarkets.component;

import net.errantchaosie.tradeandopenmarkets.data.CoinDenomination;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public final class WalletBalance {
    private WalletBalance() {}


    private static @Nullable DataComponentType<CurrencyData> typeOrNull() {
        // Avoid NPE from unbound DeferredHolder during very-early tooltip queries
        return TAOMComponents.CURRENCY_DATA.isBound() ? TAOMComponents.CURRENCY_DATA.get() : null;
    }

    /** Ensure the component exists on the stack. */
    public static void ensure(ItemStack wallet) {
        var type = typeOrNull();
        if (type == null) return; // too early; skip writes during init
        if (wallet.get(type) == null) {
            wallet.set(type, CurrencyData.EMPTY);
        }
    }

    public static CurrencyData get(ItemStack wallet) {
        var type = typeOrNull();
        if (type == null) return CurrencyData.EMPTY;
        CurrencyData c = wallet.get(type);
        return c != null ? c : CurrencyData.EMPTY;
    }

    public static void set(ItemStack wallet, CurrencyData c) {
        var type = typeOrNull();
        if (type == null) return; // too early; skip
        wallet.set(type, c);
    }

    // add/remove can stay the same, but call typeOrNull() via get()/set()/ensure()
    public static void add(ItemStack wallet, CoinDenomination d, int count) {
        ensure(wallet);
        set(wallet, get(wallet).add(d, count).updated());
    }

    public static CurrencyData.CoinsChangeResult remove(ItemStack wallet, CoinDenomination d, int count) {
        ensure(wallet);
        var result = get(wallet).remove(d, count);
        set(wallet, result.updated());
        return result;
    }
}
