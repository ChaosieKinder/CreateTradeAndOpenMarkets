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

public record CurrencyData(int coinsCopperBit, int coinsCopperShard,
                           int coinsSilverBit, int coinsSilverCrown,
                           int coinsGoldMark, int coinsGoldRegal,
                           int coinsEmeraldSun,int coinsEmeraldThrone) {

    public static final Logger LOG = LogUtils.getLogger();
    public static final Codec<CurrencyData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("coinsCopperBit").forGetter(CurrencyData::coinsCopperBit),
            Codec.INT.fieldOf("coinsCopperShard").forGetter(CurrencyData::coinsCopperShard),
            Codec.INT.fieldOf("coinsSilverBit").forGetter(CurrencyData::coinsSilverBit),
            Codec.INT.fieldOf("coinsSilverCrown").forGetter(CurrencyData::coinsSilverCrown),
            Codec.INT.fieldOf("coinsGoldMark").forGetter(CurrencyData::coinsGoldMark),
            Codec.INT.fieldOf("coinsGoldRegal").forGetter(CurrencyData::coinsGoldRegal),
            Codec.INT.fieldOf("coinsEmeraldSun").forGetter(CurrencyData::coinsEmeraldSun),
            Codec.INT.fieldOf("coinsEmeraldThrone").forGetter(CurrencyData::coinsEmeraldThrone)
    ).apply(inst, CurrencyData::new));

    // ---------- Network (packets) ----------
    public static final StreamCodec<RegistryFriendlyByteBuf, CurrencyData> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CurrencyData decode(RegistryFriendlyByteBuf buf) {
                    int c1 = buf.readVarInt();
                    int c2 = buf.readVarInt();
                    int s1 = buf.readVarInt();
                    int s2 = buf.readVarInt();
                    int g1 = buf.readVarInt();
                    int g2 = buf.readVarInt();
                    int e1 = buf.readVarInt();
                    int e2 = buf.readVarInt();
                    return new CurrencyData(c1, c2, s1, s2, g1, g2, e1, e2);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, CurrencyData v) {
                    buf.writeVarInt(v.coinsCopperBit());
                    buf.writeVarInt(v.coinsCopperShard());
                    buf.writeVarInt(v.coinsSilverBit());
                    buf.writeVarInt(v.coinsSilverCrown());
                    buf.writeVarInt(v.coinsGoldMark());
                    buf.writeVarInt(v.coinsGoldRegal());
                    buf.writeVarInt(v.coinsEmeraldSun());
                    buf.writeVarInt(v.coinsEmeraldThrone());
                }
            };
    public static final CurrencyData EMPTY = new CurrencyData(0, 0,0,0,0, 0, 0, 0);

    public boolean isEmpty() {
        return coinsCopperBit == 0 && coinsCopperShard == 0 &&
                coinsSilverBit == 0 && coinsSilverCrown == 0 &&
                coinsGoldMark == 0  && coinsGoldRegal == 0  &&
                coinsEmeraldSun == 0  && coinsEmeraldThrone == 0;
    }

    // ----- Withers -----
    public CurrencyData withCopperBit(int v)     { return new CurrencyData(v, coinsCopperShard, coinsSilverBit, coinsSilverCrown, coinsGoldMark, coinsGoldRegal, coinsEmeraldSun, coinsEmeraldThrone); }
    public CurrencyData withCopperShard(int v)   { return new CurrencyData(coinsCopperBit, v, coinsSilverBit, coinsSilverCrown, coinsGoldMark, coinsGoldRegal, coinsEmeraldSun, coinsEmeraldThrone); }
    public CurrencyData withSilverBit(int v)     { return new CurrencyData(coinsCopperBit, coinsCopperShard, v, coinsSilverCrown, coinsGoldMark, coinsGoldRegal, coinsEmeraldSun, coinsEmeraldThrone); }
    public CurrencyData withSilverCrown(int v)   { return new CurrencyData(coinsCopperBit, coinsCopperShard, coinsSilverBit, v, coinsGoldMark, coinsGoldRegal, coinsEmeraldSun, coinsEmeraldThrone); }
    public CurrencyData withGoldMark(int v)      { return new CurrencyData(coinsCopperBit, coinsCopperShard, coinsSilverBit, coinsSilverCrown, v, coinsGoldRegal, coinsEmeraldSun, coinsEmeraldThrone); }
    public CurrencyData withGoldRegal(int v)     { return new CurrencyData(coinsCopperBit, coinsCopperShard, coinsSilverBit, coinsSilverCrown, coinsGoldMark, v, coinsEmeraldSun, coinsEmeraldThrone); }
    public CurrencyData withEmeraldSun(int v)    { return new CurrencyData(coinsCopperBit, coinsCopperShard, coinsSilverBit, coinsSilverCrown, coinsGoldMark, coinsGoldRegal, v, coinsEmeraldThrone); }
    public CurrencyData withEmeraldThrone(int v) { return new CurrencyData(coinsCopperBit, coinsCopperShard, coinsSilverBit, coinsSilverCrown, coinsGoldMark, coinsGoldRegal, coinsEmeraldSun, v); }


    // ----- Enum-based access / ops -----
    public int get(CoinDenomination d) {
        return switch (d) {
            case COPPER_BIT     -> coinsCopperBit;
            case COPPER_SHARD   -> coinsCopperShard;
            case SILVER_BIT     -> coinsSilverBit;
            case SILVER_CROWN   -> coinsSilverCrown;
            case GOLD_MARK      -> coinsGoldMark;
            case GOLD_REGAL     -> coinsGoldRegal;
            case EMERALD_SUN    -> coinsEmeraldSun;
            case EMERALD_THRONE -> coinsEmeraldThrone;
            case NONE           -> 0;
        };
    }
    public CurrencyData set(CoinDenomination d, int v) {
        return switch (d) {
            case COPPER_BIT     -> withCopperBit(v);
            case COPPER_SHARD   -> withCopperShard(v);
            case SILVER_BIT     -> withSilverBit(v);
            case SILVER_CROWN   -> withSilverCrown(v);
            case GOLD_MARK      -> withGoldMark(v);
            case GOLD_REGAL     -> withGoldRegal(v);
            case EMERALD_SUN    -> withEmeraldSun(v);
            case EMERALD_THRONE -> withEmeraldThrone(v);
            case NONE           -> this;
        };
    }
    public CurrencyData add(int c1, int c2, int s1, int s2, int g1, int g2, int e1, int e2) {
        return new CurrencyData(
            coinsCopperBit + c1,
            coinsCopperShard + c2,
            coinsSilverBit + s1,
            coinsSilverCrown + s2,
            coinsGoldMark + g1,
                + coinsGoldRegal + g2,
                coinsEmeraldSun + e1,
                coinsEmeraldThrone + e2
        );
    }

    /**
     * Returns the highest denomination coin in the stack.
     * @return returns the coin denomination in the stack with the highest value
     */
    public CoinDenomination getHighestDenomonation() {
        if(coinsEmeraldThrone > 0)
            return CoinDenomination.EMERALD_THRONE;
        if(coinsEmeraldSun > 0)
            return CoinDenomination.EMERALD_SUN;
        if(coinsGoldRegal > 0)
            return CoinDenomination.GOLD_REGAL;
        if(coinsGoldMark > 0)
            return CoinDenomination.GOLD_MARK;
        if(coinsSilverCrown > 0)
            return CoinDenomination.SILVER_CROWN;
        if(coinsSilverBit > 0)
            return CoinDenomination.SILVER_BIT;
        if(coinsCopperShard > 0)
            return CoinDenomination.COPPER_SHARD;
        if(coinsCopperBit > 0)
            return CoinDenomination.COPPER_BIT;
        return CoinDenomination.NONE;
    }

    /**
     * Adds the item from the item stack to the currency data if possible
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

    public float totalValue() {
        return (coinsCopperBit * CoinDenomination.COPPER_BIT.value +
            coinsCopperShard * CoinDenomination.COPPER_SHARD.value +
            coinsSilverBit * CoinDenomination.SILVER_BIT.value +
            coinsSilverCrown * CoinDenomination.SILVER_CROWN.value +
            coinsGoldMark * CoinDenomination.GOLD_MARK.value +
            coinsGoldRegal * CoinDenomination.GOLD_REGAL.value +
            coinsEmeraldSun * CoinDenomination.EMERALD_SUN.value+
            coinsEmeraldThrone * CoinDenomination.EMERALD_THRONE.value)/100.0f;
    }

    public int totalCount() {
        return coinsCopperBit + coinsCopperShard +
                coinsSilverBit + coinsSilverCrown +
                coinsGoldMark + coinsGoldRegal +
                coinsEmeraldSun+ coinsEmeraldThrone;
    }

    @Override
    public @NotNull String toString() {
        return "CurrencyData{" +
                "coinsCopperBit=" + coinsCopperBit +
                ", coinsCopperShard=" + coinsCopperShard +
                ", coinsSilverBit=" + coinsSilverBit +
                ", coinsSilverCrown=" + coinsSilverCrown +
                ", coinsGoldMark=" + coinsGoldMark +
                ", coinsGoldRegal=" + coinsGoldRegal +
                ", coinsEmeraldSun=" + coinsEmeraldSun +
                ", coinsEmeraldThrone=" + coinsEmeraldThrone +
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
    public record CoinsChangeResult(CurrencyData updated, CoinDenomination denomination, int delta, int newValue, int remaining) {
        @Override
        public String toString() {
            return "CoinsChangeResult{" +
                    "updated=" + updated +
                    ", denomination=" + denomination +
                    ", delta=" + delta +
                    ", newValue=" + newValue +
                    ", remaining=" + remaining +
                    '}';
        }
    }
}
