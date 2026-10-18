package com.errantmc.markets.component;

import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.Purse;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The Minecraft side of {@link Purse}: its codecs, and reading and writing it on an item stack
 * (a wallet, a coin stack).
 */
public final class Purses {
    private Purses() {}

    /**
     * The save format, on items and on coin piles. The field names are what the old
     * {@code CurrencyData} record wrote, and must stay so existing saves still load (AGENTS §5).
     */
    public static final Codec<Purse> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            field("coinsCopperBit", CoinDenomination.COPPER_BIT),
            field("coinsCopperShard", CoinDenomination.COPPER_SHARD),
            field("coinsSilverBit", CoinDenomination.SILVER_BIT),
            field("coinsSilverCrown", CoinDenomination.SILVER_CROWN),
            field("coinsGoldMark", CoinDenomination.GOLD_MARK),
            field("coinsGoldRegal", CoinDenomination.GOLD_REGAL),
            field("coinsEmeraldSun", CoinDenomination.EMERALD_SUN),
            field("coinsEmeraldThrone", CoinDenomination.EMERALD_THRONE)
    ).apply(inst, (c1, c2, s1, s2, g1, g2, e1, e2) -> Purse.EMPTY
            .with(CoinDenomination.COPPER_BIT, c1).with(CoinDenomination.COPPER_SHARD, c2)
            .with(CoinDenomination.SILVER_BIT, s1).with(CoinDenomination.SILVER_CROWN, s2)
            .with(CoinDenomination.GOLD_MARK, g1).with(CoinDenomination.GOLD_REGAL, g2)
            .with(CoinDenomination.EMERALD_SUN, e1).with(CoinDenomination.EMERALD_THRONE, e2)));

    /** Read as any int, as the old record did; a negative count (never written) loads as 0 rather than failing the whole purse. */
    private static RecordCodecBuilder<Purse, Integer> field(String name, CoinDenomination denomination) {
        return Codec.INT.xmap(c -> Math.max(0, c), c -> c).fieldOf(name).forGetter(p -> p.count(denomination));
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, Purse> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public Purse decode(RegistryFriendlyByteBuf buf) {
            Purse purse = Purse.EMPTY;
            for (CoinDenomination d : CoinDenomination.coins()) purse = purse.with(d, buf.readVarInt());
            return purse;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, Purse purse) {
            for (CoinDenomination d : CoinDenomination.coins()) buf.writeVarInt(purse.count(d));
        }
    };

    /** The stack's purse, or an empty one; never null. */
    public static Purse get(ItemStack stack) {
        var type = typeOrNull();
        if (type == null) return Purse.EMPTY;
        Purse purse = stack.get(type);
        return purse != null ? purse : Purse.EMPTY;
    }

    public static void set(ItemStack stack, Purse purse) {
        var type = typeOrNull();
        if (type != null) stack.set(type, purse);
    }

    /** Adds what fits to the stack's purse; the change says what went in and what did not. */
    public static Purse.Change add(ItemStack stack, CoinDenomination denomination, int count) {
        var change = get(stack).add(denomination, count);
        set(stack, change.updated());
        return change;
    }

    public static Purse.Change remove(ItemStack stack, CoinDenomination denomination, int count) {
        var change = get(stack).remove(denomination, count);
        set(stack, change.updated());
        return change;
    }

    /** Null while the component is unbound, during very early tooltip queries. */
    private static @Nullable DataComponentType<Purse> typeOrNull() {
        return TradeComponents.PURSE.isBound() ? TradeComponents.PURSE.get() : null;
    }
}
