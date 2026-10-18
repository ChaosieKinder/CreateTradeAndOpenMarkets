package com.errantmc.markets.gametest;

import com.errantmc.markets.component.CurrencyData;
import com.errantmc.markets.item.CoinItem;
import com.errantmc.markets.item.TradeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared gametest constants and helpers. Templates live in {@code data/erranttrade/structure/}. */
public final class GameTests {
    private GameTests() {}

    /**
     * An empty 9×9×9 template, made by {@code tools/make-empty-template.py}. Build inside it: the
     * runner only loads the chunks a template covers.
     */
    public static final String ARENA = "arena";

    /** The helper's mock player is creative, and creative inventories are never charged. */
    public static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** A click on {@code face} of the block at relative {@code pos}, in the absolute coordinates useBlock wants. */
    public static BlockHitResult hitFace(GameTestHelper helper, BlockPos pos, Direction face) {
        BlockPos abs = helper.absolutePos(pos);
        Vec3 at = Vec3.atCenterOf(abs).relative(face, 0.5);
        return new BlockHitResult(at, face, abs, false);
    }

    public static ItemStack coinStackOf(CurrencyData currency) {
        ItemStack stack = new ItemStack(TradeItems.COIN_STACK_ITEM.get());
        CurrencyData.set(stack, currency);
        return stack;
    }

    public static int coinsIn(ServerPlayer player) {
        var inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (CoinItem.isCoinItem(s.getItem())) total += s.getCount();
        }
        return total;
    }

    public static void assertCount(GameTestHelper helper, String what, long expected, long actual) {
        if (expected != actual) helper.fail("expected " + expected + " " + what + ", found " + actual);
    }

    public static void assertTrue(GameTestHelper helper, String what, boolean condition) {
        if (!condition) helper.fail("expected " + what);
    }
}
