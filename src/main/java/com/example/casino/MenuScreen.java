package com.example.casino;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

final class MenuScreen extends CasinoScreen {
    private MenuScreen(int syncId, PlayerInventory pinv, ServerPlayerEntity p) {
        super(syncId, pinv, new SimpleInventory(27), 3, p, false);
        inv.setStack(10, Gui.item(Items.GOLD_INGOT, "Монетка", Formatting.GOLD, "Орёл или решка", "Выплата x2"));
        inv.setStack(12, Gui.item(Items.NETHER_STAR, "Слот-машина", Formatting.LIGHT_PURPLE, "Три барабана", "Выплаты до x150"));
        inv.setStack(14, Gui.item(Items.RED_WOOL, "Рулетка", Formatting.RED, "Европейская, 0-36", "Числа до x36"));
        inv.setStack(16, Gui.item(Items.PAPER, "Блэкджек", Formatting.WHITE, "Против дилера", "Блэкджек платит 3:2"));
        inv.setStack(22, Gui.item(ModItems.CHIP, "Баланс: " + Economy.count(p) + " фишек", Formatting.GOLD,
                "1 изумруд = 9 фишек (крафт)"));
    }

    static void open(ServerPlayerEntity p) {
        openScreen(p, "Казино", (id, pinv) -> new MenuScreen(id, pinv, p));
    }

    @Override
    protected void onButton(int slot) {
        switch (slot) {
            case 10 -> CoinflipScreen.open(player);
            case 12 -> SlotsScreen.open(player);
            case 14 -> RouletteScreen.open(player);
            case 16 -> BlackjackScreen.open(player);
            default -> {}
        }
    }
}
