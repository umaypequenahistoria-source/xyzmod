package com.xyzmod;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Test helpers (op only): /xyz stage <0-3>, /xyz summon, /xyz event, /xyz reset */
@Mod.EventBusSubscriber(modid = XyzMod.MODID)
public final class XyzCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("xyz")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("stage")
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 3)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int n = IntegerArgumentType.getInteger(ctx, "n");
                            XyzData.PlayerState st = XyzDirector.state(p);
                            st.stage = n;
                            st.ticks = XyzDirector.threshold(n);
                            if (n < st.announced) st.announced = n;
                            ctx.getSource().sendSuccess(() -> Component.literal("Xyz stage set to " + n), false);
                            return 1;
                        })))
                .then(Commands.literal("summon").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (XyzEvents.findXyz(p) != null) {
                        ctx.getSource().sendFailure(Component.literal("Xyz is already here."));
                        return 0;
                    }
                    XyzEvents.spawnXyz(p, 12, 18);
                    return 1;
                }))
                .then(Commands.literal("event").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    XyzEvents.randomEvent(p, XyzDirector.state(p));
                    return 1;
                }))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    XyzData.get(p.server).reset(p.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal("Xyz progress reset"), false);
                    return 1;
                })));
    }

    private XyzCommands() {}
}
