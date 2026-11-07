package com.example.casino;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

/** Валюта казино - предмет "Казино-фишка" (ModItems.CHIP) из инвентаря. */
final class Economy {
    static final Item CURRENCY = ModItems.CHIP;
    static final String CURRENCY_NAME = "фишек";

    private Economy() {}

    static int count(ServerPlayerEntity p) {
        PlayerInventory inv = p.getInventory();
        int total = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(CURRENCY)) total += s.getCount();
        }
        return total;
    }

    /** Проверяет баланс и списывает ставку. */
    static boolean takeBet(ServerPlayerEntity p, int bet) {
        int have = count(p);
        if (have < bet) {
            Msg.send(p, "Не хватает " + CURRENCY_NAME + ": нужно " + bet + ", у тебя " + have + ".", Formatting.RED);
            return false;
        }
        remove(p, bet);
        return true;
    }

    static void remove(ServerPlayerEntity p, int amount) {
        PlayerInventory inv = p.getInventory();
        int left = amount;
        for (int i = 0; i < inv.size() && left > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(CURRENCY)) {
                int n = Math.min(left, s.getCount());
                s.decrement(n);
                left -= n;
            }
        }
    }

    static void give(ServerPlayerEntity p, int amount) {
        int left = amount;
        while (left > 0) {
            int n = Math.min(left, CURRENCY.getMaxCount());
            ItemStack stack = new ItemStack(CURRENCY, n);
            p.getInventory().insertStack(stack);
            if (!stack.isEmpty()) {
                p.dropItem(stack, false);
            }
            left -= n;
        }
    }
}
