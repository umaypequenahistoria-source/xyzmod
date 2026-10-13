package com.xyzmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class XyzNet {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(XyzMod.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, OverlayPacket.class, OverlayPacket::encode, OverlayPacket::decode, OverlayPacket::handle);
    }

    public static void toPlayer(ServerPlayer p, OverlayPacket pkt) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
    }

    private XyzNet() {}
}
