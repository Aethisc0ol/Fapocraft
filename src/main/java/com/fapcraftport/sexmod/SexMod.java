package com.fapcraftport.sexmod;

import com.fapcraftport.sexmod.config.ClientConfig;
import com.fapcraftport.sexmod.entity.JennyEntity;
import com.fapcraftport.sexmod.network.ModNetwork;
import com.fapcraftport.sexmod.registry.ModEntities;
import com.fapcraftport.sexmod.registry.ModItems;
import com.fapcraftport.sexmod.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SexMod.MODID)
public final class SexMod {
    public static final String MODID = "sexmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public SexMod(IEventBus modBus, ModContainer container) {
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUNDS.register(modBus);

        modBus.addListener(this::onAttributes);
        modBus.addListener(this::onCreativeTabs);
        modBus.addListener(ModNetwork::register);

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.JENNY.get(), JennyEntity.createAttributes().build());
    }

    private void onCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.JENNY_SPAWN_EGG.get());
        }
    }
}
