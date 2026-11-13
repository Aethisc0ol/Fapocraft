package com.fapcraftport.sexmod.entity;

import com.fapcraftport.sexmod.client.ClientHooks;
import com.fapcraftport.sexmod.network.ModNetwork;
import com.fapcraftport.sexmod.registry.ModSounds;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class JennyEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NUDE =
            SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> PARTNER =
            SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> FOLLOWING =
            SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> PENDING_ACTION =
            SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.INT);

    private static final int HOME_RADIUS = 12;
    /** Non-looping states that last longer than this are assumed stuck (partner's client never reported). */
    private static final int STUCK_TICKS = 20 * 40;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Server-only scene bookkeeping.
    @Nullable private BlockPos home;
    @Nullable private BlockPos bed;
    @Nullable private Vec3 anchor;
    private float sceneYaw;
    private int stateTicks;

    public JennyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // ------------------------------------------------------------------ synced data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, GirlState.NULL.ordinal());
        builder.define(NUDE, false);
        builder.define(PARTNER, Optional.empty());
        builder.define(FOLLOWING, Optional.empty());
        builder.define(PENDING_ACTION, -1);
    }

    public GirlState getState() {
        return GirlState.byId(entityData.get(STATE));
    }

    private void setState(GirlState state) {
        entityData.set(STATE, state.ordinal());
        stateTicks = 0;
    }

    public boolean isNude() {
        return entityData.get(NUDE);
    }

    public Optional<UUID> getPartnerId() {
        return entityData.get(PARTNER);
    }

    @Nullable
    public Player getPartner() {
        return getPartnerId().map(id -> level().getPlayerByUUID(id)).orElse(null);
    }

    public boolean isPartner(Player player) {
        return getPartnerId().map(id -> id.equals(player.getUUID())).orElse(false);
    }

    @Nullable
    public GirlAction getPendingAction() {
        return GirlAction.byId(entityData.get(PENDING_ACTION));
    }

    public boolean isFollowing(Player player) {
        return entityData.get(FOLLOWING).map(id -> id.equals(player.getUUID())).orElse(false);
    }

    public boolean isIdle() {
        return getState() == GirlState.NULL;
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new FollowPartnerGoal());
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.8) {
            @Override public boolean canUse() { return isIdle() && super.canUse(); }
            @Override public boolean canContinueToUse() { return isIdle() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override public boolean canUse() { return isIdle() && !isFollowingAnyone() && super.canUse(); }
            @Override public boolean canContinueToUse() { return isIdle() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override public boolean canUse() { return isIdle() && super.canUse(); }
            @Override public boolean canContinueToUse() { return isIdle() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new RandomLookAroundGoal(this) {
            @Override public boolean canUse() { return isIdle() && super.canUse(); }
            @Override public boolean canContinueToUse() { return isIdle() && super.canContinueToUse(); }
        });
    }

    private boolean isFollowingAnyone() {
        return entityData.get(FOLLOWING).isPresent();
    }

    /** Follows the player who pressed "Follow me", teleporting if left far behind (like a tamed wolf). */
    private final class FollowPartnerGoal extends Goal {
        private int repath;

        FollowPartnerGoal() {
            setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Nullable
        private Player target() {
            return entityData.get(FOLLOWING).map(id -> level().getPlayerByUUID(id)).orElse(null);
        }

        @Override
        public boolean canUse() {
            Player p = target();
            return isIdle() && p != null && !p.isSpectator() && distanceToSqr(p) > 3.0 * 3.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player p = target();
            return isIdle() && p != null && distanceToSqr(p) > 2.0 * 2.0;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            Player p = target();
            if (p == null) return;
            getLookControl().setLookAt(p, 10.0F, getMaxHeadXRot());
            if (--repath > 0) return;
            repath = 10;
            double distSq = distanceToSqr(p);
            if (distSq > 24 * 24 && p.onGround()) {
                moveTo(p.getX(), p.getY(), p.getZ(), getYRot(), getXRot());
                getNavigation().stop();
                return;
            }
            getNavigation().moveTo(p, distSq > 8 * 8 ? 1.3 : 0.9);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return isIdle() && super.isPushable();
    }

    @Override
    protected void pushEntities() {
        if (isIdle()) super.pushEntities();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.get("girls.jenny.huh");
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        GirlState state = getState();
        if (state == GirlState.NULL) return;
        stateTicks++;

        Player partner = getPartner();
        if (partner == null || !partner.isAlive() || partner.distanceToSqr(this) > 12 * 12) {
            endScene();
            return;
        }

        if (state.pausesAi()) {
            getNavigation().stop();
            setYRot(sceneYaw);
            yBodyRot = sceneYaw;
            yHeadRot = sceneYaw;
        }

        if (state.isLocked() && anchor != null && partner instanceof ServerPlayer sp && tickCount % 5 == 0
                && sp.position().distanceToSqr(anchor) > 0.25) {
            sp.connection.teleport(anchor.x, anchor.y, anchor.z, sp.getYRot(), sp.getXRot());
        }

        switch (state) {
            case WALK_TO_BED -> tickWalkToBed();
            case THRUSTBLOWJOB, DOGGYFAST, PAIZURI_FAST -> {
                // Fallback in case the "done" keyframe never arrives.
                if (stateTicks > 20 * 5) setState(slowStateFor(state));
            }
            case SUCKBLOWJOB, DOGGYSLOW, PAIZURI_SLOW, WAITDOGGY -> { /* loops until player input */ }
            default -> {
                if (stateTicks > STUCK_TICKS) endScene();
            }
        }
    }

    private void tickWalkToBed() {
        if (bed == null || !level().getBlockState(bed).is(BlockTags.BEDS)) {
            sayToPartner("jenny.dialogue.nobedinsight");
            endScene();
            return;
        }
        Vec3 target = bedCenter(bed);
        if (position().distanceToSqr(target.x, getY(), target.z) < 1.5 * 1.5 || stateTicks > 20 * 12) {
            BlockState bs = level().getBlockState(bed);
            Direction facing = bs.getValue(BedBlock.FACING);
            sceneYaw = facing.toYRot();
            moveTo(target.x, target.y, target.z, sceneYaw, 0.0F);
            getNavigation().stop();
            setState(GirlState.STARTDOGGY);
        } else if (stateTicks % 20 == 1) {
            getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        }
    }

    private static Vec3 bedCenter(BlockPos foot) {
        return Vec3.atBottomCenterOf(foot).add(0, 0.5625, 0);
    }

    @Nullable
    private BlockPos findBed() {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -3, -8), origin.offset(8, 3, 8))) {
            BlockState bs = level().getBlockState(pos);
            if (!bs.is(BlockTags.BEDS) || bs.getValue(BedBlock.PART) != BedPart.FOOT) continue;
            if (!level().getBlockState(pos.above()).isAir()) continue;
            double d = pos.distSqr(origin);
            if (d < bestDist) {
                bestDist = d;
                best = pos.immutable();
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) return InteractionResult.SUCCESS;

        ServerPlayer sp = (ServerPlayer) player;
        GirlState state = getState();
        if (state == GirlState.WAITDOGGY && isPartner(player)) {
            startDoggy();
        } else if (state != GirlState.NULL) {
            say(sp, "jenny.dialogue.busy");
        } else {
            PacketDistributor.sendToPlayer(sp, new ModNetwork.OpenMenu(getId()));
        }
        return InteractionResult.CONSUME;
    }

    /** Called from the menu (server side). */
    public void handleAction(ServerPlayer player, GirlAction action) {
        if (!isIdle()) {
            say(player, "jenny.dialogue.busy");
            return;
        }
        switch (action) {
            case FOLLOW -> {
                entityData.set(FOLLOWING, Optional.of(player.getUUID()));
                clearRestriction();
                say(player, "jenny.dialogue.followme", player.getDisplayName());
            }
            case STOP_FOLLOW -> {
                entityData.set(FOLLOWING, Optional.empty());
                if (home != null) restrictTo(home, HOME_RADIUS);
                getNavigation().stop();
                say(player, "jenny.dialogue.bye");
            }
            case SET_HOME -> {
                home = blockPosition();
                if (!isFollowingAnyone()) restrictTo(home, HOME_RADIUS);
                player.sendSystemMessage(Component.translatable("sexmod.home.set", getDisplayName()));
            }
            case GO_HOME -> {
                if (home == null) {
                    player.sendSystemMessage(Component.translatable("sexmod.home.none", getDisplayName()));
                    return;
                }
                entityData.set(FOLLOWING, Optional.empty());
                restrictTo(home, HOME_RADIUS);
                getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
                say(player, "jenny.dialogue.bye2");
            }
            case DRESS_UP -> entityData.set(NUDE, false);
            default -> startScene(player, action);
        }
    }

    private void startScene(ServerPlayer player, GirlAction action) {
        if (action == GirlAction.STRIP && isNude()) return;
        if (action == GirlAction.DOGGY) {
            bed = findBed();
            if (bed == null) {
                say(player, "jenny.dialogue.nobedinsight");
                return;
            }
        }
        if (!player.getAbilities().instabuild) {
            if (player.getInventory().countItem(action.price) < action.priceCount) {
                player.sendSystemMessage(Component.translatable("sexmod.payment.missing",
                        getDisplayName(), action.price.getDescription(), action.priceCount));
                return;
            }
            removeItems(player.getInventory(), action);
        }

        entityData.set(PARTNER, Optional.of(player.getUUID()));
        entityData.set(PENDING_ACTION, action.ordinal());
        sceneYaw = yawTowards(player.position());
        getNavigation().stop();
        setState(GirlState.PAYMENT);
    }

    private static void removeItems(Inventory inv, GirlAction action) {
        int left = action.priceCount;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(action.price)) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
        inv.setChanged();
    }

    private float yawTowards(Vec3 target) {
        double dx = target.x - getX();
        double dz = target.z - getZ();
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
    }

    /** After payment (and stripping, if needed) start the actual scene. */
    private void beginMain() {
        GirlAction action = getPendingAction();
        if (action == null) {
            endScene();
            return;
        }
        switch (action) {
            case BLOWJOB -> {
                lockPartner();
                setState(GirlState.STARTBLOWJOB);
            }
            case BOOBJOB -> {
                lockPartner();
                setState(GirlState.PAIZURI_START);
            }
            case DOGGY -> setState(GirlState.WALK_TO_BED);
            default -> endScene();
        }
    }

    private void startDoggy() {
        lockPartner();
        setState(GirlState.DOGGYSTART);
    }

    /**
     * The partner's body is drawn by Jenny's model (the "steve" bone group), so the real player
     * stands at Jenny's origin, hidden, turned sideways so the third-person camera gets a side view.
     */
    private void lockPartner() {
        anchor = position();
        Player partner = getPartner();
        if (partner instanceof ServerPlayer sp) {
            sp.connection.teleport(anchor.x, anchor.y, anchor.z, Mth.wrapDegrees(sceneYaw + 90.0F), 15.0F);
        }
    }

    private void endScene() {
        setState(GirlState.NULL);
        entityData.set(PARTNER, Optional.empty());
        entityData.set(PENDING_ACTION, -1);
        anchor = null;
        bed = null;
        if (home != null && !isFollowingAnyone()) restrictTo(home, HOME_RADIUS);
    }

    private static GirlState slowStateFor(GirlState fast) {
        return switch (fast) {
            case THRUSTBLOWJOB -> GirlState.SUCKBLOWJOB;
            case DOGGYFAST -> GirlState.DOGGYSLOW;
            case PAIZURI_FAST -> GirlState.PAIZURI_SLOW;
            default -> fast;
        };
    }

    /** Animation keyframe event forwarded by the partner's client. */
    public void onSceneEvent(ServerPlayer player, String event) {
        if (!isPartner(player)) return;
        GirlState state = getState();
        switch (event) {
            case "paymentDone" -> {
                if (state != GirlState.PAYMENT) return;
                // Like the original, a boobjob strips her first if she's dressed.
                GirlAction pending = getPendingAction();
                if (pending == GirlAction.STRIP || (pending == GirlAction.BOOBJOB && !isNude())) setState(GirlState.STRIP);
                else beginMain();
            }
            case "becomeNude" -> {
                if (state == GirlState.STRIP) entityData.set(NUDE, true);
            }
            case "stripDone" -> {
                if (state != GirlState.STRIP) return;
                entityData.set(NUDE, true);
                if (getPendingAction() == GirlAction.STRIP) endScene();
                else beginMain();
            }
            case "bjiDone" -> { if (state == GirlState.STARTBLOWJOB) setState(GirlState.SUCKBLOWJOB); }
            case "bjtDone" -> { if (state == GirlState.THRUSTBLOWJOB) setState(GirlState.SUCKBLOWJOB); }
            case "bjcDone" -> { if (state == GirlState.CUMBLOWJOB) endScene(); }
            case "doggyGoOnBedDone" -> { if (state == GirlState.STARTDOGGY) setState(GirlState.WAITDOGGY); }
            case "doggystartDone" -> { if (state == GirlState.DOGGYSTART) setState(GirlState.DOGGYSLOW); }
            case "doggyfastDone" -> { if (state == GirlState.DOGGYFAST) setState(GirlState.DOGGYSLOW); }
            case "doggyCumDone" -> { if (state == GirlState.DOGGYCUM) endScene(); }
            case "paizuri_startDone" -> { if (state == GirlState.PAIZURI_START) setState(GirlState.PAIZURI_SLOW); }
            case "paizuri_fastDone" -> { if (state == GirlState.PAIZURI_FAST) setState(GirlState.PAIZURI_SLOW); }
            case "paizuri_cumDone" -> { if (state == GirlState.PAIZURI_CUM) endScene(); }
            default -> { }
        }
    }

    /** Scene key pressed by the partner. */
    public void onSceneInput(ServerPlayer player, int input) {
        if (!isPartner(player)) return;
        GirlState state = getState();
        switch (input) {
            case ModNetwork.INPUT_SPEED -> {
                switch (state) {
                    case SUCKBLOWJOB -> setState(GirlState.THRUSTBLOWJOB);
                    case DOGGYSLOW -> setState(GirlState.DOGGYFAST);
                    case PAIZURI_SLOW -> setState(GirlState.PAIZURI_FAST);
                    case WAITDOGGY -> startDoggy();
                    default -> { }
                }
            }
            case ModNetwork.INPUT_FINISH -> {
                if (!state.acceptsFinish()) return;
                switch (state) {
                    case SUCKBLOWJOB, THRUSTBLOWJOB -> setState(GirlState.CUMBLOWJOB);
                    case DOGGYSLOW, DOGGYFAST -> setState(GirlState.DOGGYCUM);
                    case PAIZURI_SLOW, PAIZURI_FAST -> setState(GirlState.PAIZURI_CUM);
                    default -> { }
                }
            }
            case ModNetwork.INPUT_LEAVE -> endScene();
            default -> { }
        }
    }

    // ------------------------------------------------------------------ chat

    private void say(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.literal("<").append(getDisplayName()).append("> ")
                .append(Component.translatable(key, args)));
    }

    private void sayToPartner(String key) {
        if (getPartner() instanceof ServerPlayer sp) say(sp, key);
    }

    // ------------------------------------------------------------------ save data

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Nude", isNude());
        if (home != null) tag.putLong("Home", home.asLong());
        entityData.get(FOLLOWING).ifPresent(id -> tag.putUUID("Following", id));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(NUDE, tag.getBoolean("Nude"));
        if (tag.contains("Home")) {
            home = BlockPos.of(tag.getLong("Home"));
        }
        entityData.set(FOLLOWING, tag.hasUUID("Following") ? Optional.of(tag.getUUID("Following")) : Optional.empty());
        if (home != null && !isFollowingAnyone()) restrictTo(home, HOME_RADIUS);
    }

    // ------------------------------------------------------------------ GeckoLib

    private static RawAnimation anim(String name) {
        return RawAnimation.begin().then("animation.jenny." + name, Animation.LoopType.DEFAULT);
    }

    private static final RawAnimation IDLE = anim("idle");
    private static final RawAnimation WALK = anim("walk");
    private static final RawAnimation FASTWALK = anim("fastwalk");
    private static final RawAnimation PAYMENT = anim("payment");
    private static final RawAnimation STRIP = anim("strip");
    private static final RawAnimation BJ_INTRO = anim("blowjobintro");
    private static final RawAnimation BJ_SUCK = anim("blowjobsuck");
    private static final RawAnimation BJ_THRUST = anim("blowjobthrust");
    private static final RawAnimation BJ_CUM = anim("blowjobcum");
    private static final RawAnimation DOGGY_GO_ON_BED = anim("doggygoonbed");
    private static final RawAnimation DOGGY_WAIT = anim("doggywait");
    private static final RawAnimation DOGGY_START = anim("doggystart");
    private static final RawAnimation DOGGY_SLOW = anim("doggyslow");
    private static final RawAnimation DOGGY_FAST_SOFT = anim("doggyfast_soft");
    private static final RawAnimation DOGGY_FAST_HARD = anim("doggyfast_hard");
    private static final RawAnimation DOGGY_CUM = anim("doggycum");
    private static final RawAnimation PAIZURI_START = anim("paizuri_start");
    private static final RawAnimation PAIZURI_SLOW = anim("paizuri_slow");
    private static final RawAnimation PAIZURI_FAST = anim("paizuri_fast");
    private static final RawAnimation PAIZURI_CUM = anim("paizuri_cum");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::predicate)
                .setSoundKeyframeHandler(event -> ClientHooks.onKeyframe(this, event.getKeyframeData().getSound())));
    }

    private PlayState predicate(AnimationState<JennyEntity> state) {
        RawAnimation anim = switch (getState()) {
            case NULL -> state.isMoving()
                    ? (getDeltaMovement().horizontalDistanceSqr() > 0.03 ? FASTWALK : WALK)
                    : IDLE;
            case WALK_TO_BED -> WALK;
            case PAYMENT -> PAYMENT;
            case STRIP -> STRIP;
            case STARTBLOWJOB -> BJ_INTRO;
            case SUCKBLOWJOB -> BJ_SUCK;
            case THRUSTBLOWJOB -> BJ_THRUST;
            case CUMBLOWJOB -> BJ_CUM;
            case STARTDOGGY -> DOGGY_GO_ON_BED;
            case WAITDOGGY -> DOGGY_WAIT;
            case DOGGYSTART -> DOGGY_START;
            case DOGGYSLOW -> DOGGY_SLOW;
            case DOGGYFAST -> ClientHooks.useHardVariant(this) ? DOGGY_FAST_HARD : DOGGY_FAST_SOFT;
            case DOGGYCUM -> DOGGY_CUM;
            case PAIZURI_START -> PAIZURI_START;
            case PAIZURI_SLOW -> PAIZURI_SLOW;
            case PAIZURI_FAST -> PAIZURI_FAST;
            case PAIZURI_CUM -> PAIZURI_CUM;
        };
        return state.setAndContinue(anim);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
