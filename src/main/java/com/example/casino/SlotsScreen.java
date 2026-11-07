package com.example.casino;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class SlotsScreen extends CasinoScreen {
    private static final String[] NAMES = {"Вишня", "Лимон", "Колокол", "Звезда", "Алмаз", "СЕМЁРКА"};
    private static final Item[] ITEMS = {Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.BELL,
            Items.NETHER_STAR, Items.DIAMOND, Items.ENCHANTED_GOLDEN_APPLE};
    private static final Formatting[] COLORS = {Formatting.RED, Formatting.YELLOW, Formatting.GOLD,
            Formatting.WHITE, Formatting.AQUA, Formatting.LIGHT_PURPLE};
    private static final int[] WEIGHTS = {30, 25, 20, 14, 8, 3};      // сумма = 100
    private static final int[] MULT = {6, 8, 12, 25, 50, 150};        // три одинаковых
    private static final int[] REELS = {11, 13, 15};

    private SlotsScreen(int syncId, PlayerInventory pinv, ServerPlayerEntity p) {
        super(syncId, pinv, new SimpleInventory(36), 4, p, true);
        inv.setStack(8, Gui.item(Items.BOOK, "Таблица выплат", Formatting.GOLD,
                "3 Вишни: x6", "3 Лимона: x8", "3 Колокола: x12", "3 Звезды: x25",
                "3 Алмаза: x50", "3 Семёрки: x150", "Пара: ставка возвращается"));
        setBanner("Слот-машина", Formatting.LIGHT_PURPLE, "Нажми КРУТИТЬ");
        for (int i = 0; i < 3; i++) inv.setStack(REELS[i], symbol(i * 2 % 6, false));
        renderButton();
    }

    static void open(ServerPlayerEntity p) {
        openScreen(p, "Казино: Слот-машина", (id, pinv) -> new SlotsScreen(id, pinv, p));
    }

    private void setBanner(String text, Formatting color, String... lore) {
        inv.setStack(4, Gui.item(Items.NAME_TAG, text, color, lore));
    }

    private static ItemStack symbol(int s, boolean glow) {
        ItemStack st = Gui.item(ITEMS[s], NAMES[s], COLORS[s]);
        return glow ? Gui.glint(st) : st;
    }

    private static int randomSymbol() {
        int r = ThreadLocalRandom.current().nextInt(100);
        int acc = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            acc += WEIGHTS[i];
            if (r < acc) return i;
        }
        return 0;
    }

    private void renderButton() {
        inv.setStack(22, Gui.item(Items.LIME_CONCRETE, "КРУТИТЬ", Formatting.GREEN, "Ставка: " + bet + " фишек"));
    }

    @Override
    protected void onBetChanged() {
        renderButton();
    }

    @Override
    protected void onButton(int slot) {
        if (!busy && slot == 22) spin();
    }

    private void spin() {
        if (!Economy.takeBet(player, bet)) return;
        busy = true;
        drawBar();
        inv.setStack(22, Gui.item(Items.ORANGE_CONCRETE, "Крутится...", Formatting.GOLD));
        setBanner("Барабаны вращаются...", Formatting.YELLOW);

        final int stake = bet;
        final int[] fin = {randomSymbol(), randomSymbol(), randomSymbol()};
        final int[] stop = {12, 18, 24};
        final boolean triple = fin[0] == fin[1] && fin[1] == fin[2];
        final boolean pair = !triple && (fin[0] == fin[1] || fin[1] == fin[2] || fin[0] == fin[2]);
        final int mult = triple ? MULT[fin[0]] : 0;
        final int payout = triple ? stake * mult : pair ? stake : 0;

        for (int f = 1; f <= 24; f++) {
            final int fr = f;
            Scheduler.later(player, fr * 2, () -> {
                if (closed) return;
                for (int i = 0; i < 3; i++) {
                    boolean stopped = fr >= stop[i];
                    inv.setStack(REELS[i], symbol(stopped ? fin[i] : randomSymbol(), false));
                }
            });
        }
        Scheduler.later(player, 54, () -> {
            if (payout > 0) Economy.give(player, payout);
            if (triple) {
                announce("ДЖЕКПОТ x" + mult + "! Получено " + payout + " фишек.", Formatting.GOLD);
                setBanner("ДЖЕКПОТ x" + mult + "!", Formatting.GOLD);
                for (int i = 0; i < 3; i++) inv.setStack(REELS[i], symbol(fin[i], true));
                if (mult >= 50 && player.getServer() != null) {
                    player.getServer().getPlayerManager().broadcast(
                            Text.literal(player.getName().getString() + " сорвал джекпот в слотах: +" + payout + " фишек!")
                                    .formatted(Formatting.GOLD), false);
                }
            } else if (pair) {
                announce("Пара - ставка возвращается.", Formatting.YELLOW);
                setBanner("Пара: ставка возвращена", Formatting.YELLOW);
            } else {
                announce("Мимо. Потеряно " + stake + " фишек.", Formatting.RED);
                setBanner("Мимо...", Formatting.RED);
            }
            busy = false;
            renderButton();
            drawBar();
        });
    }
}
