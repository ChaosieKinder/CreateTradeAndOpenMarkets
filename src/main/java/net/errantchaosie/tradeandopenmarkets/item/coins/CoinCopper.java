package net.errantchaosie.tradeandopenmarkets.item.coins;

import net.errantchaosie.tradeandopenmarkets.block.TAOMBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

public class CoinCopper extends CoinItemBase {

    public static final String FormatCode = "§6";

    public CoinCopper(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if(!stack.isEmpty())
            tooltipComponents.add(Component.translatable("tooltip_info_item.coin")
                    .append(FormatCode) // Copper Color
                    .append(Component.literal(String.valueOf(stack.getCount()))));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

}
