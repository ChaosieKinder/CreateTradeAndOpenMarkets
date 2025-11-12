package net.errantchaosie.tradeandopenmarkets.shop.tab;

import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Provides contextual information for shop tab generation.
 */
public record ShopTabGenerationContext(
        @Nullable Merchant merchant,
        MerchantOffers offers,
        Level level,
        long dayIndex,
        RandomSource random
) {
}
