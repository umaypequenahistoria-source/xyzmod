package com.xyzmod;

import com.xyzmod.entity.XyzEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, XyzMod.MODID);

    public static final RegistryObject<EntityType<XyzEntity>> XYZ = ENTITIES.register("xyz",
            () -> EntityType.Builder.of(XyzEntity::new, MobCategory.MISC)
                    .sized(0.6F, 2.9F)          // as tall as an Enderman
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("xyz"));

    private ModEntities() {}
}
