package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.npcinteractions.components.SimpleShopTab;
import com.errantmc.markets.npcinteractions.registry.InteractionTabManager;
import com.errantmc.markets.npcinteractions.tabs.buy.BuyShopTab;
import com.errantmc.markets.npcinteractions.tabs.buy.BuyShopTabType;
import com.errantmc.markets.npcinteractions.trading.ShopEntry;
import com.errantmc.markets.screen.villager.VillagerInteractionMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.errantmc.markets.gametest.GameTests.*;

@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillagerInteractionGameTests {
    private VillagerInteractionGameTests() {}

    /** Right-clicking a villager opens the mod's interaction menu on the server, in place of vanilla's. */
    @GameTest(template = ARENA)
    public static void rightClickingAVillagerOpensTheInteractionMenu(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityType.VILLAGER, new Vec3(4.5, 1, 4.5));
        villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.FARMER));
        ServerPlayer player = survivalPlayer(helper);

        player.interactOn(villager, InteractionHand.MAIN_HAND);

        assertTrue(helper, "the interaction menu to be open, but it is " + player.containerMenu.getClass().getSimpleName(),
                player.containerMenu instanceof VillagerInteractionMenu);
        helper.succeed();
    }

    /** The client builds its tabs from what the server sends, so they must survive the trip intact. */
    @GameTest(template = ARENA)
    public static void tabsSurviveTheTripToTheClient(GameTestHelper helper) {
        Villager villager = farmer(helper);
        var sent = InteractionTabManager.INSTANCE.resolveTabs(villager, villager.getOffers(), helper.getLevel());
        assertTrue(helper, "the farmer to have tabs", !sent.isEmpty());

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        InteractionTabManager.INSTANCE.writeToStream(buf, sent);
        var received = InteractionTabManager.INSTANCE.readFromStream(buf);

        assertCount(helper, "tabs received", sent.size(), received.size());
        assertCount(helper, "bytes left unread", 0, buf.readableBytes());
        for (int t = 0; t < sent.size(); t++) {
            var a = ((SimpleShopTab) sent.get(t)).entries();
            var b = ((SimpleShopTab) received.get(t)).entries();
            assertCount(helper, "entries in tab " + t, a.size(), b.size());
            for (int e = 0; e < a.size(); e++) {
                assertTrue(helper, "entry " + t + "/" + e + " to arrive unchanged",
                        ItemStack.matches(a.get(e).item(), b.get(e).item())
                                && a.get(e).price() == b.get(e).price() && a.get(e).stock() == b.get(e).stock());
            }
        }
        helper.succeed();
    }

    /** Like vanilla's merchant screen, the menu closes when the player walks away. */
    @GameTest(template = ARENA)
    public static void walkingAwayClosesTheMenu(GameTestHelper helper) {
        Villager villager = farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        player.interactOn(villager, InteractionHand.MAIN_HAND);
        if (!(player.containerMenu instanceof VillagerInteractionMenu menu)) {
            helper.fail("the menu did not open");
            return;
        }
        assertTrue(helper, "the menu to be valid beside the villager", menu.stillValid(player));
        assertTrue(helper, "the villager to be trading with the player", villager.getTradingPlayer() == player);

        player.moveTo(player.getX() + 30, player.getY(), player.getZ());

        assertTrue(helper, "the menu to be invalid 30 blocks away", !menu.stillValid(player));
        player.closeContainer();
        assertTrue(helper, "the villager to be free once the menu closes", villager.getTradingPlayer() == null);
        helper.succeed();
    }

    /** Only the trade itself is taken over; a name tag still names the villager. */
    @GameTest(template = ARENA)
    public static void aNameTagStillNamesAVillager(GameTestHelper helper) {
        Villager villager = farmer(helper);
        ServerPlayer player = survivalPlayer(helper);
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Ada"));
        player.setItemInHand(InteractionHand.MAIN_HAND, tag);

        player.interactOn(villager, InteractionHand.MAIN_HAND);

        assertTrue(helper, "the villager to be named Ada", villager.hasCustomName()
                && villager.getCustomName().getString().equals("Ada"));
        assertTrue(helper, "no trading menu to open", !(player.containerMenu instanceof VillagerInteractionMenu));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aBabyVillagerDoesNotTrade(GameTestHelper helper) {
        Villager villager = farmer(helper);
        villager.setAge(-24000);
        ServerPlayer player = survivalPlayer(helper);

        player.interactOn(villager, InteractionHand.MAIN_HAND);

        assertTrue(helper, "no trading menu to open", !(player.containerMenu instanceof VillagerInteractionMenu));
        helper.succeed();
    }

    /**
     * The shop grid's cells sit on the panel art (tab_sell.png drawn at 17,31: cells from 61,40,
     * 18 apart) and page 27 entries at a time. Clicking a cell must pick the entry drawn there.
     */
    @GameTest(template = ARENA)
    public static void theGridPicksTheEntryUnderTheMouse(GameTestHelper helper) {
        List<ShopEntry> entries = new ArrayList<>();
        for (int i = 0; i < 30; i++) entries.add(new ShopEntry(new ItemStack(Items.BREAD, i + 1), 100, 5));
        BuyShopTab tab = new BuyShopTab(BuyShopTabType.ID, Component.literal("Buy"), entries);

        assertCount(helper, "pages for 30 entries", 2, tab.pageCount());
        assertCount(helper, "first cell, page 1", 0, tab.entryAt(0, 0, 61 + 8, 40 + 8, 0));
        assertCount(helper, "last cell of row 1", 8, tab.entryAt(0, 0, 61 + 8 * 18 + 8, 40 + 8, 0));
        assertCount(helper, "first cell of row 3", 18, tab.entryAt(0, 0, 61 + 8, 40 + 2 * 18 + 8, 0));
        assertCount(helper, "first cell, page 2", 27, tab.entryAt(0, 0, 61 + 8, 40 + 8, 1));
        assertCount(helper, "an empty cell on page 2", -1, tab.entryAt(0, 0, 61 + 5 * 18 + 8, 40 + 8, 1));
        assertCount(helper, "left of the grid", -1, tab.entryAt(0, 0, 50, 48, 0));
        assertCount(helper, "below the grid", -1, tab.entryAt(0, 0, 69, 40 + 3 * 18 + 4, 0));
        assertCount(helper, "offset by the screen's corner", 0, tab.entryAt(100, 50, 100 + 61 + 8, 50 + 40 + 8, 0));
        helper.succeed();
    }

    static Villager farmer(GameTestHelper helper) {
        Villager villager = helper.spawn(EntityType.VILLAGER, new Vec3(4.5, 1, 4.5));
        villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.FARMER));
        villager.setNoAi(true); // stands still, so distances in the test stay what they say
        return villager;
    }
}
