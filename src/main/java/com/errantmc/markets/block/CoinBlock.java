package com.errantmc.markets.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.errantmc.markets.block.entity.CoinBlockEntity;
import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.CoinDenomination;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.item.CoinDeposits;
import com.errantmc.markets.item.CoinStackItem;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.item.CoinItem;
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
        // Only when really replaced by a different block; before super, which removes the block entity
        if (!oldState.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof CoinBlockEntity coinBE) {
            Purse coins = coinBE.getCurrency();
            if (stacksFor(coins) > MAX_DROPPED_STACKS) {
                // One coin stack instead of hundreds of item entities (README)
                ItemStack compressed = new ItemStack(TradeItems.COIN_STACK_ITEM.get());
                Purses.set(compressed, coins);
                Block.popResource(level, pos, compressed);
            } else {
                for (CoinDenomination d : CoinDenomination.coins()) dropStacks(level, pos, d, coins.count(d));
            }
            coinBE.setCurrency(Purse.EMPTY); // nothing left to drop twice
        }
        super.onRemove(oldState, level, pos, newState, isMoving);
    }

    /** Beyond this many item stacks a broken pile drops as one coin stack. */
    private static final int MAX_DROPPED_STACKS = 20;

    private static int stacksFor(Purse coins) {
        int stacks = 0;
        for (CoinDenomination d : CoinDenomination.coins()) {
            int max = CoinItem.forDenomination(d).getDefaultMaxStackSize();
            stacks += (coins.count(d) + max - 1) / max;
        }
        return stacks;
    }

    private static void dropStacks(Level level, BlockPos pos, CoinDenomination denomination, int total) {
        Item item = CoinItem.forDenomination(denomination);
        int max = item.getDefaultMaxStackSize();
        while (total > 0) {
            int n = Math.min(total, max);
            total -= n;
            Block.popResource(level, pos, new ItemStack(item, n));
        }
    }

    /** As with chests, a creative player breaking a pile gets nothing. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.getAbilities().instabuild) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CoinBlockEntity coinBE) {
                coinBE.setCurrency(Purse.EMPTY);
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
                && !below.is(TradeBlocks.COIN_PILE.get());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        BlockState below = ctx.getLevel().getBlockState(pos.below());
        if (below.is(TradeBlocks.COIN_PILE.get())) return null; // disallow stacking
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
        boolean sneaking = player.isCrouching();
        boolean isCoin   = !held.isEmpty()
                && CoinItem.isCoinItem(held.getItem());
        boolean isCoinStack = !held.isEmpty()
                && held.getItem() instanceof CoinStackItem;

        // Not sneaking and not a coin-in-hand → let other handlers run
        if (!sneaking && (!isCoin && !isCoinStack && !held.isEmpty())) {
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
        if(sneaking && !isCoinStack)
        {
            long now = level.getGameTime();
            var id   = player.getUUID();
            var prev = LAST_SNEAK_USE.get(id);
            boolean doubleTap = sneaking && prev != null && prev.pos.equals(pos) && (now - prev.tick) <= DOUBLE_TAP_WINDOW_TICKS;
            // Expired taps can never complete a double tap; dropping them keeps the map at active players
            LAST_SNEAK_USE.values().removeIf(t -> now - t.tick() > DOUBLE_TAP_WINDOW_TICKS);
            LAST_SNEAK_USE.put(id, new DoubleTap(now, pos.immutable()));

            if (doubleTap) {
                if (depositEveryCoin(player, be)) sync(be, level, pos, state);
                return true; // handled either way (we suppress other interactions)
            }
        }

        // A single tap with coins or a coin stack in hand deposits it; with an empty hand while
        // sneaking it only arms the double tap
        if (isCoin || isCoinStack) {
            if (deposit(held, player, be)) sync(be, level, pos, state);
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


    /** Puts what fits of a held coin or coin stack into the pile; a creative hand is not charged. */
    private static boolean deposit(ItemStack held, Player player, CoinBlockEntity be) {
        Purse before = be.getCurrency();
        Purse after = CoinDeposits.absorb(before, held, !player.getAbilities().instabuild);
        if (after.equals(before)) return false;
        be.setCurrency(after);
        return true;
    }

    /** The double tap: every coin in the inventory, armour and offhand included. */
    private static boolean depositEveryCoin(Player player, CoinBlockEntity be) {
        Purse before = be.getCurrency();
        Purse after = before;
        boolean consume = !player.getAbilities().instabuild;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof CoinItem) after = CoinDeposits.absorb(after, s, consume);
        }
        if (after.equals(before)) return false;
        be.setCurrency(after);
        return true;
    }

}
