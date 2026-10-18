package com.errantmc.markets.component;

import com.errantmc.markets.data.CoinDenomination;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public final class WalletBalance {
    private WalletBalance() {}


    private static @Nullable DataComponentType<CurrencyData> typeOrNull() {
        // Avoid NPE from unbound DeferredHolder during very-early tooltip queries
        return TradeComponents.CURRENCY_DATA.isBound() ? TradeComponents.CURRENCY_DATA.get() : null;
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

    /** Adds what fits; the result's {@code delta} is what went in and {@code remaining} is what did not. */
    public static CurrencyData.CoinsChangeResult add(ItemStack wallet, CoinDenomination d, int count) {
        ensure(wallet);
        var result = get(wallet).add(d, count);
        set(wallet, result.updated());
        return result;
    }

    public static CurrencyData.CoinsChangeResult remove(ItemStack wallet, CoinDenomination d, int count) {
        ensure(wallet);
        var result = get(wallet).remove(d, count);
        set(wallet, result.updated());
        return result;
    }
}
