package com.xyzmod;

import com.xyzmod.client.XyzOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: show text on screen (Xyz's voice, or a fake glitch/error line). */
public record OverlayPacket(String text, int type, int ticks) {
    public static final int SPEECH = 0;
    public static final int GLITCH = 1;

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(text, 512);
        buf.writeVarInt(type);
        buf.writeVarInt(ticks);
    }

    public static OverlayPacket decode(FriendlyByteBuf buf) {
        return new OverlayPacket(buf.readUtf(512), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(OverlayPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> XyzOverlay.show(pkt.text(), pkt.type(), pkt.ticks())));
        ctx.get().setPacketHandled(true);
    }
}
