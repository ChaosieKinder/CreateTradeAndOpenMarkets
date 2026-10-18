package com.errantmc.markets.block.entity;

import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.Purse;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


public class CoinBlockEntity extends BlockEntity {
    private static final Logger LOG = LogUtils.getLogger();

    private Purse currency = Purse.EMPTY;

    public CoinBlockEntity(BlockPos pos, BlockState state) {
        super(TradeBlockEntities.COIN_PILE.get(), pos, state);
    }

    // ---------- Persistent save/load ----------
    private static final String KEY_CURRENCY = "Currency";

    // ---------- Networking sync (standard BE) ----------
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);        // include your fields
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        loadAdditional(tag, provider);        // read your fields
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this); // sends getUpdateTag() to client
    }


    // ---------- Mutators / helpers ----------
    public Purse getCurrency() {
        return currency;
    }

    public void setCurrency(Purse info) {
        if (info == null) info = Purse.EMPTY;
        this.currency = info;
        markDirtyAndSync();
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        Purses.CODEC.encodeStart(NbtOps.INSTANCE, currency)
                .resultOrPartial(err -> LOG.error("Coin pile at {} could not be saved: {}", worldPosition, err))
                .ifPresent(encoded -> tag.put(KEY_CURRENCY, encoded));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(KEY_CURRENCY, Tag.TAG_COMPOUND)) {
            Tag data = tag.get(KEY_CURRENCY);
            Purses.CODEC.parse(NbtOps.INSTANCE, data)
                    .resultOrPartial(err -> LOG.error("Coin pile at {} could not be loaded: {}", worldPosition, err))
                    .ifPresentOrElse(
                            v -> this.currency = v,
                            () -> this.currency = Purse.EMPTY
                    );
        } else {
            this.currency = Purse.EMPTY;
        }
    }

}