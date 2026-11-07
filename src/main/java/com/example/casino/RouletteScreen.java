package com.example.casino;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

final class RouletteScreen extends CasinoScreen {
    private static final Set<Integer> RED = Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
    private static final int[] OUTSIDE_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final String[] KEYS = {"red", "black", "even", "odd", "low", "high", "green"};
    private static final String[] LABELS = {"Красное", "Чёрное", "Чёт", "Нечёт", "1-18", "19-36", "Зеро (0)"};
    private static final int[] PAY = {2, 2, 2, 2, 2, 2, 36};
    private static final Item[] ICONS = {Items.RED_WOOL, Items.BLACK_WOOL, Items.BLUE_CONCRETE,
            Items.ORANGE_CONCRETE, Items.WHITE_CONCRETE, Items.YELLOW_CONCRETE, Items.LIME_WOOL};

    private String choice = "red";
    private int number = 17;

    private RouletteScreen(int syncId, PlayerInventory pinv, ServerPlayerEntity p) {
        super(syncId, pinv, new SimpleInventory(54), 6, p, true);
        inv.setStack(4, Gui.item(Items.CLOCK, "Рулетка", Formatting.RED, "Выбери ставку и нажми КРУТИТЬ"));
        render();
    }

    static void open(ServerPlayerEntity p) {
        openScreen(p, "Казино: Рулетка", (id, pinv) -> new RouletteScreen(id, pinv, p));
    }

    private static ItemStack numStack(int n, String prefix, String... lore) {
        Item it = n == 0 ? Items.LIME_WOOL : RED.contains(n) ? Items.RED_WOOL : Items.BLACK_WOOL;
        Formatting col = n == 0 ? Formatting.GREEN : RED.contains(n) ? Formatting.RED : Formatting.GRAY;
        return Gui.count(Gui.item(it, prefix + n, col, lore), n);
    }

    private String describe() {
        for (int i = 0; i < KEYS.length; i++) if (choice.equals(KEYS[i])) return LABELS[i] + " (x" + PAY[i] + ")";
        return "Число " + number + " (x36)";
    }

    private void render() {
        for (int i = 0; i < 7; i++) {
            boolean sel = choice.equals(KEYS[i]);
            ItemStack s = Gui.item(ICONS[i], LABELS[i], Formatting.WHITE,
                    "Выплата x" + PAY[i], sel ? "ВЫБРАНО" : "Нажми, чтобы выбрать");
            inv.setStack(OUTSIDE_SLOTS[i], sel ? Gui.glint(s) : s);
        }
        inv.setStack(19, Gui.item(Items.RED_STAINED_GLASS_PANE, "-10", Formatting.RED, "Число на столе"));
        inv.setStack(20, Gui.item(Items.RED_STAINED_GLASS_PANE, "-1", Formatting.RED, "Число на столе"));
        inv.setStack(24, Gui.item(Items.LIME_STAINED_GLASS_PANE, "+1", Formatting.GREEN, "Число на столе"));
        inv.setStack(25, Gui.item(Items.LIME_STAINED_GLASS_PANE, "+10", Formatting.GREEN, "Число на столе"));
        boolean numSel = choice.equals("num");
        ItemStack ns = numStack(number, "Число: ", "Выплата x36", numSel ? "ВЫБРАНО" : "Нажми, чтобы выбрать");
        inv.setStack(22, numSel ? Gui.glint(ns) : ns);
        inv.setStack(31, Gui.item(Items.LIME_CONCRETE, "КРУТИТЬ", Formatting.GREEN, "Ставка: " + bet + " фишек"));
        inv.setStack(40, Gui.item(Items.PAPER, "Ставка на: " + describe(), Formatting.GOLD));
    }

    @Override
    protected void onBetChanged() {
        render();
    }

    private void shift(int d) {
        number = Math.floorMod(number + d, 37);
        choice = "num";
        render();
    }

    @Override
    protected void onButton(int slot) {
        if (busy) return;
        for (int i = 0; i < 7; i++) {
            if (slot == OUTSIDE_SLOTS[i]) {
                choice = KEYS[i];
                render();
                return;
            }
        }
        switch (slot) {
            case 19 -> shift(-10);
            case 20 -> shift(-1);
            case 22 -> { choice = "num"; render(); }
            case 24 -> shift(1);
            case 25 -> shift(10);
            case 31 -> spin();
            default -> {}
        }
    }

    private static int multiplier(String c, int num, int n) {
        switch (c) {
            case "red":   return n != 0 && RED.contains(n) ? 2 : 0;
            case "black": return n != 0 && !RED.contains(n) ? 2 : 0;
            case "even":  return n != 0 && n % 2 == 0 ? 2 : 0;
            case "odd":   return n % 2 == 1 ? 2 : 0;
            case "low":   return n >= 1 && n <= 18 ? 2 : 0;
            case "high":  return n >= 19 ? 2 : 0;
            case "green": return n == 0 ? 36 : 0;
            case "num":   return n == num ? 36 : 0;
            default:      return 0;
        }
    }

    private void spin() {
        if (!Economy.takeBet(player, bet)) return;
        busy = true;
        drawBar();
        inv.setStack(31, Gui.item(Items.ORANGE_CONCRETE, "Крутится...", Formatting.GOLD));

        final int stake = bet;
        final int n = ThreadLocalRandom.current().nextInt(37);
        final int mult = multiplier(choice, number, n);
        final String colorWord = n == 0 ? "зелёное" : RED.contains(n) ? "красное" : "чёрное";

        int lastT = 0;
        for (int k = 0; k < 20; k++) {
            final boolean last = k == 19;
            int t = 2 + 2 * k + (k * k) / 6;
            lastT = t;
            Scheduler.later(player, t, () -> {
                if (closed) return;
                int shown = last ? n : ThreadLocalRandom.current().nextInt(37);
                inv.setStack(4, numStack(shown, last ? "Выпало: " : ""));
            });
        }
        Scheduler.later(player, lastT + 4, () -> {
            if (mult > 0) {
                int payout = stake * mult;
                Economy.give(player, payout);
                announce("Выпало " + n + " (" + colorWord + "). Победа! Получено " + payout + " фишек.", Formatting.GREEN);
            } else {
                announce("Выпало " + n + " (" + colorWord + "). Потеряно " + stake + " фишек.", Formatting.RED);
            }
            busy = false;
            render();
            drawBar();
        });
    }
}
