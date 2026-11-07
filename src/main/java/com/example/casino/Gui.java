package com.example.casino;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Помощник для создания "кнопок" - предметов с названием и описанием. */
final class Gui {
    private Gui() {}

    static ItemStack item(Item item, String name, Formatting color, String... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).setStyle(Style.EMPTY.withColor(color).withItalic(false)));
        if (lore.length > 0) {
            List<Text> lines = new ArrayList<>();
            for (String l : lore) {
                lines.add(Text.literal(l).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false)));
            }
            s.set(DataComponentTypes.LORE, new LoreComponent(lines));
        }
        return s;
    }

    static ItemStack glint(ItemStack s) {
        s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    static ItemStack count(ItemStack s, int n) {
        s.setCount(Math.max(1, Math.min(n, s.getMaxCount())));
        return s;
    }

    static ItemStack filler() {
        return item(Items.GRAY_STAINED_GLASS_PANE, " ", Formatting.GRAY);
    }
}
