package net.errantchaosie.tradeandopenmarkets.block;

import com.mojang.serialization.MapCodec;
import net.errantchaosie.tradeandopenmarkets.block.entity.SafeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

public class SafeBlock extends BaseEntityBlock {
    //public static final Property<Integer> OWNER = IntegerProperty.create("owner", 0, Integer.MAX_VALUE);


    public static final MapCodec<SafeBlock> CODEC = simpleCodec(SafeBlock::new);

    public SafeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SafeBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SafeBlockEntity(blockPos, blockState);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if(state.getBlock() != newState.getBlock()) {
            if(level.getBlockEntity(pos) instanceof SafeBlockEntity safeBlockEntity) {
                safeBlockEntity.drops();
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if(level.getBlockEntity(pos) instanceof SafeBlockEntity safeBlockEntity
                && !level.isClientSide()) {
            if(false && player.isCrouching()) {
                // check for in-hand safe upgrades
                if(safeBlockEntity.inventory.getStackInSlot(0).isEmpty()) {
                    safeBlockEntity.inventory.insertItem(0, stack.copy(), false);
                    stack.shrink(1);
                    level.playSound(player, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 2f);
                    return ItemInteractionResult.SUCCESS;
                }
            }
            ((ServerPlayer) player).openMenu(new SimpleMenuProvider(safeBlockEntity, Component.literal("Safe")), pos);
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public @org.jetbrains.annotations.Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = this.defaultBlockState();
        // always place the safe facing the player.
        state = state.setValue(FACING, context.getHorizontalDirection().getOpposite());

        //state.setValue(OWNER,  playerId != null ? playerId : 0);
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

}
