package net.errantchaosie.tradeandopenmarkets.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.errantchaosie.tradeandopenmarkets.block.entity.CoinBlockEntity;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyInfo;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
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
                    CurrencyInfo c = coinBE.getCurrency();
                    // Drop each denomination
                    dropStacks(level, pos, TAOMItems.COIN_COPPER.get(),   c.coinsCopper());
                    dropStacks(level, pos, TAOMItems.COIN_BRONZE.get(),   c.coinsBronze());
                    dropStacks(level, pos, TAOMItems.COIN_SILVER.get(),   c.coinsSilver());
                    dropStacks(level, pos, TAOMItems.COIN_ELECTRUM.get(), c.coinsElectrum());
                    dropStacks(level, pos, TAOMItems.COIN_EMERALD.get(),  c.coinsEmerald());

                    // Optional: clear to avoid dupes if something else reads it later
                    coinBE.setCurrency(CurrencyInfo.EMPTY);
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
                coinBE.setCurrency(CurrencyInfo.EMPTY);
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
        ItemStack held = isCoinItem(main.getItem()) ? main
                : isCoinItem(off.getItem())  ? off
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
        LOG.info("CoinBlock - handleInteraction()");
        LOG.info("CoinBlock - handleInteraction.pos = {}", pos );
        LOG.info("CoinBlock - handleInteraction.held = {}", held );
        LOG.info("CoinBlock - handleInteraction.hand = {}", hand );
        boolean sneaking = player.isCrouching() || player.isFallFlying();
        boolean isCoin   = !held.isEmpty() && isCoinItem(held.getItem());

        LOG.info("CoinBlock - handleInteraction.sneaking = {}", sneaking);
        LOG.info("CoinBlock - handleInteraction.isCoin = {}", isCoin);
        // Not sneaking and not a coin-in-hand → let other handlers run
        if (!sneaking && (!isCoin && !held.isEmpty())) {
            LOG.info("CoinBlock - handleInteraction - failed first sanity check");
            return false;
        }

        if (!(level.getBlockEntity(pos) instanceof CoinBlockEntity be)){
            LOG.info("CoinBlock - handleInteraction - failed second sanity check");
            return false;
        }

        // CLIENT: claim the interaction so the animation/GUI suppression happens.
        if (level.isClientSide) {
            LOG.info("CoinBlock - handleInteraction - suppressed on clientSide");
            return true;
        }

        LOG.info("CoinBlock - handleInteraction - continuing on serverSide");
        // SERVER: double-tap detection
        if(sneaking)
        {
            LOG.info("CoinBlock - handleInteraction - is sneaking");
            long now = level.getGameTime();
            var id   = player.getUUID();
            var prev = LAST_SNEAK_USE.get(id);
            boolean doubleTap = sneaking && prev != null && prev.pos.equals(pos) && (now - prev.tick) <= DOUBLE_TAP_WINDOW_TICKS;
            LAST_SNEAK_USE.put(id, new DoubleTap(now, pos.immutable()));

            if (doubleTap) {
                LOG.info("CoinBlock - handleInteraction - is sneaking - is doubleTap");
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
            LOG.info("CoinBlock - handleInteraction - inserting coin");
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

    private static boolean isCoinItem(Item item) {
        return item == TAOMItems.COIN_COPPER.get()
                || item == TAOMItems.COIN_BRONZE.get()
                || item == TAOMItems.COIN_SILVER.get()
                || item == TAOMItems.COIN_ELECTRUM.get()
                || item == TAOMItems.COIN_EMERALD.get();
    }

    private static int insertFromHeldStack(ItemStack held, Player player, CoinBlockEntity be) {
        LOG.info("CoinBlock - insertFromHeldStack()");
        if (held.isEmpty() || !isCoinItem(held.getItem())) return 0;
        CurrencyInfo c = be.getCurrency();
        int denom   = denomIndex(held.getItem());
        int current = getByDenom(c, denom);
        int space   = Math.max(0, Integer.MAX_VALUE - current);
        int move    = Math.min(space, player.isCrouching() ? held.getCount() : 1);
        LOG.info("CoinBlock - insertFromHeldStack() denom = {}", denom);
        LOG.info("CoinBlock - insertFromHeldStack() current = {}", current);
        LOG.info("CoinBlock - insertFromHeldStack() space = {}", space);
        LOG.info("CoinBlock - insertFromHeldStack() move = {}", move);
        if (move <= 0) return 0;

        c = setByDenom(c, denom, current + move);
        be.setCurrency(c);
        if (!player.getAbilities().instabuild) held.shrink(move);
        return move;
    }

    private static int dumpAllCoinsFromInventory(Player player, CoinBlockEntity be) {
        CurrencyInfo c = be.getCurrency();
        int movedTotal = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !isCoinItem(s.getItem())) continue;
            int denom = denomIndex(s.getItem());
            int current = getByDenom(c, denom);
            int space = Math.max(0, Integer.MAX_VALUE - current);
            int move = Math.min(space, s.getCount());
            if (move <= 0) continue;
            c = setByDenom(c, denom, current + move);
            if (!player.getAbilities().instabuild) {
                s.shrink(move);
                if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
            }
            movedTotal += move;
        }
        // offhand, too
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && isCoinItem(off.getItem())) {
            int denom = denomIndex(off.getItem());
            int current = getByDenom(c, denom);
            int space = Math.max(0, Integer.MAX_VALUE - current);
            int move = Math.min(space, off.getCount());
            if (move > 0) {
                c = setByDenom(c, denom, current + move);
                if (!player.getAbilities().instabuild) off.shrink(move);
                movedTotal += move;
            }
        }
        if (movedTotal > 0) be.setCurrency(c);
        return movedTotal;
    }

    private static int denomIndex(Item item) {
        if (item == TAOMItems.COIN_COPPER.get()) return 0;
        if (item == TAOMItems.COIN_BRONZE.get()) return 1;
        if (item == TAOMItems.COIN_SILVER.get()) return 2;
        if (item == TAOMItems.COIN_ELECTRUM.get()) return 3;
        if (item == TAOMItems.COIN_EMERALD.get()) return 4;
        return -1;
    }
    private static int getByDenom(CurrencyInfo c, int idx) {
        return switch (idx) {
            case 0 -> c.coinsCopper();
            case 1 -> c.coinsBronze();
            case 2 -> c.coinsSilver();
            case 3 -> c.coinsElectrum();
            case 4 -> c.coinsEmerald();
            default -> 0;
        };
    }
    private static CurrencyInfo setByDenom(CurrencyInfo c, int idx, int v) {
        return switch (idx) {
            case 0 -> new CurrencyInfo(v, c.coinsBronze(), c.coinsSilver(), c.coinsElectrum(), c.coinsEmerald(), c.owner());
            case 1 -> new CurrencyInfo(c.coinsCopper(), v, c.coinsSilver(), c.coinsElectrum(), c.coinsEmerald(), c.owner());
            case 2 -> new CurrencyInfo(c.coinsCopper(), c.coinsBronze(), v, c.coinsElectrum(), c.coinsEmerald(), c.owner());
            case 3 -> new CurrencyInfo(c.coinsCopper(), c.coinsBronze(), c.coinsSilver(), v, c.coinsEmerald(), c.owner());
            case 4 -> new CurrencyInfo(c.coinsCopper(), c.coinsBronze(), c.coinsSilver(), c.coinsElectrum(), v, c.owner());
            default -> c;
        };
    }

}
