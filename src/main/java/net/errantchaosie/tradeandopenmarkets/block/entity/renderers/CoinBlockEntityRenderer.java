package net.errantchaosie.tradeandopenmarkets.block.entity.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.errantchaosie.tradeandopenmarkets.block.entity.CoinBlockEntity;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyData;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jline.utils.Log;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CoinBlockEntityRenderer implements BlockEntityRenderer<CoinBlockEntity> {
    // ---------- Tuning knobs ----------
    // base vertical spacing per layer (one coin) before scaling = 0.0625
    private static final float Layer_Z = (1.0f/16f);
    // size of coin items
    private static final float COIN_SCALE_HORIZONTAL    = 0.15f;
    // vertical spacing per layer (one coin)
    private static final float COIN_SCALE_VERTICAL      = 0.20f;
    // render “inside” the block = 0.03125
    private static final float INNER_FLOOR_Z = (Layer_Z / 2);
    // radius for ring #1
    private static final float RING_1_RADIUS            = 0.18f;
    // additional radius per ring
    private static final float RING_STEP                = 0.15f;
    // stay within block bounds
    private static final float RING_RADIUS_MAX          = 0.44f;
    // desired gap between stack edges
    private static final float RING_GAP_MIN             = 0.06f;
    // coins per stack cap
    private static final int   STACK_MAX_HEIGHT_STEP    = 10;
    // approximate lateral footprint radius of one stack in block units
    // Item’s quad is ~0.5 block wide in GROUND/FIXED; scale shrinks it.
    private static final float STACK_FOOTPRINT_RADIUS   = 0.25f * COIN_SCALE_HORIZONTAL;
    // minor x/z jitter per layer/coin
    private static final float SLOT_JITTER              = 0.015f;
    // maximum number of slots allowed, calculated from other values
    private static final int   SLOT_MAXIMUM             = getMaxSlotsCapacity();

    // ---------- Constructor ----------
    private final ItemRenderer itemRenderer;
    public CoinBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public boolean shouldRenderOffScreen(CoinBlockEntity be) {
        return false; // piles are small; no need to render off-screen
    }

    @Override
    public void render(CoinBlockEntity be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
        var level = be.getLevel();
        var info  = be.getCurrency();
        if (level == null || info == null) return;
        int total = info.totalCount();
        if (total <= 0) return;
        var coinHeight = 0;

        // Build stack descriptors (largest denomination toward center)
        List<StackVis> stacks;
        do {
            coinHeight += STACK_MAX_HEIGHT_STEP;
            stacks = getStacks(info, coinHeight);
        } while (stacks.size() > SLOT_MAXIMUM);

        if(stacks.isEmpty()) return;

        // Optionally “thicken” full stacks (two coins per layer for full 10)
        if(coinHeight > 10)
            thickenFullStacks(stacks, coinHeight);

        // Generate ring positions (center, then 6, 12, 18, …)
        List<Slot> slots = ringSlots(stacks.size());

        // Render
        final int seedBase = (int) be.getBlockPos().asLong();
        pose.pushPose();
        pose.rotateAround(com.mojang.math.Axis.XP.rotationDegrees(90f), 0,0,0);
        pose.scale(COIN_SCALE_HORIZONTAL,
                COIN_SCALE_HORIZONTAL,
                COIN_SCALE_VERTICAL);
        pose.translate(0.5f / COIN_SCALE_HORIZONTAL, 0.5f / COIN_SCALE_HORIZONTAL, -INNER_FLOOR_Z);
        //pose.translate(0.5, 0.5, INNER_FLOOR_Y);
        for (int i = 0; i < stacks.size(); i++) {
            StackVis s = stacks.get(i);
            Slot slot  = slots.get(i);
            RandomSource rand = RandomSource.create(seedBase + 31 * i);

            pose.pushPose();
            // grow base model height if "thick"
            pose.translate(slot.x / COIN_SCALE_HORIZONTAL,
                    slot.z / COIN_SCALE_HORIZONTAL,
                    0.0f);
            pose.scale(1, 1, s.thick);

            // 3) rest the first coin's BOTTOM on the floor:
            //    after the Z-scale above, this local -Layer_Z/2 becomes
            //    world -(Layer_Z/2) * COIN_SCALE_VERTICAL * s.thick  (correct half-thickness)
            pose.translate(0f, 0f, -Layer_Z * 0.5f);
            // Render the coin stack
            ItemStack coin = new ItemStack(s.item);

            for (int layer = 0; layer < s.height; layer++) {
                float dz = layer * -Layer_Z;
                pose.pushPose();
                pose.translate( // move to the coin's world-space location
                        ((rand.nextFloat() - 0.5f) * 2f * SLOT_JITTER) / COIN_SCALE_HORIZONTAL,
                        ((rand.nextFloat() - 0.5f) * 2f * SLOT_JITTER) / COIN_SCALE_HORIZONTAL,
                        dz);

                itemRenderer.renderStatic(
                        coin,
                        net.minecraft.world.item.ItemDisplayContext.FIXED,
                        light, overlay, pose, buf, null,
                        seedBase + i + layer
                );
                pose.popPose();
            }

            pose.popPose();
        }

        pose.popPose();
    }

    // ---------- Build stacks from counts ----------
    private static List<StackVis> getStacks(CurrencyData info, int stackSize) {
        List<StackVis> stacks = new ArrayList<>();
        addStacks(stacks, stackSize, TAOMItems.COIN_EMERALD.get(),  info.coinsEmerald());
        addStacks(stacks, stackSize, TAOMItems.COIN_PLATINUM.get(),   info.coinsPlatinum());
        addStacks(stacks, stackSize, TAOMItems.COIN_ELECTRUM.get(), info.coinsElectrum());
        addStacks(stacks, stackSize, TAOMItems.COIN_GOLD.get(),   info.coinsGold());
        addStacks(stacks, stackSize, TAOMItems.COIN_SILVER.get(),   info.coinsSilver());
        addStacks(stacks, stackSize, TAOMItems.COIN_BRONZE.get(),   info.coinsBronze());
        addStacks(stacks, stackSize, TAOMItems.COIN_COPPER.get(),   info.coinsCopper());
        return stacks;
    }

    private static void addStacks(List<StackVis> out, int stackSize, Item coinItem, int count) {
        if(count > 1000) {
            out.add(new StackVis(coinItem, Math.max(stackSize, 50)));
            return;
        }
        while (count > 0) {
            int h = Math.min(stackSize, count);
            out.add(new StackVis(coinItem, h));
            count -= h;
        }
    }

    private static final int[] STACK_LIMITS = new int[] { 4, 3, 2, 5 };
    // Turn any full-height stacks into a “thick” stacks
    // This cuts the amount of jitter/column in favor of decreasing render time for large stacks.
    private static void thickenFullStacks(@NotNull List<StackVis> stacks, int maxHeight) {
        for (int i = 0; i < stacks.size(); i++) {
            StackVis s = stacks.get(i);
            // height = 20, Max = 20, STACK_MAX_HEIGHT_STEP = 10
            if (s.height == maxHeight) {
                var emergencyBreak = false;
                while(s.height >= STACK_MAX_HEIGHT_STEP
                    && !emergencyBreak)
                {
                    emergencyBreak = true;
                    for(int j = 0; j < STACK_LIMITS.length && emergencyBreak; j++) {
                        int lmt = STACK_LIMITS[j];
                        if(s.height % lmt == 0)
                        {
                            emergencyBreak = false;
                            s.thick *= lmt;
                            s.height = s.height / lmt;
                        }
                    }
                }
            }
        }
    }

    // ---------- Ring slot generator ----------
    private static List<Slot> ringSlots(int needed) {
        List<Slot> out = new ArrayList<>(needed);
        if (needed <= 0) return out;

        // center first
        out.add(new Slot(0f, 0f));
        if (needed == 1) return out;

        int placed = 1;
        int ring = 1;
        float stackDiameter = 2f * STACK_FOOTPRINT_RADIUS;
        float targetSpacing = stackDiameter + RING_GAP_MIN;   // min distance along the ring between stack centers

        while (placed < needed) {
            float r = Math.min(RING_1_RADIUS + (ring - 1) * RING_STEP, RING_RADIUS_MAX);
            float circumference = (float) (2 * Math.PI * r);

            int slots = Math.max(6, Mth.floor(circumference / targetSpacing));
            // ensure we can actually place something
            if (slots <= 0) slots = 6;

            for (int i = 0; i < slots && placed < needed; i++) {
                float ang = (float) (i * (2 * Math.PI / slots));
                out.add(new Slot((float) Math.cos(ang) * r, (float) Math.sin(ang) * r));
                placed++;
            }
            ring++;
        }
        return out;
    }

    // ---------- small helpers ----------
    private record Slot(float x, float z) {}
    private static class StackVis {
        final Item item;
        int height; // 1..MAX_HEIGHT
        int thick = 1;    // if 2, two coins per layer, etc
        StackVis(Item item, int height, int thick) {
            this.item = item;
            this.height = height;
            this.thick = thick;
        }
        StackVis(Item item, int height) {
            this(item, height, 1);
        }
    }


    // How many stacks can we place with the current constants?
    private static int getMaxSlotsCapacity() {
        // Guard weird configs
        if (RING_STEP <= 0f || RING_RADIUS_MAX <= 0f) return 1; // center only

        // Same footprint math used by ringSlots(...)
        float stackDiameter = 2f * STACK_FOOTPRINT_RADIUS;
        float targetSpacing = stackDiameter + RING_GAP_MIN;
        if (targetSpacing <= 0f) return 1; // avoid div-by-zero

        // Number of distinct rings we can fit before hitting MAX_RADIUS
        if (RING_1_RADIUS > RING_RADIUS_MAX) return 1; // only the center fits
        int rings = 1 + Mth.floor((RING_RADIUS_MAX - RING_1_RADIUS) / RING_STEP);

        int total = 1; // center slot
        for (int ring = 1; ring <= rings; ring++) {
            float r = RING_1_RADIUS + (ring - 1) * RING_STEP;
            float circumference = (float) (2 * Math.PI * Math.min(r, RING_RADIUS_MAX));
            int slots = Math.max(6, Mth.floor(circumference / targetSpacing));
            total += Math.max(slots, 0);
        }
        return total;
    }

}


