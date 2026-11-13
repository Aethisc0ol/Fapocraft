package com.fapcraftport.sexmod.registry;

import com.fapcraftport.sexmod.SexMod;
import com.fapcraftport.sexmod.entity.JennyEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, SexMod.MODID);

    // Hitbox taken from the original (setSize(0.49F, 1.95F)).
    public static final DeferredHolder<EntityType<?>, EntityType<JennyEntity>> JENNY = ENTITIES.register("jenny",
            () -> EntityType.Builder.of(JennyEntity::new, MobCategory.CREATURE)
                    .sized(0.49F, 1.95F)
                    .eyeHeight(1.75F)
                    .clientTrackingRange(10)
                    .build("jenny"));

    private ModEntities() {}
}
