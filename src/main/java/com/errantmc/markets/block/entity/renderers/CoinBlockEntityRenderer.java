package com.errantmc.markets.block.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.errantmc.markets.block.entity.CoinBlockEntity;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.PileLayout;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.item.CoinItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a coin pile as columns of coin items. Where the columns stand is {@link PileLayout}'s job;
 * this only draws them, with a little jitter per coin so a column does not look machined.
 */
public class CoinBlockEntityRenderer implements BlockEntityRenderer<CoinBlockEntity> {
    private static final float LAYER = 1.0f / 16f;                // one coin's thickness, before scaling
    private static final float COIN_SCALE_HORIZONTAL = PileLayout.COIN_SCALE;
    private static final float COIN_SCALE_VERTICAL = 0.20f;
    private static final float INNER_FLOOR = LAYER / 2;           // sit just inside the block
    private static final float JITTER = 0.015f;
    private static final int LAYOUT_CACHE_SIZE = 128;

    private final ItemRenderer itemRenderer;
    private final Map<CoinDenomination, ItemStack> coinStacks = new EnumMap<>(CoinDenomination.class);

    /** The layout depends only on the balance, so it is computed once per balance, not every frame. */
    private final Map<Purse, List<PileLayout.Column>> layouts = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Purse, List<PileLayout.Column>> eldest) {
            return size() > LAYOUT_CACHE_SIZE;
        }
    };

    public CoinBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public boolean shouldRenderOffScreen(CoinBlockEntity be) {
        return false; // piles are small
    }

    @Override
    public void render(CoinBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        List<PileLayout.Column> columns = layouts.computeIfAbsent(be.getCurrency(), PileLayout::of);
        if (columns.isEmpty()) return;

        final int seedBase = (int) be.getBlockPos().asLong();
        pose.pushPose();
        pose.rotateAround(com.mojang.math.Axis.XP.rotationDegrees(90f), 0, 0, 0);
        pose.scale(COIN_SCALE_HORIZONTAL, COIN_SCALE_HORIZONTAL, COIN_SCALE_VERTICAL);
        pose.translate(0.5f / COIN_SCALE_HORIZONTAL, 0.5f / COIN_SCALE_HORIZONTAL, -INNER_FLOOR);

        for (int i = 0; i < columns.size(); i++) {
            PileLayout.Column column = columns.get(i);
            RandomSource rand = RandomSource.create(seedBase + 31L * i);
            ItemStack coin = coinStacks.computeIfAbsent(column.denomination(), d -> new ItemStack(CoinItem.forDenomination(d)));

            pose.pushPose();
            pose.translate(column.x() / COIN_SCALE_HORIZONTAL, column.z() / COIN_SCALE_HORIZONTAL, 0.0f);
            pose.scale(1, 1, column.thickness());
            // rest the first coin's bottom on the floor: after the scale above this is half a
            // (thickened) coin's thickness
            pose.translate(0f, 0f, -LAYER * 0.5f);
            for (int layer = 0; layer < column.height(); layer++) {
                pose.pushPose();
                pose.translate(
                        ((rand.nextFloat() - 0.5f) * 2f * JITTER) / COIN_SCALE_HORIZONTAL,
                        ((rand.nextFloat() - 0.5f) * 2f * JITTER) / COIN_SCALE_HORIZONTAL,
                        layer * -LAYER);
                itemRenderer.renderStatic(coin, ItemDisplayContext.FIXED, light, overlay, pose, buffers, null,
                        seedBase + i + layer);
                pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
    }
}
