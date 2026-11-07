package com.example.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

final class BlackjackScreen extends CasinoScreen {
    private static final Map<UUID, BlackjackScreen> ACTIVE = new HashMap<>();
    private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
    private static final String[] SUITS = {"\u2660", "\u2665", "\u2666", "\u2663"};

    private final List<Integer> deck = new ArrayList<>();
    private final List<Integer> pl = new ArrayList<>();
    private final List<Integer> dl = new ArrayList<>();
    private boolean inPlay;
    private int handBet;
    private int dealerShown;

    private BlackjackScreen(int syncId, PlayerInventory pinv, ServerPlayerEntity p) {
        super(syncId, pinv, new SimpleInventory(54), 6, p, true);
        setBanner("Блэкджек", Formatting.WHITE, "Выбери ставку и нажми РАЗДАТЬ", "Блэкджек платит 3:2");
        renderAll();
    }

    static void open(ServerPlayerEntity p) {
        openScreen(p, "Казино: Блэкджек", (id, pinv) -> new BlackjackScreen(id, pinv, p));
    }

    /** Игрок вышел посреди партии - возвращаем ставку. */
    static void abandon(ServerPlayerEntity p) {
        BlackjackScreen s = ACTIVE.remove(p.getUuid());
        if (s != null && s.inPlay) {
            s.inPlay = false;
            Economy.give(p, s.handBet);
        }
    }

    // ---------- карты ----------

    private int draw() {
        return deck.remove(deck.size() - 1);
    }

    private static int value(int card) {
        int r = card % 13;
        if (r == 0) return 11;
        return r >= 10 ? 10 : r + 1;
    }

