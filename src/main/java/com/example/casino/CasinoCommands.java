package com.example.casino;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

public final class CasinoCommands {
    private CasinoCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("casino")
                .executes(ctx -> {
                    MenuScreen.open(ctx.getSource().getPlayerOrThrow());
                    return 1;
                })
                .then(CommandManager.literal("balance").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    Msg.send(p, "У тебя " + Economy.count(p) + " " + Economy.CURRENCY_NAME + ".", Formatting.GOLD);
                    return 1;
                }))
                .then(CommandManager.literal("coinflip").executes(ctx -> {
                    CoinflipScreen.open(ctx.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(CommandManager.literal("slots").executes(ctx -> {
                    SlotsScreen.open(ctx.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(CommandManager.literal("roulette").executes(ctx -> {
                    RouletteScreen.open(ctx.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(CommandManager.literal("blackjack").executes(ctx -> {
                    BlackjackScreen.open(ctx.getSource().getPlayerOrThrow());
                    return 1;
                })));
    }
}
