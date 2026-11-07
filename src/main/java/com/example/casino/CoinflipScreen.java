package com.example.casino;

import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

final class CoinflipScreen extends CasinoScreen {
    private boolean pickHeads = true;

    private CoinflipScreen(int syncId, PlayerInventory pinv, ServerPlayerEntity p) {
        super(syncId, pinv, new SimpleInventory(36), 4, p, true);
        inv.setStack(4, Gui.item(Items.BOOK, "Орёл или решка", Formatting.GOLD,
                "Выигрыш x2", "Выбери сторону и нажми ПОДБРОСИТЬ"));
        inv.setStack(13, coin(null));
        render();
    }

    static void open(ServerPlayerEntity p) {
        openScreen(p, "Казино: Монетка", (id, pinv) -> new CoinflipScreen(id, pinv, p));
    }

    private static ItemStack coin(Boolean heads) {
        if (heads == null) return Gui.item(Items.GOLD_NUGGET, "Монета", Formatting.YELLOW);
        return heads ? Gui.item(Items.GOLD_INGOT, "ОРЁЛ", Formatting.GOLD)
                     : Gui.item(Items.IRON_INGOT, "РЕШКА", Formatting.GRAY);
    }

    private ItemStack sideButton(boolean heads) {
        boolean selected = pickHeads == heads;
        ItemStack s = Gui.item(heads ? Items.GOLD_INGOT : Items.IRON_INGOT,
                heads ? "ОРЁЛ" : "РЕШКА", heads ? Formatting.GOLD : Formatting.GRAY,
                selected ? "ВЫБРАНО" : "Нажми, чтобы выбрать");
        return selected ? Gui.glint(s) : s;
    }

    private void render() {
        inv.setStack(10, sideButton(true));
        inv.setStack(16, sideButton(false));
        inv.setStack(22, Gui.item(Items.LIME_CONCRETE, "ПОДБРОСИТЬ", Formatting.GREEN,
                "Ставка: " + bet + " фишек", "Выплата: " + (bet * 2)));
    }

    @Override
    protected void onBetChanged() {
        render();
    }

    @Override
    protected void onButton(int slot) {
        if (busy) return;
        if (slot == 10) {
            pickHeads = true;
            render();
        } else if (slot == 16) {
            pickHeads = false;
            render();
        } else if (slot == 22) {
            flip();
        }
    }

    private void flip() {
        if (!Economy.takeBet(player, bet)) return;
        busy = true;
        drawBar();
        inv.setStack(22, Gui.item(Items.ORANGE_CONCRETE, "Монета в воздухе...", Formatting.GOLD));

        final int stake = bet;
        final boolean heads = ThreadLocalRandom.current().nextBoolean();
        final boolean win = heads == pickHeads;

        for (int f = 1; f <= 10; f++) {
            final boolean showHeads = f % 2 == 0;
            Scheduler.later(player, f * 3, () -> {
                if (!closed) inv.setStack(13, coin(showHeads));
            });
        }
        Scheduler.later(player, 36, () -> {
            if (!closed) inv.setStack(13, coin(heads));
            if (win) {
                Economy.give(player, stake * 2);
                announce((heads ? "Орёл" : "Решка") + "! Победа, получено " + (stake * 2) + " фишек.", Formatting.GREEN);
            } else {
                announce((heads ? "Орёл" : "Решка") + ". Потеряно " + stake + " фишек.", Formatting.RED);
            }
            busy = false;
            render();
            drawBar();
        });
    }
}
