package com.example.casino;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    /** Казино-фишка: валюта для всех игр. */
    public static final Item CHIP = Registry.register(
            Registries.ITEM,
            Identifier.of(CasinoMod.MOD_ID, "chip"),
            new ChipItem(new Item.Settings()
                    .rarity(Rarity.UNCOMMON)
                    .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));

    public static final ItemGroup CASINO_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(CasinoMod.MOD_ID, "casino"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(CHIP))
                    .displayName(Text.translatable("itemGroup.casino"))
                    .entries((ctx, entries) -> entries.add(CHIP))
                    .build());

    private ModItems() {}

    /** Вызывается из onInitialize, чтобы статические поля точно инициализировались. */
    public static void register() {}
}
