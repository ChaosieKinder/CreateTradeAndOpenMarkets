package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.component.IdentityCardComponent;
import com.errantmc.markets.item.TradeItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.errantmc.markets.gametest.GameTests.*;

@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IdentityCardGameTests {
    private IdentityCardGameTests() {}

    @GameTest(template = ARENA)
    public static void sneakUsingABlankCardBindsItToThePlayer(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack card = holdCard(player);
        player.setPose(Pose.CROUCHING);

        card.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        var identity = IdentityCardComponent.get(player.getMainHandItem());
        assertTrue(helper, "the card to be bound", identity.isPresent());
        assertTrue(helper, "the card to name its holder", identity.get().playerId().equals(player.getUUID()));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void usingACardWithoutSneakingDoesNotBindIt(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ItemStack card = holdCard(player);

        card.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        assertTrue(helper, "the card to stay blank", !IdentityCardComponent.hasIdentity(player.getMainHandItem()));
        helper.succeed();
    }

    /** A bound card belongs to whoever bound it; nobody else can take it over. */
    @GameTest(template = ARENA)
    public static void aBoundCardCannotBeRebound(GameTestHelper helper) {
        ServerPlayer owner = survivalPlayer(helper);
        ItemStack card = holdCard(owner);
        owner.setPose(Pose.CROUCHING);
        card.use(helper.getLevel(), owner, InteractionHand.MAIN_HAND);

        ServerPlayer thief = survivalPlayer(helper);
        thief.setItemInHand(InteractionHand.MAIN_HAND, owner.getMainHandItem().copy());
        thief.setPose(Pose.CROUCHING);
        thief.getMainHandItem().use(helper.getLevel(), thief, InteractionHand.MAIN_HAND);

        var identity = IdentityCardComponent.get(thief.getMainHandItem());
        assertTrue(helper, "the card to still name its first owner",
                identity.isPresent() && identity.get().playerId().equals(owner.getUUID()));
        helper.succeed();
    }

    private static ItemStack holdCard(ServerPlayer player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.ID_CARD.get()));
        return player.getMainHandItem();
    }
}
