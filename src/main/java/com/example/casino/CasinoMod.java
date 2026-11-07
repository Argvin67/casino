package com.example.casino;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasinoMod implements ModInitializer {
    public static final String MOD_ID = "casino";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.register();

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> CasinoCommands.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(server -> Scheduler.tick());

        // При выходе: доигрываем отложенные выплаты и возвращаем ставку за незаконченный блэкджек
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Scheduler.flush(handler.player.getUuid());
            BlackjackScreen.abandon(handler.player);
        });
    }
}
