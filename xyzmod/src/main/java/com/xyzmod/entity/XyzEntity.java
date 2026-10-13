package com.xyzmod.entity;

import com.xyzmod.XyzData;
import com.xyzmod.XyzDialogue;
import com.xyzmod.XyzDirector;
import com.xyzmod.XyzEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Xyz. Tall, black, red-eyed. Bound to one player (targetUUID).
 * Behaviour depends on that player's story stage: watch (0-1) -> stalk (2) -> hunt (3).
 * Only players can hurt him, and he can't die: at low health he heals and gets angry.
 */
public class XyzEntity extends PathfinderMob implements GeoEntity {
    private static final Random R = new Random();
    private static final EntityDataAccessor<Boolean> HOSTILE =
            SynchedEntityData.defineId(XyzEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.xyz.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.xyz.walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("animation.xyz.run");
    private static final RawAnimation ATTACK = RawAnimation.begin().then("animation.xyz.attack", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID targetUUID;
    private boolean greeted;
    private int lifeTicks, lookTicks, speechCooldown = 40, attackCooldown, tpCooldown = 200;

    public XyzEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)   // tune: hunt speed = this * 1.15
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HOSTILE, false);
    }

    public void setTargetUUID(UUID id) { this.targetUUID = id; }
    public UUID getTargetUUID() { return targetUUID; }
    public void setGreeted() { this.greeted = true; }

    @Override public boolean shouldBeSaved() { return false; }              // the director respawns him
    @Override public boolean removeWhenFarAway(double dist) { return false; }

    // ------------------------------------------------------------------ damage rules

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (level().isClientSide) return false;
        // Nothing but a player can hurt him (no mobs, fall, fire, void, /kill...)
        if (!(src.getEntity() instanceof ServerPlayer p)) return false;

        boolean result = super.hurt(src, amount);
        XyzData.PlayerState st = XyzDirector.state(p);

        if (result) {
            st.anger += 10;
            if (st.anger >= 35 && st.stage < 3) {
                st.stage = 3;                       // too violent: he snaps
                speechCooldown = 60;
                XyzDirector.say(p, XyzDialogue.pick(XyzDialogue.RAGE));
            } else if (speechCooldown <= 0) {
                String[] pool = st.stage >= 3 ? XyzDialogue.HURT_ANGRY : XyzDialogue.HURT_DOCILE;
                XyzDirector.say(p, XyzDialogue.pick(pool));
                speechCooldown = 40;
            }
        }
        if (getHealth() <= getMaxHealth() * 0.15F) {  // he cannot die
            setHealth(getMaxHealth());
            if (st.stage < 3) st.stage = 3;
            speechCooldown = 60;
            XyzDirector.say(p, XyzDialogue.pick(XyzDialogue.UNDYING));
        }
        return result;
    }

    // ------------------------------------------------------------------ behaviour

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel lvl = (ServerLevel) level();
        ServerPlayer p = targetUUID == null ? null : lvl.getServer().getPlayerList().getPlayer(targetUUID);
        if (p == null || p.level() != level() || p.isSpectator() || !p.isAlive()) {
            discard();
            return;
        }
        // only ONE Xyz per player: if a twin exists, the newer one disappears
        if (tickCount % 20 == 0) {
            List<? extends XyzEntity> twins = lvl.getEntities(EntityTypeTest.forClass(XyzEntity.class),
                    e -> e != this && p.getUUID().equals(e.getTargetUUID()));
            if (twins.stream().anyMatch(o -> o.getId() < getId())) { discard(); return; }
        }
        XyzData.PlayerState st = XyzDirector.state(p);

        lifeTicks++;
        if (speechCooldown > 0) speechCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (tpCooldown > 0) tpCooldown--;

        boolean hostile = st.stage >= 3;
        if (hostile != entityData.get(HOSTILE)) entityData.set(HOSTILE, hostile);

        double dist = distanceTo(p);
        boolean seen = looksAt(p);
        lookTicks = seen ? lookTicks + 1 : Math.max(0, lookTicks - 2);
        getLookControl().setLookAt(p, 40.0F, 40.0F);

        if (st.stage >= 2) greeted = true;

        if (hostile) hunt(p, st, dist);
        else if (st.stage == 2) stalk(p, st, dist, seen);
        else watch(p, st, dist, seen);
    }

    /** Stage 0-1: stands far away, speaks gently, vanishes if you get close or stare. */
    private void watch(ServerPlayer p, XyzData.PlayerState st, double dist, boolean seen) {
        getNavigation().stop();
        if (speechCooldown <= 0 && dist < 32 && seen) {
            if (!greeted || (R.nextFloat() < 0.6F && st.storyIndex < XyzDialogue.STORY.length)) {
                int i = Math.min(st.storyIndex++, XyzDialogue.STORY.length - 1);
                XyzDirector.say(p, XyzDialogue.STORY[i]);
            } else {
                XyzDirector.say(p, XyzDialogue.pick(XyzDialogue.DOCILE));
            }
            greeted = true;
            speechCooldown = 20 * (14 + R.nextInt(14));
        }
        if (dist < 6 || lookTicks > 120 || lifeTicks > 20 * 90) vanish(p, st);
    }

    /** Stage 2: follows at a distance, teleports behind you when you stare, never stops talking. */
    private void stalk(ServerPlayer p, XyzData.PlayerState st, double dist, boolean seen) {
        if (dist > 16) getNavigation().moveTo(p, 1.0D); else getNavigation().stop();

        if (seen && lookTicks > 30 && tpCooldown <= 0) {
            teleportBehind(p, 4 + R.nextInt(4));
            lookTicks = 0;
            tpCooldown = 20 * (20 + R.nextInt(20));
        }
        if (speechCooldown <= 0 && dist < 30) {
            XyzDirector.say(p, XyzDialogue.pick(XyzDialogue.STALK));
            speechCooldown = 20 * (10 + R.nextInt(15));
        }
        if (dist < 3.0 || lifeTicks > 20 * 150) vanish(p, st);
    }

    /** Stage 3: chases and kills. */
    private void hunt(ServerPlayer p, XyzData.PlayerState st, double dist) {
        if (dist > 45 && tpCooldown <= 0) { teleportBehind(p, 12); tpCooldown = 200; }
        if (tickCount % 10 == 0 || getNavigation().isDone()) getNavigation().moveTo(p, 1.15D);
        if (tickCount % 100 == 0 && getNavigation().isDone() && dist > 6) teleportBehind(p, 4); // stuck

        if (attackCooldown <= 0 && !p.isCreative()
                && getBoundingBox().inflate(0.8, 0.2, 0.8).intersects(p.getBoundingBox())) {
            doHurtTarget(p);
            swing(InteractionHand.MAIN_HAND);
            triggerAnim("attackController", "attack");
            attackCooldown = 20;
        }
        if (speechCooldown <= 0) {
            XyzDirector.say(p, XyzDialogue.pick(XyzDialogue.HUNT));
            speechCooldown = 20 * (6 + R.nextInt(8));
        }
    }

    private void vanish(ServerPlayer p, XyzData.PlayerState st) {
        if (speechCooldown <= 0 || R.nextBoolean()) XyzDirector.say(p, XyzDialogue.pick(XyzDialogue.VANISH));
        XyzEvents.play((ServerLevel) level(), position(), SoundEvents.ENDERMAN_TELEPORT, 0.6F, 0.4F);
        st.appearCooldown = switch (st.stage) {
            case 0, 1 -> 45 + R.nextInt(60);
            case 2 -> 20 + R.nextInt(30);
            default -> 8 + R.nextInt(10);
        };
        discard();
    }

    private void teleportBehind(ServerPlayer p, double dist) {
        ServerLevel lvl = (ServerLevel) level();
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        for (int i = 0; i < 8; i++) {
            Vec3 dir = flat.yRot((float) ((R.nextDouble() - 0.5) * 0.9));
            BlockPos pos = XyzEvents.findStand(lvl, p.getX() - dir.x * dist, p.getZ() - dir.z * dist, p.getBlockY());
            if (pos != null) {
                moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                getNavigation().stop();
                XyzEvents.play(lvl, position(), SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.4F);
                return;
            }
        }
    }

    private boolean looksAt(ServerPlayer p) {
        Vec3 view = p.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(getX() - p.getX(), getEyeY() - p.getEyeY(), getZ() - p.getZ());
        double d = to.length();
        if (d < 0.001) return true;
        to = to.scale(1.0 / d);
        return view.dot(to) > 1.0 - 0.03 / d && p.hasLineOfSight(this);
    }

    // ------------------------------------------------------------------ sounds (vanilla placeholders; swap here)

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WARDEN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.WARDEN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDERMAN_DEATH; }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 5, state -> {
            if (state.isMoving()) return state.setAndContinue(entityData.get(HOSTILE) ? RUN : WALK);
            return state.setAndContinue(IDLE);
        }));
        controllers.add(new AnimationController<>(this, "attackController", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
