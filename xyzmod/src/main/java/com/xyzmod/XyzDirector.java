package com.xyzmod;

import com.xyzmod.entity.XyzEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** The story engine: advances each player through the stages and reacts to what they do. */
@Mod.EventBusSubscriber(modid = XyzMod.MODID)
public final class XyzDirector {
    private static final Random R = new Random();

    // ------------------------------------------------------------------ speaking

    /** Xyz speaks: chat line + on-screen text. */
    public static void say(ServerPlayer p, String raw) {
        String t = raw.replace("%p", p.getName().getString());
        p.sendSystemMessage(Component.literal("<")
                .append(Component.literal("Xyz").withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal("> " + t)));
        XyzNet.toPlayer(p, new OverlayPacket(t, OverlayPacket.SPEECH, 70 + t.length() * 3));
    }

    /** Fake system line (looks like the game/server talking). Optionally mirrored as a glitching overlay. */
    public static void fake(ServerPlayer p, String raw, ChatFormatting color, boolean overlay) {
        String t = raw.replace("%p", p.getName().getString());
        p.sendSystemMessage(Component.literal(t).withStyle(color));
        if (overlay) XyzNet.toPlayer(p, new OverlayPacket(t, OverlayPacket.GLITCH, 100));
    }

    public static void queue(ServerPlayer p, int ticks, Consumer<ServerPlayer> action) {
        state(p).pending.add(new XyzData.Pending(ticks, action));
    }

    public static XyzData.PlayerState state(ServerPlayer p) {
        return XyzData.get(p.server).of(p.getUUID());
    }

    public static long threshold(int stage) {
        return switch (stage) {
            case 1 -> XyzConfig.STAGE1_MIN.get() * 1200L;
            case 2 -> XyzConfig.STAGE2_MIN.get() * 1200L;
            case 3 -> XyzConfig.STAGE3_MIN.get() * 1200L;
            default -> 0L;
        };
    }

    // ------------------------------------------------------------------ main loop

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        XyzData data = XyzData.get(p.server);
        XyzData.PlayerState st = data.of(p.getUUID());
        st.ticks++;

        // delayed actions (run after the loop so they may queue more)
        if (!st.pending.isEmpty()) {
            List<Consumer<ServerPlayer>> due = new ArrayList<>();
            for (Iterator<XyzData.Pending> it = st.pending.iterator(); it.hasNext(); ) {
                XyzData.Pending pd = it.next();
                if (--pd.delay <= 0) { due.add(pd.action); it.remove(); }
            }
            for (Consumer<ServerPlayer> a : due) a.accept(p);
        }

        if (st.ticks % 20 != 0 || !p.isAlive()) return; // everything below runs once per second

        // time-based escalation (stage never goes down by itself)
        int timeStage = 0;
        for (int s = 1; s <= 3; s++) if (st.ticks >= threshold(s)) timeStage = s;
        if (timeStage > st.stage) st.stage = timeStage;
        if (st.stage > st.announced) {
            st.announced = st.stage;
            XyzEvents.onStageReached(p, st, st.stage);
            data.setDirty();
        }
        if (st.anger > 0) st.anger--;

        // "something is wrong" events
        if (--st.eventCooldown <= 0) {
            XyzEvents.randomEvent(p, st);
            double base = XyzConfig.EVENT_INTERVAL_SEC.get();
            st.eventCooldown = Math.max(5, (int) (base * (0.6 + R.nextDouble() * 0.8) / (1 + st.stage * 0.5)));
        }

        // Xyz himself
        if (st.stage >= 1) {
            if (XyzEvents.findXyz(p) == null) {
                if (st.appearCooldown > 0) {
                    st.appearCooldown--;
                } else {
                    XyzEntity x = st.stage >= 3 ? XyzEvents.spawnXyz(p, 10, 20) : XyzEvents.spawnXyz(p, 18, 28);
                    st.appearCooldown = (x == null) ? 5 : 30;
                }
            } else if (st.stage >= 3) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, false));
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.getServer().getTickCount() % 200 == 0) {
            XyzData.get(e.getServer()).setDirty();
        }
    }

    // ------------------------------------------------------------------ reactions

    /** The player sets a spawn point on a bed: Xyz asks his question. */
    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        BlockPos np = e.getNewSpawn();
        if (np == null || e.isForced() || np.equals(p.getRespawnPosition())) return;

        XyzData.PlayerState st = state(p);
        final int n = ++st.bedSets;
        if (st.stage < 1) st.stage = 1;
        if (n >= 3 && st.stage < 2) st.stage = 2;

        queue(p, 20 * (6 + R.nextInt(14)), pl -> {
            XyzEntity x = XyzEvents.findXyz(pl);
            if (x == null) x = XyzEvents.spawnXyz(pl, 14, 22);
            if (x != null) x.setGreeted();
            say(pl, XyzDialogue.bedLine(n));
            if (n == 1) queue(pl, 20 * 9, p2 -> say(p2, XyzDialogue.BED_FOLLOWUP));
        });
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        XyzData.PlayerState st = state(p);
        st.deaths++;
        if (e.getSource().getEntity() instanceof XyzEntity) {
            st.killedByXyz = true;
            say(p, XyzDialogue.pick(XyzDialogue.KILL));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        XyzData.PlayerState st = state(p);
        if (!st.killedByXyz) return;
        st.killedByXyz = false;
        // he lets you breathe for a while, then it starts again
        st.stage = 2;
        st.announced = 2;
        st.ticks = Math.min(st.ticks, threshold(3) - 3 * 60 * 20L);
        st.appearCooldown = 60;
        queue(p, 100, pl -> say(pl, XyzDialogue.pick(XyzDialogue.RESPAWN)));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        XyzData.PlayerState st = state(p);
        st.sessions++;
        if (st.stage >= 1 && st.sessions > 1) {
            queue(p, 20 * 6, pl -> say(pl, XyzDialogue.pick(XyzDialogue.LOGIN)));
        }
    }

    private XyzDirector() {}
}
