package com.errantmc.markets.economy;

import com.errantmc.markets.domain.MoneyFormat;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /erranttrade money}: the money supply and what has created and destroyed it (economy §8.4). For operators. */
public final class MoneyCommand {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("erranttrade")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("money").executes(context -> report(context.getSource()))));
    }

    private static int report(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        MoneySupply supply = MoneySupply.measure(server);
        MoneyLedger ledger = MoneyLedger.get(server);
        String key = "commands.erranttrade.money.";
        source.sendSuccess(() -> Component.translatable(key + "header"), false);
        source.sendSuccess(() -> Component.translatable(key + "players", supply.players(),
                money(supply.loose()), money(supply.wallets())), false);
        source.sendSuccess(() -> Component.translatable(key + "villagers", supply.villagers(), money(supply.tills())), false);
        source.sendSuccess(() -> Component.translatable(key + "treasuries", supply.treasuries(), money(supply.treasury())), false);
        source.sendSuccess(() -> Component.translatable(key + "total", money(supply.total())), false);
        source.sendSuccess(() -> Component.translatable(key + "ledger", money(ledger.created()), money(ledger.destroyed()),
                money(ledger.createdToday(server)), money(ledger.destroyedToday(server))), false);
        // The total in whole dollars, for command blocks and scripts; capped, as a command result is an int
        return (int) Math.min(Integer.MAX_VALUE, supply.total() / 100);
    }

    private static String money(long copperBits) {
        return MoneyFormat.format(copperBits);
    }
}
