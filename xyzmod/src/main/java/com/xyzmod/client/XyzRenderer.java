package com.xyzmod.client;

import com.xyzmod.XyzMod;
import com.xyzmod.entity.XyzEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class XyzRenderer extends GeoEntityRenderer<XyzEntity> {
    public XyzRenderer(EntityRendererProvider.Context ctx) {
        // geo/entity/xyz.geo.json, textures/entity/xyz.png, animations/entity/xyz.animation.json
        super(ctx, new DefaultedEntityGeoModel<>(new ResourceLocation(XyzMod.MODID, "xyz"), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this)); // textures/entity/xyz_glowmask.png -> glowing red eyes
        this.shadowRadius = 0.0F;
    }
}
