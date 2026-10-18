package com.errantmc.markets.item.idcard;

import com.errantmc.markets.component.IdentityCardComponent;
import com.errantmc.markets.component.IdentityCardData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.Level;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class IdentityCardItem extends Item {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

    public IdentityCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return IdentityCardComponent.hasIdentity(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(stack);
        }

        if (!player.isCrouching()) {
            return InteractionResultHolder.pass(stack);
        }

        if (IdentityCardComponent.hasIdentity(stack)) {
            player.displayClientMessage(Component.translatable("item.erranttrade.id_card.already_bound"), true);
            return InteractionResultHolder.fail(stack);
        }

        IdentityCardData data = IdentityCardData.of(player);
        IdentityCardComponent.set(stack, data);
        player.displayClientMessage(Component.translatable("item.erranttrade.id_card.bound", data.playerName()), true);
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1.1F);
        if (player instanceof ServerPlayer serverPlayer) IdentityCardDataSyncHandler.sendTo(serverPlayer); // fill the new card now
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Optional<IdentityCardData> data = IdentityCardComponent.get(stack);
        if (data.isPresent()) {
            IdentityCardData identity = data.get();
            Component ownerLine = Component.translatable("tooltip.erranttrade.id_card.owner", identity.playerName()).withStyle(ChatFormatting.GRAY);
            Component dateComponent = Component.literal(formatDate(identity.createdAtMillis()));
            Component createdLine = Component.translatable("tooltip.erranttrade.id_card.created", dateComponent).withStyle(ChatFormatting.DARK_GRAY);
            tooltip.add(ownerLine);
            tooltip.add(createdLine);
        } else {
            tooltip.add(Component.translatable("item.erranttrade.id_card.unassigned").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.erranttrade.id_card.instructions").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return IdentityCardComponent.get(stack).map(identity -> {
            Component ownerLine = Component.translatable("tooltip.erranttrade.id_card.owner", identity.playerName());
            Component dateComponent = Component.literal(formatDate(identity.createdAtMillis()));
            Component createdLine = Component.translatable("tooltip.erranttrade.id_card.created", dateComponent);
            return new IdentityCardTooltip(identity, ownerLine, createdLine);
        });
    }

    private static String formatDate(long epochMillis) {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }
}