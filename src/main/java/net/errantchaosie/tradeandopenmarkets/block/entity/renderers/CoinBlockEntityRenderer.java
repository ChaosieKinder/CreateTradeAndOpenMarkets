package net.errantchaosie.tradeandopenmarkets.block.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.errantchaosie.tradeandopenmarkets.block.entity.CoinBlockEntity;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

public class CoinBlockEntityRenderer implements BlockEntityRenderer<CoinBlockEntity> {
    private final ItemRenderer itemRenderer;
    public CoinBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CoinBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buf, int light, int overlay) {
        var info = be.getCurrency();
        if (info == null || info.isEmpty() || be.getLevel() == null) return;

        int seed = (int) be.getBlockPos().asLong();

        pose.pushPose();
        // center X/Z, lift slightly above the inner floor of the glass block
        //final float innerFloorY = 0.01f;      // render "inside" the block
        pose.translate(0.5, 0.01f, 0.5);

        // five piles at corners+center (stay well inside 1x1 bounds)
        renderPile(seed + 1, pose, buf, light, overlay, TAOMItems.COIN_COPPER.get(),   info.coinsCopper(),   -0.28f, -0.28f);
        renderPile(seed + 2, pose, buf, light, overlay, TAOMItems.COIN_BRONZE.get(),   info.coinsBronze(),    0.28f, -0.28f);
        renderPile(seed + 3, pose, buf, light, overlay, TAOMItems.COIN_SILVER.get(),   info.coinsSilver(),   -0.28f,  0.28f);
        renderPile(seed + 4, pose, buf, light, overlay, TAOMItems.COIN_ELECTRUM.get(), info.coinsElectrum(),  0.28f,  0.28f);
        renderPile(seed + 5, pose, buf, light, overlay, TAOMItems.COIN_EMERALD.get(),  info.coinsEmerald(),   0.00f,  0.00f);

        pose.popPose();
    }

    private static final int COIN_MAX_TOWER_SIZE = 10;
    private static final float COIN_SCALE = 0.15f;
    private static final float COIN_THICKNESS = 0.2f;            // coin base thickness
    private static final float COIN_THICKNESS_OFFSET = 0.0625f;            // vertical spacing per coin
    private static final float JITTER = 0.20f;                    // stay inside the block walls

    private void renderPile(int seed, PoseStack pose, MultiBufferSource buf, int light, int overlay,
                            Item coinItem, int amount, float xOff, float zOff) {
        if (amount <= 0) return;

        var rand = RandomSource.create(seed);
        var stack = new ItemStack(coinItem);

        int numPiles = Math.min(25,Math.max(1, amount / COIN_MAX_TOWER_SIZE));
        int coinsPerPile = amount / numPiles / numPiles;

        pose.pushPose();
        pose.translate(xOff, 0, zOff);
        pose.scale(COIN_SCALE, COIN_THICKNESS, COIN_SCALE);
        pose.scale(1, numPiles, 1);
        pose.mulPose(Axis.XP.rotationDegrees(90f)); // Lay the pile flat: rotate the item 90° around X so it “faces up”

        for (int p = 0; p < numPiles; p++) {
            pose.pushPose();
            if(p > 0) {
                float rotation = p > 9  ? 22.5f : 45f;
                pose.mulPose(Axis.ZP.rotationDegrees(p * rotation)); // change rotation of coin
                pose.translate((p > 9  ? 1.6f : 0.8f), 0, 0);                 // shift off towards rotation on X
            }
            for (int i = 0; i < coinsPerPile; i++) {
                pose.pushPose();
                // Jitter in the *coin plane*; after the X-rotation, Y here is “height”
                float dx = (rand.nextFloat() - 0.5f) * JITTER;
                float dy = (rand.nextFloat() - 0.5f) * JITTER;
                float dz = i * COIN_THICKNESS_OFFSET;

                // After the X-rotation: (dx, dz) are lateral, dz is “stack height” where negative is up due to rotation
                pose.translate(dx, dy, -dz);

                itemRenderer.renderStatic(
                        stack,
                        ItemDisplayContext.FIXED,   // no auto-tilt; we control orientation
                        light,
                        overlay,
                        pose,
                        buf,
                        null,                       // level (only needed for certain effects)
                        seed + i
                );

                pose.popPose();
            }
            pose.popPose();
        }

        pose.popPose();
    }


    @Override
    public boolean shouldRenderOffScreen(CoinBlockEntity be) {
        return false; // piles are small; no need to render off-screen
    }


    private int getLightLevel(Level level, BlockPos pos) {
        int bLight = level.getBrightness(LightLayer.BLOCK, pos);
        int sLight = level.getBrightness(LightLayer.SKY, pos);
        return LightTexture.pack(bLight, sLight);
    }
}