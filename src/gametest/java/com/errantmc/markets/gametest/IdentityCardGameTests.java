package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.component.IdentityCardComponent;
import com.errantmc.markets.component.IdentityCardData;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.item.idcard.IdentityCardDataSyncHandler;
import com.errantmc.markets.item.wallet.WalletInventory;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

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

    // ---------- who sees whose live data (BACKLOG #17) ----------

    /** A snapshot carries its subject's position, so nobody may receive one for a card they do not carry. */
    @GameTest(template = ARENA)
    public static void aPlayerWithoutCardsIsSentNoOnesData(GameTestHelper helper) {
        survivalPlayer(helper); // someone else online
        ServerPlayer viewer = survivalPlayer(helper);

        assertCount(helper, "snapshots for a player carrying no cards", 0,
                IdentityCardDataSyncHandler.snapshotsFor(viewer).size());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void carryingSomeonesCardSendsOnlyTheirData(GameTestHelper helper) {
        ServerPlayer owner = survivalPlayer(helper);
        ServerPlayer bystander = survivalPlayer(helper);
        ServerPlayer viewer = survivalPlayer(helper);
        ItemStack card = new ItemStack(TradeItems.ID_CARD.get());
        IdentityCardComponent.set(card, IdentityCardData.of(owner));
        viewer.getInventory().setItem(20, card);

        var sent = IdentityCardDataSyncHandler.snapshotsFor(viewer);

        assertCount(helper, "snapshots", 1, sent.size());
        assertTrue(helper, "the snapshot to be the card owner's", sent.getFirst().playerId().equals(owner.getUUID()));
        assertTrue(helper, "nothing about the bystander",
                sent.stream().noneMatch(s -> s.playerId().equals(bystander.getUUID())));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aCardInsideAWalletCounts(GameTestHelper helper) {
        ServerPlayer owner = survivalPlayer(helper);
        ServerPlayer viewer = survivalPlayer(helper);
        ItemStack card = new ItemStack(TradeItems.ID_CARD.get());
        IdentityCardComponent.set(card, IdentityCardData.of(owner));
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        List<ItemStack> grid = new ArrayList<>(WalletInventory.read(wallet));
        grid.set(4, card);
        WalletInventory.write(wallet, grid);
        viewer.getInventory().setItem(9, wallet);

        assertTrue(helper, "the owner's card in the wallet to count",
                IdentityCardDataSyncHandler.cardOwnersCarriedBy(viewer).contains(owner.getUUID()));
        helper.succeed();
    }

    private static ItemStack holdCard(ServerPlayer player) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TradeItems.ID_CARD.get()));
        return player.getMainHandItem();
    }
}