    private static int total(List<Integer> hand) {
        int sum = 0, aces = 0;
        for (int c : hand) {
            sum += value(c);
            if (c % 13 == 0) aces++;
        }
        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces--;
        }
        return sum;
    }

    private static ItemStack card(int c) {
        int suit = c / 13;
        boolean red = suit == 1 || suit == 2;
        return Gui.item(Items.PAPER, RANKS[c % 13] + SUITS[suit], red ? Formatting.RED : Formatting.WHITE);
    }

    // ---------- отрисовка ----------

    private void setBanner(String text, Formatting color, String... lore) {
        inv.setStack(4, Gui.item(Items.BOOK, text, color, lore));
    }

    private void renderAll() {
        for (int i = 9; i <= 35; i++) inv.setStack(i, Gui.filler());

        for (int i = 0; i < dl.size() && i < 9; i++) {
            if (i < dealerShown) inv.setStack(9 + i, card(dl.get(i)));
            else if (i == 1 && dealerShown == 1) inv.setStack(10, Gui.item(Items.MAP, "??", Formatting.GRAY, "Закрытая карта"));
        }
        for (int i = 0; i < pl.size() && i < 9; i++) inv.setStack(27 + i, card(pl.get(i)));

        String dealerLabel = dl.isEmpty() ? "Дилер"
                : (dealerShown < 2 && inPlay) ? "Дилер: ?"
                : "Дилер: " + total(dl.subList(0, Math.min(dealerShown, dl.size())));
        inv.setStack(0, Gui.item(Items.NAME_TAG, dealerLabel, Formatting.GRAY));
        inv.setStack(36, Gui.item(Items.NAME_TAG, pl.isEmpty() ? "Ты" : "Ты: " + total(pl), Formatting.WHITE));

        if (inPlay) {
            inv.setStack(20, Gui.item(Items.LIME_CONCRETE, "ВЗЯТЬ", Formatting.GREEN, "Ещё одна карта"));
            inv.setStack(22, Gui.item(Items.RED_CONCRETE, "ХВАТИТ", Formatting.RED, "Остановиться"));
            inv.setStack(24, Gui.item(Items.YELLOW_CONCRETE, "УДВОИТЬ", Formatting.YELLOW,
                    "Ставка x2, одна карта и стоп", "Только на первых двух картах"));
        } else if (!busy) {
            inv.setStack(22, Gui.item(Items.LIME_CONCRETE, "РАЗДАТЬ", Formatting.GREEN, "Ставка: " + bet + " фишек"));
        } else {
            inv.setStack(22, Gui.item(Items.ORANGE_CONCRETE, "Дилер играет...", Formatting.GOLD));
        }
    }

    @Override
    protected void onBetChanged() {
        renderAll();
    }

    // ---------- игра ----------

    @Override
    protected void onButton(int slot) {
        if (inPlay) {
            if (slot == 20) hit();
            else if (slot == 22) stand();
            else if (slot == 24) doubleDown();
        } else if (!busy && slot == 22) {
            deal();
        }
    }

    private void deal() {
        if (!Economy.takeBet(player, bet)) return;
        handBet = bet;
        deck.clear();
        for (int i = 0; i < 52; i++) deck.add(i);
        Collections.shuffle(deck);
        pl.clear();
        dl.clear();
        pl.add(draw());
        dl.add(draw());
        pl.add(draw());
        dl.add(draw());
        dealerShown = 1;
        inPlay = true;
        busy = true;
        ACTIVE.put(player.getUuid(), this);
        drawBar();

        if (total(pl) == 21) { // блэкджек
            dealerShown = 2;
            boolean dealerBj = total(dl) == 21;
            if (dealerBj) {
                finish(handBet, "Ничья: у дилера тоже блэкджек", Formatting.YELLOW);
            } else {
                int payout = handBet + handBet * 3 / 2;
                finish(payout, "БЛЭКДЖЕК! Получено " + payout + " фишек", Formatting.GREEN);
            }
            return;
        }
        renderAll();
        setBanner("Твой ход", Formatting.AQUA, "Взять, остановиться или удвоить");
    }

    private void hit() {
        pl.add(draw());
        int t = total(pl);
        if (t > 21) {
            dealerShown = 2;
            finish(0, "Перебор! Потеряно " + handBet + " фишек", Formatting.RED);
        } else if (t == 21) {
            stand();
        } else {
            renderAll();
        }
    }

    private void doubleDown() {
        if (pl.size() != 2) {
            announce("Удвоить можно только на первых двух картах.", Formatting.RED);
            return;
        }
        if (!Economy.takeBet(player, handBet)) return;
        handBet *= 2;
        drawBar();
        pl.add(draw());
        if (total(pl) > 21) {
            dealerShown = 2;
            finish(0, "Перебор! Потеряно " + handBet + " фишек", Formatting.RED);
        } else {
            stand();
        }
    }

    private int payoutFor() {
        int pt = total(pl), dt = total(dl);
        if (dt > 21 || pt > dt) return handBet * 2;
        if (pt == dt) return handBet;
        return 0;
    }

    private void stand() {
        inPlay = false; // кнопки исчезают, дилер играет
        while (total(dl) < 17) dl.add(draw());
        renderAll();

        final int payout = payoutFor();
        final int hb = handBet;
        final String text;
        final Formatting color;
        if (payout > hb) {
            text = (total(dl) > 21 ? "Дилер перебрал! " : "Победа! ") + "Получено " + payout + " фишек";
            color = Formatting.GREEN;
        } else if (payout == hb) {
            text = "Ничья, ставка возвращена";
            color = Formatting.YELLOW;
        } else {
            text = "Дилер выиграл. Потеряно " + hb + " фишек";
            color = Formatting.RED;
        }

        int t = 8;
        for (int shown = 2; shown <= dl.size(); shown++) {
            final int sh = shown;
            Scheduler.later(player, t, () -> {
                dealerShown = sh;
                if (!closed) renderAll();
            });
            t += 10;
        }
        Scheduler.later(player, t, () -> finish(payout, text, color));
    }

    private void finish(int payout, String text, Formatting color) {
        inPlay = false;
        ACTIVE.remove(player.getUuid());
        if (payout > 0) Economy.give(player, payout);
        busy = false;
        renderAll();
        setBanner(text, color, "Нажми РАЗДАТЬ для новой партии");
        drawBar();
        announce(text + ".", color);
    }

    /** Окно закрыли посреди партии - считаем, что игрок остановился. */
    @Override
    public void onClosed(PlayerEntity p) {
        if (inPlay) {
            inPlay = false;
            ACTIVE.remove(player.getUuid());
            while (total(dl) < 17) dl.add(draw());
            int payout = payoutFor();
            if (payout > 0) Economy.give(player, payout);
            announce(payout > handBet ? "Партия завершена: победа (+" + payout + ")"
                    : payout == handBet ? "Партия завершена: ничья" : "Партия завершена: проигрыш",
                    Formatting.YELLOW);
        }
        super.onClosed(p);
    }
}
