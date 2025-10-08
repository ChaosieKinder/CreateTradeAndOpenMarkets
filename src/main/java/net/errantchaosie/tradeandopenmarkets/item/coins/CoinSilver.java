package net.errantchaosie.tradeandopenmarkets.item.coins;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CoinSilver extends CoinItemBase {

    public static final String FormatCode = "§i";

    public CoinSilver(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if(!stack.isEmpty())
            tooltipComponents.add(Component.translatable("tooltip_info_item.coin")
                    .append(FormatCode) // Silver Color
                    .append(Component.literal(String.valueOf(stack.getCount() * 64))));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
