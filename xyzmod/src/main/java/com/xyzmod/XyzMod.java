package com.xyzmod;

import com.xyzmod.entity.XyzEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(XyzMod.MODID)
public class XyzMod {
    public static final String MODID = "xyzmod";

    public XyzMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::attributes);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, XyzConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(XyzNet::register);
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.XYZ.get(), XyzEntity.createAttributes().build());
    }
}
