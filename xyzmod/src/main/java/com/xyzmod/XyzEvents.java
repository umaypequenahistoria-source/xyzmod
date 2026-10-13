package com.xyzmod;

import com.xyzmod.entity.XyzEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** The "something is wrong" toolbox: sounds, doors, torches, darkness, fake errors, spawning Xyz. */
public final class XyzEvents {
    private static final Random R = new Random();

    // ------------------------------------------------------------------ helpers

    public static void play(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.AMBIENT, volume, pitch);
    }

    /** A spot where something 3 blocks tall can stand, near the given column. */
    public static BlockPos findStand(ServerLevel level, double x, double z, int aroundY) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        if (!level.hasChunkAt(new BlockPos(bx, aroundY, bz))) return null;
        for (int dy = 4; dy >= -8; dy--) {
            BlockPos feet = new BlockPos(bx, aroundY + dy, bz);
            BlockPos below = feet.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            boolean clear = true;
            for (int h = 0; h < 3 && clear; h++) {
                BlockPos c = feet.above(h);
                BlockState s = level.getBlockState(c);
                clear = s.getCollisionShape(level, c).isEmpty() && s.getFluidState().isEmpty();
            }
            if (clear) return feet;
        }
        return null;
    }

    public static XyzEntity findXyz(ServerPlayer p) {
        List<? extends XyzEntity> list = p.serverLevel().getEntities(
                EntityTypeTest.forClass(XyzEntity.class), e -> p.getUUID().equals(e.getTargetUUID()));
        return list.isEmpty() ? null : list.get(0);
    }

    public static XyzEntity spawnXyz(ServerPlayer p, double minDist, double maxDist) {
        ServerLevel level = p.serverLevel();
        for (int i = 0; i < 12; i++) {
            double ang = R.nextDouble() * Math.PI * 2;
            double d = minDist + R.nextDouble() * (maxDist - minDist);
            BlockPos pos = findStand(level, p.getX() + Math.cos(ang) * d, p.getZ() + Math.sin(ang) * d, p.getBlockY());
            if (pos == null) continue;
            XyzEntity x = ModEntities.XYZ.get().create(level);
            if (x == null) return null;
            x.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, R.nextFloat() * 360F, 0F);
            x.setTargetUUID(p.getUUID());
            level.addFreshEntity(x);
            return x;
        }
        return null;
    }

    private static Vec3 around(ServerPlayer p, double dist) {
        double a = R.nextDouble() * Math.PI * 2;
        return new Vec3(p.getX() + Math.cos(a) * dist, p.getY(), p.getZ() + Math.sin(a) * dist);
    }

    // ------------------------------------------------------------------ ambient events

    public static void whisper(ServerPlayer p) {
        SoundEvent s = R.nextBoolean() ? SoundEvents.ENDERMAN_STARE : SoundEvents.WARDEN_NEARBY_CLOSE;
        play(p.serverLevel(), around(p, 5 + R.nextInt(5)), s, 0.6F, 0.5F + R.nextFloat() * 0.3F);
    }

    /** Footsteps that approach from behind and stop abruptly. */
    public static void footsteps(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z).normalize().yRot((float) ((R.nextDouble() - 0.5) * 0.8));
        for (int i = 0; i < 6; i++) {
            final double dist = 10 - i * 1.4;
            XyzDirector.queue(p, i * 9, pl -> {
                Vec3 pos = pl.position().add(back.scale(dist));
                SoundEvent step = pl.getBlockStateOn().getSoundType().getStepSound();
                play(pl.serverLevel(), pos, step, 0.9F, 0.7F);
            });
        }
    }

    public static void heartbeat(ServerPlayer p) {
        for (int i = 0; i < 4; i++) {
            XyzDirector.queue(p, i * 25, pl -> play(pl.serverLevel(), pl.position(), SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F));
        }
    }

    public static void door(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos c = p.blockPosition();
        List<BlockPos> doors = BlockPos.betweenClosedStream(c.offset(-8, -3, -8), c.offset(8, 3, 8))
                .filter(b -> {
                    BlockState s = level.getBlockState(b);
                    return s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER;
                })
                .map(BlockPos::immutable).toList();
        if (doors.isEmpty()) { whisper(p); return; }
        BlockPos b = doors.get(R.nextInt(doors.size()));
        BlockState s = level.getBlockState(b);
        ((DoorBlock) s.getBlock()).setOpen(null, level, s, b, !s.getValue(DoorBlock.OPEN));
    }

    /** A nearby torch simply goes out. */
    public static void torch(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos c = p.blockPosition();
        List<BlockPos> torches = BlockPos.betweenClosedStream(c.offset(-10, -4, -10), c.offset(10, 4, 10))
                .filter(b -> {
                    Block bl = level.getBlockState(b).getBlock();
                    return bl == Blocks.TORCH || bl == Blocks.WALL_TORCH;
                })
                .map(BlockPos::immutable).toList();
        if (torches.isEmpty()) { whisper(p); return; }
        BlockPos b = torches.get(R.nextInt(torches.size()));
        level.removeBlock(b, false);
        level.playSound(null, b, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.sendParticles(ParticleTypes.SMOKE, b.getX() + 0.5, b.getY() + 0.6, b.getZ() + 0.5, 8, 0.1, 0.1, 0.1, 0.01);
    }

    public static void darkPulse(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0, false, false, false));
    }

    public static void glitchMessage(ServerPlayer p) {
        XyzDirector.fake(p, XyzDialogue.pick(XyzDialogue.FAKE_ERRORS), ChatFormatting.RED, true);
    }

    public static void randomEvent(ServerPlayer p, XyzData.PlayerState st) {
        List<Consumer<ServerPlayer>> pool = new ArrayList<>();
        pool.add(XyzEvents::whisper);
        pool.add(XyzEvents::footsteps);
        pool.add(XyzEvents::door);
        if (st.stage >= 1) { pool.add(XyzEvents::torch); pool.add(XyzEvents::heartbeat); }
        if (st.stage >= 2) {
            pool.add(XyzEvents::darkPulse);
            pool.add(XyzEvents::glitchMessage);
            pool.add(XyzEvents::glitchMessage);
            pool.add(XyzEvents::torch);
        }
        if (st.stage >= 3) { pool.add(XyzEvents::darkPulse); pool.add(XyzEvents::footsteps); }
        pool.get(R.nextInt(pool.size())).accept(p);
    }

    // ------------------------------------------------------------------ stage transitions

    public static void onStageReached(ServerPlayer p, XyzData.PlayerState st, int stage) {
        ServerLevel level = p.serverLevel();
        switch (stage) {
            case 1 -> {
                XyzDirector.fake(p, "Xyz joined the game", ChatFormatting.YELLOW, false);
                whisper(p);
            }
            case 2 -> {
                XyzDirector.fake(p, "[Xyz] Error: entity 'xyz' is flagged for deletion. Retrying...", ChatFormatting.RED, true);
                darkPulse(p);
                if (level.dimension() == Level.OVERWORLD) level.setWeatherParameters(0, 12000, true, false);
                XyzDirector.queue(p, 80, pl -> XyzDirector.say(pl, "Not yet. Not yet. Not yet."));
            }
            case 3 -> {
                if (level.dimension() == Level.OVERWORLD) {
                    level.setDayTime((level.getDayTime() / 24000L) * 24000L + 14000L);
                    level.setWeatherParameters(0, 12000, true, true);
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(Vec3.atBottomCenterOf(p.blockPosition().offset(24, 0, 0)));
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                darkPulse(p);
                st.appearCooldown = 0;
                XyzDirector.queue(p, 40, pl -> XyzDirector.say(pl, "I'm sorry, %p. I can't let you leave."));
            }
            default -> { }
        }
    }

    private XyzEvents() {}
}
