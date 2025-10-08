package net.errantchaosie.tradeandopenmarkets.block;

import com.mojang.serialization.MapCodec;
import net.errantchaosie.tradeandopenmarkets.block.entity.CoinBlockEntity;
import net.errantchaosie.tradeandopenmarkets.component.CurrencyInfo;
import net.errantchaosie.tradeandopenmarkets.item.TAOMItems;
import net.errantchaosie.tradeandopenmarkets.item.coins.CoinItemBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CoinBlock extends BaseEntityBlock {
    public static final VoxelShape SHAPE = Block.box(0,0,0,16,1,16);

    public static final MapCodec<CoinBlock> CODEC = simpleCodec(CoinBlock::new);

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
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos,
                                  PathComputationType type) {
        return true;
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
        // only handle coin items; otherwise let vanilla/other handlers run
        Item coinItem = held.getItem();
        CoinItemBase.Denomination denomIndex = CoinItemBase.getDenomination(coinItem); // 0=copper,1=bronze,2=silver,3=electrum,4=emerald, -1=not a coin
        if (denomIndex == CoinItemBase.Denomination.NONE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            // client gets the “yes” for hand animation; server does the real work
            return ItemInteractionResult.SUCCESS;
        }

        // get our block entity
        var be = level.getBlockEntity(pos);
        if (!(be instanceof CoinBlockEntity coinBE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // how many are we trying to insert?
        int want = player.isCrouching() ? held.getCount() : 1;

        // current balances
        CurrencyInfo c = coinBE.getCurrency();
        int copper   = c.coinsCopper();
        int bronze   = c.coinsBronze();
        int silver   = c.coinsSilver();
        int electrum = c.coinsElectrum();
        int emerald  = c.coinsEmerald();

        // figure remaining capacity for this pile
        int current = switch (denomIndex) {
            case CoinItemBase.Denomination.COPPER -> copper;
            case CoinItemBase.Denomination.BRONZE -> bronze;
            case CoinItemBase.Denomination.SILVER -> silver;
            case CoinItemBase.Denomination.ELECTRUM -> electrum;
            case CoinItemBase.Denomination.EMERALD -> emerald;
            default -> 0;
        };
        int canAccept = Math.max(0, Integer.MAX_VALUE - current);
        if (canAccept <= 0) {
            // pile is full; do nothing
            return ItemInteractionResult.CONSUME; // “handled” but no change; choose PASS if you prefer
        }

        int toAdd = Math.min(want, canAccept);

        // update the correct pile
        switch (denomIndex) {
            case CoinItemBase.Denomination.COPPER -> copper   += toAdd;
            case CoinItemBase.Denomination.BRONZE -> bronze   += toAdd;
            case CoinItemBase.Denomination.SILVER -> silver   += toAdd;
            case CoinItemBase.Denomination.ELECTRUM -> electrum += toAdd;
            case CoinItemBase.Denomination.EMERALD -> emerald  += toAdd;
        }

        // write back & sync
        coinBE.setCurrency(new CurrencyInfo(copper, bronze, silver, electrum, emerald, c.owner()));
        coinBE.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);

        // consume from player hand (skip if you want creative to be free)
        if (player == null || !player.getAbilities().instabuild) {
            held.shrink(toAdd);
        }

        return ItemInteractionResult.CONSUME; // interaction fully handled
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        BlockState below = ctx.getLevel().getBlockState(pos.below());
        if (below.is(TAOMBlocks.COIN_BLOCK.get())) return null; // disallow stacking
        return super.getStateForPlacement(ctx); // or return defaultBlockState();
    }
}
