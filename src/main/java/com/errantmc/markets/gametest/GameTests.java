package com.errantmc.markets.gametest;

import com.mojang.authlib.GameProfile;
import com.errantmc.markets.component.CurrencyData;
import com.errantmc.markets.item.CoinItem;
import com.errantmc.markets.item.TradeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.UUID;

/** Shared gametest constants and helpers. Templates live in {@code data/erranttrade/structure/}. */
public final class GameTests {
    private GameTests() {}

    /**
     * An empty 9×9×9 template, made by {@code tools/make-empty-template.py}. Build inside it: the
     * runner only loads the chunks a template covers.
     */
    public static final String ARENA = "arena";

    /**
     * A survival player in the test level, joined through the real login path.
     *
     * <p>Not {@code helper.makeMockServerPlayerInLevel()}: that player is creative, so inventories
     * are never charged, and its connection negotiated no mod channels, so the first mod to send it
     * a payload on login (Curios does) throws. {@code configureMockConnection} is NeoForge's hook
     * for exactly this.
     */
    public static ServerPlayer survivalPlayer(GameTestHelper helper) {
        return mockPlayer(helper, GameType.SURVIVAL);
    }

    public static ServerPlayer mockPlayer(GameTestHelper helper, GameType mode) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(mode);
        // Inside this test's own arena: every player otherwise joins at the world spawn, where
        // what one test drops is counted by the next
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        player.moveTo(at.x, at.y, at.z, 0, 0);
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
