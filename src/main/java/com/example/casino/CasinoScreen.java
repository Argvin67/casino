package com.example.casino;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Основа всех окон казино. Это обычное окно сундука, где предметы - кнопки.
 * Все перемещения предметов запрещены, клики обрабатываются на сервере.
 * Нижний ряд - панель ставки (-100/-10/-1, ставка, +1/+10/+100, баланс, назад).
 */
abstract class CasinoScreen extends GenericContainerScreenHandler {
    static final int MAX_BET = 1000;
    private static final int[] DELTAS = {-100, -10, -1, 0, 1, 10, 100};
    private static final Map<UUID, Integer> LAST_BET = new HashMap<>();

    protected final ServerPlayerEntity player;
    protected final SimpleInventory inv;
    protected final int rows;
    protected final boolean hasBar;
    protected int bet;
    /** true - идёт игра/анимация: панель ставки заблокирована. */
    protected boolean busy;
    protected boolean closed;

    interface ScreenFactory {
        CasinoScreen create(int syncId, PlayerInventory playerInventory);
    }

    protected CasinoScreen(int syncId, PlayerInventory pinv, SimpleInventory inv, int rows,
                           ServerPlayerEntity player, boolean hasBar) {
        super(typeFor(rows), syncId, pinv, inv, rows);
        this.inv = inv;
        this.rows = rows;
        this.player = player;
        this.hasBar = hasBar;
        this.bet = LAST_BET.getOrDefault(player.getUuid(), 10);
        for (int i = 0; i < rows * 9; i++) inv.setStack(i, Gui.filler());
        drawBar();
    }

    private static ScreenHandlerType<GenericContainerScreenHandler> typeFor(int rows) {
        switch (rows) {
            case 3: return ScreenHandlerType.GENERIC_9X3;
            case 4: return ScreenHandlerType.GENERIC_9X4;
            default: return ScreenHandlerType.GENERIC_9X6;
        }
    }

    static void openScreen(ServerPlayerEntity p, String title, ScreenFactory factory) {
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, pinv, pl) -> factory.create(syncId, pinv), Text.literal(title)));
    }

    /** Клик по кнопке игры (не по панели ставки). */
    protected abstract void onButton(int slot);

    /** Ставка изменилась - можно перерисовать кнопки. */
    protected void onBetChanged() {}

    // ---------- панель ставки ----------

    protected final void drawBar() {
        if (!hasBar) return;
        int base = (rows - 1) * 9;
        for (int i = 0; i <= 6; i++) {
            if (i == 3) continue;
            int d = DELTAS[i];
            inv.setStack(base + i, d < 0
                    ? Gui.item(Items.RED_STAINED_GLASS_PANE, String.valueOf(d), Formatting.RED, "Уменьшить ставку")
                    : Gui.item(Items.LIME_STAINED_GLASS_PANE, "+" + d, Formatting.GREEN, "Увеличить ставку"));
        }
        inv.setStack(base + 3, Gui.count(
                Gui.item(ModItems.CHIP, "Ставка: " + bet + " фишек", Formatting.GOLD, "Макс. ставка: " + MAX_BET), bet));
        inv.setStack(base + 7, Gui.item(Items.GOLD_INGOT,
                "Баланс: " + Economy.count(player) + " фишек", Formatting.GOLD));
        inv.setStack(base + 8, Gui.item(Items.ARROW, "Назад в меню", Formatting.WHITE));
    }

    private boolean handleBar(int slot) {
        if (!hasBar) return false;
        int base = (rows - 1) * 9;
        if (slot < base) return false;
        int c = slot - base;
        if (busy) return true;
        if (c == 8) {
            MenuScreen.open(player);
        } else if (c <= 6 && c != 3) {
            bet = Math.max(1, Math.min(MAX_BET, bet + DELTAS[c]));
            LAST_BET.put(player.getUuid(), bet);
            drawBar();
            onBetChanged();
        }
        return true;
    }

    protected final void announce(String text, Formatting color) {
        player.sendMessage(Text.literal(text).formatted(color), false);
        player.sendMessage(Text.literal(text).formatted(color), true);
    }

    // ---------- обработка кликов ----------

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity p) {
        if (actionType == SlotActionType.PICKUP && slotIndex >= 0 && slotIndex < rows * 9) {
            if (!handleBar(slotIndex)) onButton(slotIndex);
        }
        // Никакие предметы не двигаем - просто возвращаем клиенту актуальное состояние
        if (player.currentScreenHandler == this) syncState();
    }

    @Override
    public ItemStack quickMove(PlayerEntity p, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onClosed(PlayerEntity p) {
        closed = true;
        super.onClosed(p);
    }
}
