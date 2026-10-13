package com.xyzmod.client;

import com.xyzmod.ModEntities;
import com.xyzmod.XyzMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ClientSetup {

    @Mod.EventBusSubscriber(modid = XyzMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus {
        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModEntities.XYZ.get(), XyzRenderer::new);
        }

        @SubscribeEvent
        public static void overlays(RegisterGuiOverlaysEvent e) {
            e.registerAboveAll("xyz_overlay", XyzOverlay.OVERLAY);
        }
    }

    @Mod.EventBusSubscriber(modid = XyzMod.MODID, value = Dist.CLIENT)
    public static class ForgeBus {
        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent e) {
            if (e.phase == TickEvent.Phase.END) XyzOverlay.tick();
        }
    }

    private ClientSetup() {}
}
