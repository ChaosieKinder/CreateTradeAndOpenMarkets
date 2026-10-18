package com.errantmc.markets.item.wallet;

import com.errantmc.markets.component.Purses;
import com.errantmc.markets.domain.MoneyFormat;
import com.errantmc.markets.domain.Purse;
import com.errantmc.markets.item.CoinStyle;
import com.errantmc.markets.screen.wallet.WalletMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class WalletItem extends Item {


    public WalletItem(Properties props) { super(props.stacksTo(1)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // If uncommented - will only open wallet if user is crouching
        //if (!player.isCrouching()) return InteractionResultHolder.pass(stack);

        if (!level.isClientSide) {
            WalletInventory.ensure(stack);      // 9 slots
            ((ServerPlayer) player).openMenu(
                    new SimpleMenuProvider((id, inv, p) -> new WalletMenu(id, inv, hand),
                            Component.translatable("item.erranttrade.leather_wallet")),
                    buf -> buf.writeEnum(hand)
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tooltip, flag);
        Purse balance = Purses.get(stack);
        tooltip.add(Component.translatable("item.erranttrade.wallet.tooltip_prefix")
                .append(Component.literal(MoneyFormat.format(balance.totalValue()))
                        .withStyle(CoinStyle.colour(balance.highest()))));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        List<ItemStack> contents = WalletInventory.read(stack);
        boolean hasItems = contents.stream().anyMatch(item -> !item.isEmpty());
        if (!hasItems) {
            return Optional.empty();
        }
        return Optional.of(new WalletTooltip(contents));
    }
}
