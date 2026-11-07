package com.example.casino;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class Msg {
    private Msg() {}

    static void send(ServerPlayerEntity p, String text, Formatting color) {
        p.sendMessage(Text.literal(text).formatted(color), false);
    }
}
