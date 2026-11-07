package com.example.casino;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Казино-фишка: ПКМ с фишкой в руке открывает меню казино. */
public class ChipItem extends Item {
    public ChipItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            MenuScreen.open(sp);
        }
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
    }
}
