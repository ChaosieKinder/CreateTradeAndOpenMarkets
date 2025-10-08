package net.errantchaosie.tradeandopenmarkets.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

public record CurrencyInfo(int coinsCopper, int coinsBronze, int coinsSilver, int coinsElectrum, int coinsEmerald, Optional<UUID> owner) {


    public static final Codec<CurrencyInfo> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("copper").forGetter(CurrencyInfo::coinsCopper),
            Codec.INT.fieldOf("bronze").forGetter(CurrencyInfo::coinsBronze),
            Codec.INT.fieldOf("silver").forGetter(CurrencyInfo::coinsSilver),
            Codec.INT.fieldOf("electrum").forGetter(CurrencyInfo::coinsElectrum),
            Codec.INT.fieldOf("emerald").forGetter(CurrencyInfo::coinsEmerald),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(CurrencyInfo::owner)
    ).apply(inst, CurrencyInfo::new));

    // ---------- Network (packets) ----------
    public static final StreamCodec<ByteBuf, CurrencyInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CurrencyInfo::coinsCopper,
            ByteBufCodecs.VAR_INT, CurrencyInfo::coinsBronze,
            ByteBufCodecs.VAR_INT, CurrencyInfo::coinsSilver,
            ByteBufCodecs.VAR_INT, CurrencyInfo::coinsElectrum,
            ByteBufCodecs.VAR_INT, CurrencyInfo::coinsEmerald,
            // If your version lacks UUIDUtil.STREAM_CODEC, use the fallback shown below.
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), CurrencyInfo::owner,
            CurrencyInfo::new
    );

    public static final CurrencyInfo EMPTY = new CurrencyInfo(0, 0,0,0,0, Optional.empty());

    // ---- convenience helpers (optional) ----
    public CurrencyInfo withOwner(UUID id) { return new CurrencyInfo(copper(), bronze(), silver(), electrum(), emerald(), Optional.ofNullable(id)); }
    public CurrencyInfo withoutOwner()     { return new CurrencyInfo(copper(), bronze(), silver(), electrum(), emerald(), Optional.empty()); }

    public CurrencyInfo add(int dc, int db, int ds, int de, int dem) {
        return new CurrencyInfo(
                copper() + dc, bronze() + db, silver() + ds,
                electrum() + de, emerald() + dem, owner());
    }

    public boolean isEmpty() {
        return copper() == 0 && bronze() == 0 && silver() == 0 && electrum() == 0 && emerald() == 0;
    }

    // shorthand getters matching JSON keys
    public int copper()   { return coinsCopper; }
    public int bronze()   { return coinsBronze; }
    public int silver()   { return coinsSilver; }
    public int electrum() { return coinsElectrum; }
    public int emerald()  { return coinsEmerald; }

}
