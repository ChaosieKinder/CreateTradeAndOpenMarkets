package net.errantchaosie.tradeandopenmarkets.block.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyInfo;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


public class CoinBlockEntity extends BlockEntity {

    private CurrencyInfo currency = CurrencyInfo.EMPTY;

    public CoinBlockEntity(BlockPos pos, BlockState state) {
        super(TAOMBlockEntities.COIN_BLOCKENTITY.get(), pos, state);
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
    public CurrencyInfo getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyInfo info) {
        if (info == null) info = CurrencyInfo.EMPTY;
        this.currency = info;
        markDirtyAndSync();
    }

    public void addCoins(int copper, int bronze, int silver, int electrum, int emerald) {
        CurrencyInfo c = this.currency;
        this.currency = new CurrencyInfo(
                (c.coinsCopper() + copper),
                (c.coinsBronze() + bronze),
                (c.coinsSilver() + silver),
                (c.coinsElectrum() + electrum),
                (c.coinsEmerald() + emerald),
                c.owner()
        );
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
        // Encode CurrencyInfo → Tag via NbtOps
        CurrencyInfo.CODEC.encodeStart(NbtOps.INSTANCE, currency)
                .resultOrPartial(err -> /* your logger here */ System.err.println("Currency save failed: " + err))
                .ifPresent(encoded -> tag.put(KEY_CURRENCY, encoded));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(KEY_CURRENCY, Tag.TAG_COMPOUND)) {
            Tag data = tag.get(KEY_CURRENCY);
            CurrencyInfo.CODEC.parse(NbtOps.INSTANCE, data)
                    .resultOrPartial(err -> /* your logger here */ System.err.println("Currency load failed: " + err))
                    .ifPresentOrElse(
                            v -> this.currency = v,
                            () -> this.currency = CurrencyInfo.EMPTY
                    );
        } else {
            this.currency = CurrencyInfo.EMPTY;
        }
    }

}