package com.errantmc.markets.economy;

import com.errantmc.markets.ErrantTrade;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * How much money the economy has created and destroyed, since the world began and today
 * (economy §8.4). Every other movement is a transfer, so these two explain every change in the
 * money supply: starting balances and the treasuries' income create it, restocking destroys it.
 */
public final class MoneyLedger extends SavedData {
    private static final String NAME = ErrantTrade.MOD_ID + "_money_ledger";

    private long created, destroyed;
    private long day = Long.MIN_VALUE, createdToday, destroyedToday;

    private MoneyLedger() {}

    public static MoneyLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MoneyLedger::new, MoneyLedger::load, null), NAME);
    }

    public static void created(MinecraftServer server, long copperBits) {
        if (copperBits > 0) get(server).record(server, copperBits, 0);
    }

    public static void destroyed(MinecraftServer server, long copperBits) {
        if (copperBits > 0) get(server).record(server, 0, copperBits);
    }

    private void record(MinecraftServer server, long created, long destroyed) {
        rollOver(server);
        this.created += created;
        this.destroyed += destroyed;
        this.createdToday += created;
        this.destroyedToday += destroyed;
        setDirty();
    }

    /** Starts a new day's counts once the day has turned. */
    private void rollOver(MinecraftServer server) {
        long today = server.overworld().getDayTime() / 24000L;
        if (today != this.day) {
            this.day = today;
            this.createdToday = 0;
            this.destroyedToday = 0;
            setDirty();
        }
    }

    public long created() { return this.created; }
    public long destroyed() { return this.destroyed; }

    public long createdToday(MinecraftServer server) {
        rollOver(server);
        return this.createdToday;
    }

    public long destroyedToday(MinecraftServer server) {
        rollOver(server);
        return this.destroyedToday;
    }

    private static MoneyLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        MoneyLedger ledger = new MoneyLedger();
        ledger.created = tag.getLong("created");
        ledger.destroyed = tag.getLong("destroyed");
        ledger.day = tag.contains("day") ? tag.getLong("day") : Long.MIN_VALUE;
        ledger.createdToday = tag.getLong("created_today");
        ledger.destroyedToday = tag.getLong("destroyed_today");
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("created", this.created);
        tag.putLong("destroyed", this.destroyed);
        tag.putLong("day", this.day);
        tag.putLong("created_today", this.createdToday);
        tag.putLong("destroyed_today", this.destroyedToday);
        return tag;
    }
}
