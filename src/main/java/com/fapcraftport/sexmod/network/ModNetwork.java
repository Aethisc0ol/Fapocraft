package com.fapcraftport.sexmod.network;

import com.fapcraftport.sexmod.SexMod;
import com.fapcraftport.sexmod.entity.GirlAction;
import com.fapcraftport.sexmod.entity.JennyEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    /** Server -> client: open Jenny's action menu. */
    public record OpenMenu(int entityId) implements CustomPacketPayload {
        public static final Type<OpenMenu> TYPE = new Type<>(SexMod.id("open_menu"));
        public static final StreamCodec<ByteBuf, OpenMenu> CODEC =
                ByteBufCodecs.VAR_INT.map(OpenMenu::new, OpenMenu::entityId);
        @Override public Type<OpenMenu> type() { return TYPE; }
    }

    /** Client -> server: a button in the menu was pressed. */
    public record Action(int entityId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(SexMod.id("action"));
        public static final StreamCodec<ByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::entityId,
                ByteBufCodecs.VAR_INT, Action::action,
                Action::new);
        @Override public Type<Action> type() { return TYPE; }
    }

    /**
     * Client -> server: an animation keyframe event fired on the partner's client.
     * GeckoLib only runs animations client-side, so scene progression is driven from there,
     * the same way the original did it.
     */
    public record SceneEvent(int entityId, String event) implements CustomPacketPayload {
        public static final Type<SceneEvent> TYPE = new Type<>(SexMod.id("scene_event"));
        public static final StreamCodec<ByteBuf, SceneEvent> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SceneEvent::entityId,
                ByteBufCodecs.stringUtf8(64), SceneEvent::event,
                SceneEvent::new);
        @Override public Type<SceneEvent> type() { return TYPE; }
    }

    public static final int INPUT_SPEED = 0;
    public static final int INPUT_FINISH = 1;
    public static final int INPUT_LEAVE = 2;

    /** Client -> server: scene key pressed. */
    public record SceneInput(int entityId, int input) implements CustomPacketPayload {
        public static final Type<SceneInput> TYPE = new Type<>(SexMod.id("scene_input"));
        public static final StreamCodec<ByteBuf, SceneInput> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SceneInput::entityId,
                ByteBufCodecs.VAR_INT, SceneInput::input,
                SceneInput::new);
        @Override public Type<SceneInput> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenMenu.TYPE, OpenMenu.CODEC,
                (payload, ctx) -> com.fapcraftport.sexmod.client.ClientHooks.openMenu(payload.entityId()));
        registrar.playToServer(Action.TYPE, Action.CODEC, ModNetwork::handleAction);
        registrar.playToServer(SceneEvent.TYPE, SceneEvent.CODEC, ModNetwork::handleSceneEvent);
        registrar.playToServer(SceneInput.TYPE, SceneInput.CODEC, ModNetwork::handleSceneInput);
    }

    private static JennyEntity findJenny(IPayloadContext ctx, int entityId) {
        if (!(ctx.player() instanceof ServerPlayer player)) return null;
        Entity e = player.level().getEntity(entityId);
        if (e instanceof JennyEntity jenny && jenny.distanceToSqr(player) < 16 * 16) return jenny;
        return null;
    }

    private static void handleAction(Action payload, IPayloadContext ctx) {
        JennyEntity jenny = findJenny(ctx, payload.entityId());
        GirlAction action = GirlAction.byId(payload.action());
        if (jenny != null && action != null) jenny.handleAction((ServerPlayer) ctx.player(), action);
    }

    private static void handleSceneEvent(SceneEvent payload, IPayloadContext ctx) {
        JennyEntity jenny = findJenny(ctx, payload.entityId());
        if (jenny != null) jenny.onSceneEvent((ServerPlayer) ctx.player(), payload.event());
    }

    private static void handleSceneInput(SceneInput payload, IPayloadContext ctx) {
        JennyEntity jenny = findJenny(ctx, payload.entityId());
        if (jenny != null) jenny.onSceneInput((ServerPlayer) ctx.player(), payload.input());
    }
}
