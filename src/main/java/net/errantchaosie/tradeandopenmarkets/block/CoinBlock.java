package net.errantchaosie.tradeandopenmarkets.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.errantchaosie.tradeandopenmarkets.block.entity.CoinBlockEntity;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyData;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
import net.errantchaosie.tradeandopenmarkets.item.CoinItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CoinBlock extends BaseEntityBlock {
    public static final Logger LOG = LogUtils.getLogger();
    public static final VoxelShape SHAPE = Block.box(2,0,2,14,1,14);
    public static final MapCodec<CoinBlock> CODEC = simpleCodec(CoinBlock::new);
    // --- double-tap tracker (server-side) ---
    private static final int DOUBLE_TAP_WINDOW_TICKS = 5; // ~0.25s
    private static final Map<UUID, DoubleTap> LAST_SNEAK_USE = new ConcurrentHashMap<>();
    private record DoubleTap(long tick, BlockPos pos) {}

    public CoinBlock(Properties properties) {
        super(properties
                .noCollission()
                .noOcclusion()
                .sound(SoundType.METAL));
    }

    @Override
    protected MapCodec<? extends CoinBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        // No physical collision at all
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new CoinBlockEntity(blockPos, blockState);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        // Only if we are really being replaced by a different block
        if (!oldState.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CoinBlockEntity coinBE) {
                    CurrencyData c = coinBE.getCurrency();
                    // Drop each denomination
                    dropStacks(level, pos, TAOMItems.COIN_COPPER_BIT.get(),   c.coinsCopperBit());
                    dropStacks(level, pos, TAOMItems.COIN_COPPER_SHARD.get(),   c.coinsCopperShard());
                    dropStacks(level, pos, TAOMItems.COIN_SILVER_BIT.get(),   c.coinsSilverBit());
                    dropStacks(level, pos, TAOMItems.COIN_SILVER_CROWN.get(),     c.coinsSilverCrown());
                    dropStacks(level, pos, TAOMItems.COIN_GOLD_MARK.get(), c.coinsGoldMark());
                    dropStacks(level, pos, TAOMItems.COIN_GOLD_REGAL.get(), c.coinsGoldRegal());
                    dropStacks(level, pos, TAOMItems.COIN_EMERALD_SUN.get(),  c.coinsEmeraldSun());
                    dropStacks(level, pos, TAOMItems.COIN_EMERALD_THRONE.get(),  c.coinsEmeraldThrone());

                    // Optional: clear to avoid dupes if something else reads it later
                    coinBE.setCurrency(CurrencyData.EMPTY);
                }
            }
            // IMPORTANT: call super AFTER your logic (BE might be removed by super)
            super.onRemove(oldState, level, pos, newState, isMoving);
        } else {
            super.onRemove(oldState, level, pos, newState, isMoving);
        }
    }

    private static void dropStacks(Level level, BlockPos pos, Item item, int total) {
        if (total <= 0) return;
        final int max = item.getDefaultMaxStackSize(); // usually 64
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;

        while (total > 0) {
            int n = Math.min(total, max);
            total -= n;
            ItemStack stack = new ItemStack(item, n);
            // Either of these is fine; use one:
            // net.minecraft.world.Containers.dropItemStack(level, x, y, z, stack);
            Block.popResource(level, pos, stack); // simple helper to spawn the item entity
        }
    }

    // (Optional) If you want to suppress drops in Creative (vanilla chests do this),
    // handle it in playerWillDestroy and clear the BE instead of dropping:
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.getAbilities().instabuild) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CoinBlockEntity coinBE) {
                coinBE.setCurrency(CurrencyData.EMPTY);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        // Either of these is fine; pick one:
        // return Block.canSupportCenter(level, below, Direction.UP);
        return below.isFaceSturdy(level, belowPos, Direction.UP)
                && !below.is(TAOMBlocks.COIN_BLOCK.get());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        BlockState below = ctx.getLevel().getBlockState(pos.below());
        if (below.is(TAOMBlocks.COIN_BLOCK.get())) return null; // disallow stacking
        return super.getStateForPlacement(ctx); // or return defaultBlockState();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // If the block below changed and we no longer have support, break the block
        if (dir == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState(); // removal; will call onRemove
        }
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held,
                                              BlockState state,
                                              Level level,
                                              BlockPos pos,
                                              Player player,
                                              InteractionHand hand,
                                              BlockHitResult hit) {
        LOG.info("CoinBlock - useItemOn()");
        boolean did = handleInteraction(state, level, pos, player, held, hand);
        return did
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state,
                                               Level level,
                                               BlockPos pos,
                                               Player player,
                                               BlockHitResult hit) {
        LOG.info("CoinBlock - useWithoutItem()");
        // When sneaking, MC calls this even if the player IS holding a coin.
        // So, peek both hands and pass a coin stack if present.
        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();
        ItemStack held = CoinItem.isCoinItem(main.getItem()) ? main
                : CoinItem.isCoinItem(off.getItem())  ? off
                : ItemStack.EMPTY;

        boolean did = handleInteraction(state, level, pos, player, held, InteractionHand.OFF_HAND);
        return did
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }

    protected boolean handleInteraction(
                                              BlockState state,
                                              Level level,
                                              BlockPos pos,
                                              Player player,
                                              ItemStack held,
                                              InteractionHand hand) {
        boolean sneaking = player.isCrouching() || player.isFallFlying();
        boolean isCoin   = !held.isEmpty() && CoinItem.isCoinItem(held.getItem());

        // Not sneaking and not a coin-in-hand → let other handlers run
        if (!sneaking && (!isCoin && !held.isEmpty())) {
            return false;
        }

        if (!(level.getBlockEntity(pos) instanceof CoinBlockEntity be)){
            return false;
        }

        // CLIENT: claim the interaction so the animation/GUI suppression happens.
        if (level.isClientSide) {
            return true;
        }

        // SERVER: double-tap detection
        if(sneaking)
        {
            long now = level.getGameTime();
            var id   = player.getUUID();
            var prev = LAST_SNEAK_USE.get(id);
            boolean doubleTap = sneaking && prev != null && prev.pos.equals(pos) && (now - prev.tick) <= DOUBLE_TAP_WINDOW_TICKS;
            LAST_SNEAK_USE.put(id, new DoubleTap(now, pos.immutable()));

            if (doubleTap) {
                int moved = dumpAllCoinsFromInventory(player, be);
                if (moved > 0) {
                    sync(be, level, pos, state);
                }
                return true; // handled either way (we suppress other interactions)
            }
        }

        // Single tap behavior:
        // - sneaking + holding coin → insert WHOLE hand stack
        // - sneaking + empty hand → arm for double tap (do nothing else)
        if (isCoin) {
            int moved = insertFromHeldStack(held, player, be);
            if (moved > 0) {
                sync(be, level, pos, state);
            }
            return true;
        }

        // sneaking + empty/non-coin: handled (we’ve armed the double-tap window)
        return sneaking;
    }

    // ---------- helpers ----------
    private static void sync(BlockEntity be, Level level, BlockPos pos, BlockState state) {
        be.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }


    /**
     * Attempts to insert an item stack, removing the in-hand item stack if the player is not in creative
     * @param held
     * @param player
     * @param be
     * @return the number of items added from the in-hand stack. (might be less then requested if pile is full)
     */
    private static int insertFromHeldStack(ItemStack held, Player player, CoinBlockEntity be) {
        int res = 0;
        if (!held.isEmpty() && CoinItem.isCoinItem(held.getItem())) {
            var result = be.getCurrency().add(held);
            be.setCurrency(result.updated());
            if (!player.getAbilities().instabuild) held.shrink(result.delta());
            res = result.delta();
        }
        return res;
    }

    private static int dumpAllCoinsFromInventory(Player player, CoinBlockEntity be) {
        CurrencyData c = be.getCurrency();
        int movedTotal = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !CoinItem.isCoinItem(s.getItem())) continue;
            var addResult = c.add(s);
            be.setCurrency(addResult.updated());
            if (!player.getAbilities().instabuild) {
                s.shrink(addResult.delta());
                if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
            }
            movedTotal += addResult.delta();
        }
        // offhand, too
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && CoinItem.isCoinItem(off.getItem())) {
            var addResult = c.add(off);
            be.setCurrency(addResult.updated());
            if (!player.getAbilities().instabuild) {
                off.shrink(addResult.delta());
            }
            movedTotal += addResult.delta();
        }
        if (movedTotal > 0) be.setCurrency(c);
        return movedTotal;
    }

}
