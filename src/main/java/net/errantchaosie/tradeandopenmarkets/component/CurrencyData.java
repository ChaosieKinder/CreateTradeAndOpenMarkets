package net.errantchaosie.tradeandopenmarkets.component;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.errantchaosie.tradeandopenmarkets.data.CoinDenomination;
import net.errantchaosie.tradeandopenmarkets.item.CoinItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public record CurrencyData(int coinsCopper, int coinsBronze, int coinsSilver, int coinsGold, int coinsElectrum, int coinsPlatinum, int coinsEmerald) {

    public static final Logger LOG = LogUtils.getLogger();
    public static final Codec<CurrencyData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("coinsCopper").forGetter(CurrencyData::coinsCopper),
            Codec.INT.fieldOf("coinsBronze").forGetter(CurrencyData::coinsBronze),
            Codec.INT.fieldOf("coinsSilver").forGetter(CurrencyData::coinsSilver),
            Codec.INT.fieldOf("coinsGold").forGetter(CurrencyData::coinsGold),
            Codec.INT.fieldOf("coinsElectrum").forGetter(CurrencyData::coinsElectrum),
            Codec.INT.fieldOf("coinsPlatinum").forGetter(CurrencyData::coinsElectrum),
            Codec.INT.fieldOf("coinsEmerald").forGetter(CurrencyData::coinsEmerald)
    ).apply(inst, CurrencyData::new));

    // ---------- Network (packets) ----------
    public static final StreamCodec<RegistryFriendlyByteBuf, CurrencyData> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CurrencyData decode(RegistryFriendlyByteBuf buf) {
                    int copper    = buf.readVarInt();
                    int bronze    = buf.readVarInt();
                    int silver    = buf.readVarInt();
                    int gold      = buf.readVarInt();
                    int electrum  = buf.readVarInt();
                    int platinum  = buf.readVarInt();
                    int emerald   = buf.readVarInt();
                    return new CurrencyData(copper, bronze, silver, gold, electrum, platinum, emerald);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, CurrencyData v) {
                    buf.writeVarInt(v.coinsCopper());
                    buf.writeVarInt(v.coinsBronze());
                    buf.writeVarInt(v.coinsSilver());
                    buf.writeVarInt(v.coinsGold());
                    buf.writeVarInt(v.coinsElectrum());
                    buf.writeVarInt(v.coinsPlatinum());
                    buf.writeVarInt(v.coinsEmerald());
                }
            };
    public static final CurrencyData EMPTY = new CurrencyData(0, 0,0,0,0, 0, 0);

    public boolean isEmpty() {
        return coinsCopper == 0 && coinsBronze == 0 && coinsSilver == 0 && coinsGold == 0 && coinsElectrum == 0  && coinsPlatinum == 0  && coinsEmerald == 0;
    }

    // ----- Withers -----
    public CurrencyData withCopper(int v)   { return new CurrencyData(v, coinsBronze, coinsSilver, coinsGold, coinsElectrum, coinsPlatinum, coinsEmerald); }
    public CurrencyData withBronze(int v)   { return new CurrencyData(coinsCopper, v, coinsSilver, coinsGold, coinsElectrum, coinsPlatinum, coinsEmerald); }
    public CurrencyData withSilver(int v)   { return new CurrencyData(coinsCopper, coinsBronze, v, coinsGold, coinsElectrum, coinsPlatinum, coinsEmerald); }
    public CurrencyData withGold(int v)     { return new CurrencyData(coinsCopper, coinsBronze, coinsSilver, v, coinsElectrum, coinsPlatinum, coinsEmerald); } // NEW
    public CurrencyData withElectrum(int v) { return new CurrencyData(coinsCopper, coinsBronze, coinsSilver, coinsGold, v, coinsPlatinum, coinsEmerald); }
    public CurrencyData withPlatinum(int v) { return new CurrencyData(coinsCopper, coinsBronze, coinsSilver, coinsGold, coinsElectrum, v, coinsEmerald); }     // NEW
    public CurrencyData withEmerald(int v)  { return new CurrencyData(coinsCopper, coinsBronze, coinsSilver, coinsGold, coinsElectrum, coinsPlatinum, v); }


    // ----- Enum-based access / ops -----
    public int get(CoinDenomination d) {
        return switch (d) {
            case COPPER   -> coinsCopper;
            case BRONZE   -> coinsBronze;
            case SILVER   -> coinsSilver;
            case GOLD     -> coinsGold;
            case ELECTRUM -> coinsElectrum;
            case PLATINUM -> coinsPlatinum;
            case EMERALD  -> coinsEmerald;
            case NONE     -> 0;
        };
    }
    public CurrencyData set(CoinDenomination d, int v) {
        return switch (d) {
            case COPPER   -> withCopper(v);
            case BRONZE   -> withBronze(v);
            case SILVER   -> withSilver(v);
            case GOLD     -> withGold(v);
            case ELECTRUM -> withElectrum(v);
            case PLATINUM -> withPlatinum(v);
            case EMERALD  -> withEmerald(v);
            case NONE     -> this;
        };
    }
    public CurrencyData add(int dc, int db, int ds, int dg, int de, int dp, int dem) {
        return new CurrencyData(
                coinsCopper + dc, coinsBronze + db,
                coinsSilver + ds, coinsGold + dg,
                coinsElectrum + de, coinsPlatinum + dp, coinsEmerald + dem);
    }

    /**
     * Returns the highest denomination coin in the stack.
     * @return returns the coin denomination in the stack with the highest value
     */
    public CoinDenomination getHighestDenomonation() {
        if(coinsEmerald > 0)
            return CoinDenomination.EMERALD;
        if(coinsPlatinum > 0)
            return CoinDenomination.PLATINUM;
        if(coinsElectrum > 0)
            return CoinDenomination.ELECTRUM;
        if(coinsGold > 0)
            return CoinDenomination.GOLD;
        if(coinsSilver > 0)
            return CoinDenomination.SILVER;
        if(coinsBronze > 0)
            return CoinDenomination.BRONZE;
        if(coinsCopper > 0)
            return CoinDenomination.COPPER;
        return CoinDenomination.NONE;
    }

    /**
     * Adds the items from the item stack to the currency data if possible
     * @implNote does not handle adjusting the item stack size
     * @param itemstack
     * @return Result object that describes the change
     */
    public CoinsChangeResult add(ItemStack itemstack) {
        var coinItem = itemstack.getItem();
        var stackSize = itemstack.getCount();
        if (itemstack.isEmpty() || !CoinItem.isCoinItem(coinItem))
            return new CoinsChangeResult(this, CoinDenomination.NONE,0 , 0, stackSize);
        var denomination = CoinItem.getDenomination(coinItem);
        return add(denomination, itemstack.getCount());
    }

    public CoinsChangeResult add(CoinDenomination denomination, int addCount) {
        if (denomination == CoinDenomination.NONE || addCount == 0)
            return new CoinsChangeResult(this, CoinDenomination.NONE,0 ,0, addCount);
        int currentCoinCount = get(denomination);
        int space   = Integer.MAX_VALUE - currentCoinCount;
        int toAdd = Math.min(addCount, space);
        int newValue = currentCoinCount + toAdd;
        return new CoinsChangeResult(set(denomination, newValue),denomination, toAdd, newValue,
                addCount - toAdd);
    }

    public CoinsChangeResult remove(CoinDenomination denomination, int removeCount) {
        if (denomination == CoinDenomination.NONE || removeCount <= 0)
            return new CoinsChangeResult(this, denomination,0 ,0, removeCount);
        int currentCoinCount = get(denomination);
        int removed = Math.min(removeCount, currentCoinCount);
        int newValue = currentCoinCount - removed;
        return new CoinsChangeResult(set(denomination, newValue),denomination, removed, newValue,
                removeCount - removed);
    }

    /** Build a CurrencyInfo with exactly `amount` in the given denomination. */
    public static CurrencyData newCurrencyFromDenom(CoinDenomination d, int amount) {
        return CurrencyData.EMPTY.add(d, amount).updated();
    }

    public int totalValue() {
        return coinsCopper * CoinDenomination.COPPER.value +
                coinsBronze * CoinDenomination.BRONZE.value +
                coinsSilver * CoinDenomination.SILVER.value +
                coinsGold * CoinDenomination.GOLD.value +
                coinsElectrum * CoinDenomination.ELECTRUM.value +
                coinsPlatinum * CoinDenomination.PLATINUM.value +
                coinsEmerald * CoinDenomination.EMERALD.value;
    }

    public int totalCount() {
        return coinsCopper + coinsBronze + coinsSilver + coinsGold + coinsElectrum + coinsPlatinum + coinsEmerald;
    }

    @NotNull
    @Override
    public String toString() {
        return "CurrencyData{" +
                "coinsCopper=" + coinsCopper +
                ", coinsBronze=" + coinsBronze +
                ", coinsSilver=" + coinsSilver +
                ", coinsGold=" + coinsGold +
                ", coinsElectrum=" + coinsElectrum +
                ", coinsPlatinum=" + coinsPlatinum +
                ", coinsEmerald=" + coinsEmerald +
                '}';
    }

    /**
     * Represents a change on a CurrencyData object.
     * This is frequently the result of messages updating a single denomination within the currency data.
     * @param updated The new CurrencyData after the change was applied
     * @param denomination The denomination that changed
     * @param delta The amount a value changed (can be negative)
     * @param newValue The amount of that value after the change was applied
     * @param remaining The amount that was not able to be added/removed from what was requested
     */
    public record CoinsChangeResult(CurrencyData updated, CoinDenomination denomination, int delta, int newValue, int remaining) {}
}
