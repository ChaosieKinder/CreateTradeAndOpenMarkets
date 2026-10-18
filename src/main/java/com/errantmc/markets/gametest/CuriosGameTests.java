package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.compat.CuriosCompat;
import com.errantmc.markets.component.WalletBalance;
import com.errantmc.markets.data.CoinDenomination;
import com.errantmc.markets.item.TradeItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import static com.errantmc.markets.gametest.GameTests.*;

/**
 * Curios integration. Each test passes trivially in a run without Curios ({@code -PnoCurios}); the
 * Curios calls sit after the check, so the JVM never links them there.
 */
@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CuriosGameTests {
    private CuriosGameTests() {}

    private static final String SLOT = "wallet";

    @GameTest(template = ARENA)
    public static void playersHaveAWalletSlot(GameTestHelper helper) {
        if (!CuriosCompat.isLoaded()) { helper.succeed(); return; }
        ServerPlayer player = survivalPlayer(helper);

        assertTrue(helper, "players to have a '" + SLOT + "' curio slot",
                CuriosApi.getPlayerSlots(player).containsKey(SLOT));
        helper.succeed();
    }

    /** README: the wallet "works in inventory or Curios slots", so Curios must accept it there. */
    @GameTest(template = ARENA)
    public static void theWalletFitsTheWalletSlot(GameTestHelper helper) {
        if (!CuriosCompat.isLoaded()) { helper.succeed(); return; }
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());

        var slots = CuriosApi.getItemStackSlots(wallet, player);
        assertTrue(helper, "the wallet to be valid in the '" + SLOT + "' slot, but it fits " + slots.keySet(),
                slots.containsKey(SLOT));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void pickedUpCoinsGoIntoAWalletWornAsACurio(GameTestHelper helper) {
        if (!CuriosCompat.isLoaded()) { helper.succeed(); return; }
        ServerPlayer player = survivalPlayer(helper);
        ItemStack wallet = new ItemStack(TradeItems.LEATHER_WALLET.get());
        var inventory = CuriosApi.getCuriosInventory(player);
        if (inventory.isEmpty()) { helper.fail("player has no curios inventory"); return; }
        inventory.get().setEquippedCurio(SLOT, 0, wallet);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        ItemEntity coins = new ItemEntity(helper.getLevel(), at.x, at.y, at.z,
                new ItemStack(TradeItems.COIN_GOLD_REGAL.get(), 3));
        coins.setNoPickUpDelay();
        helper.getLevel().addFreshEntity(coins);

        coins.playerTouch(player);

        ItemStack worn = inventory.get().getStacksHandler(SLOT).orElseThrow().getStacks().getStackInSlot(0);
        assertCount(helper, "gold regals in the worn wallet", 3,
                WalletBalance.get(worn).getFrom(CoinDenomination.GOLD_REGAL));
        assertCount(helper, "coins in the inventory", 0, coinsIn(player));
        helper.succeed();
    }
}
