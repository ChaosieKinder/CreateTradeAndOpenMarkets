package com.errantmc.markets.economy;

import com.errantmc.markets.Config;
import com.errantmc.markets.ErrantTrade;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every village's treasury, by its meeting point (docs/design/economy.md §7). World state, saved
 * with the overworld whichever dimension the village is in. A treasury is created, with the
 * configured starting balance, the first time anything asks for it.
 */
public final class Treasuries extends SavedData {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String NAME = ErrantTrade.MOD_ID + "_treasuries";

    private record Entry(GlobalPos village, long balance) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("village").forGetter(Entry::village),
                Codec.LONG.fieldOf("balance").forGetter(Entry::balance)
        ).apply(i, Entry::new));
    }

    private static final Codec<List<Entry>> ENTRIES = Entry.CODEC.listOf();

    private final Map<GlobalPos, Long> balances = new HashMap<>();

    private Treasuries() {}

    public static Treasuries get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Treasuries::new, Treasuries::load, null), NAME);
    }

    public long balance(GlobalPos village) {
        Long balance = this.balances.get(village);
        if (balance == null) {
            balance = Config.TREASURY_STARTING_BALANCE.get();
            this.balances.put(village, balance);
            setDirty();
        }
        return balance;
    }

    public void set(GlobalPos village, long copperBits) {
        if (copperBits < 0) throw new IllegalArgumentException("negative treasury " + copperBits);
        this.balances.put(village, copperBits);
        setDirty();
    }

    public Account account(GlobalPos village) {
        return new Account() {
            @Override public long balance() { return Treasuries.this.balance(village); }
            @Override public void set(long copperBits) { Treasuries.this.set(village, copperBits); }
        };
    }

    /** Every treasury so far, for measuring the money supply. */
    public Map<GlobalPos, Long> all() {
        return Collections.unmodifiableMap(this.balances);
    }

    private static Treasuries load(CompoundTag tag, HolderLookup.Provider registries) {
        Treasuries treasuries = new Treasuries();
        Tag list = tag.get("treasuries");
        if (list != null) {
            ENTRIES.parse(NbtOps.INSTANCE, list)
                    .resultOrPartial(err -> LOG.error("Village treasuries: {}", err))
                    .ifPresent(entries -> entries.forEach(e -> treasuries.balances.put(e.village(), e.balance())));
        }
        return treasuries;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        List<Entry> list = this.balances.entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList();
        ENTRIES.encodeStart(NbtOps.INSTANCE, list).ifSuccess(t -> tag.put("treasuries", t));
        return tag;
    }
}
