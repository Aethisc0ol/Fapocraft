package com.fapcraftport.sexmod.registry;

import com.fapcraftport.sexmod.SexMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SexMod.MODID);

    public static final DeferredItem<DeferredSpawnEggItem> JENNY_SPAWN_EGG = ITEMS.register("jenny_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.JENNY, 0x3B2A4D, 0xE8B4C8, new Item.Properties()));

    private ModItems() {}
}
