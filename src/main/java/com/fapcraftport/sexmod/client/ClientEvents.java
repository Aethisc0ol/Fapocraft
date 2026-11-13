package com.fapcraftport.sexmod.client;

import com.fapcraftport.sexmod.SexMod;
import com.fapcraftport.sexmod.config.ClientConfig;
import com.fapcraftport.sexmod.entity.GirlState;
import com.fapcraftport.sexmod.entity.JennyEntity;
import com.fapcraftport.sexmod.network.ModNetwork;
import javax.annotation.Nullable;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = SexMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    /** The Jenny the local player is currently in a scene with, if any. */
    @Nullable static JennyEntity activeScene;

    @Nullable private static CameraType savedCamera;
    private static int speedCooldown;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            activeScene = null;
            restoreCamera(mc);
            return;
        }

        activeScene = findScene(player);
        JennyEntity jenny = activeScene;
        GirlState state = jenny == null ? GirlState.NULL : jenny.getState();

        if (jenny == null) {
            ClientHooks.meter = 0.0F;
        }

        // Side-on third-person camera while the player is part of the scene.
        if (state.isLocked()) {
            if (savedCamera == null) {
                savedCamera = mc.options.getCameraType();
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
        } else {
            restoreCamera(mc);
        }

        if (speedCooldown > 0) speedCooldown--;
        if (jenny == null) {
            // Drop presses made outside of a scene so they don't fire later.
            while (ClientSetup.FINISH.consumeClick()) { }
            while (ClientSetup.LEAVE.consumeClick()) { }
            return;
        }

        if (ClientSetup.SPEED.isDown() && speedCooldown == 0
                && (state.acceptsSpeed() || state == GirlState.WAITDOGGY)) {
            send(jenny, ModNetwork.INPUT_SPEED);
            speedCooldown = 5;
        }
        while (ClientSetup.FINISH.consumeClick()) {
            if (state.acceptsFinish() && ClientHooks.meter >= 1.0F) send(jenny, ModNetwork.INPUT_FINISH);
        }
        while (ClientSetup.LEAVE.consumeClick()) {
            send(jenny, ModNetwork.INPUT_LEAVE);
        }
    }

    private static void send(JennyEntity jenny, int input) {
        PacketDistributor.sendToServer(new ModNetwork.SceneInput(jenny.getId(), input));
    }

    @Nullable
    private static JennyEntity findScene(Player player) {
        for (JennyEntity jenny : player.level().getEntitiesOfClass(JennyEntity.class, player.getBoundingBox().inflate(16))) {
            if (jenny.getState() != GirlState.NULL && jenny.isPartner(player)) return jenny;
        }
        return null;
    }

    private static void restoreCamera(Minecraft mc) {
        if (savedCamera != null) {
            mc.options.setCameraType(savedCamera);
            savedCamera = null;
        }
    }

    /** Freeze movement while locked into a scene. Looking around (orbiting the camera) still works. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        JennyEntity jenny = activeScene;
        if (jenny == null || !jenny.getState().isLocked()) return;
        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    /** The partner is drawn by Jenny's model during a scene, so hide the real player model. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        for (JennyEntity jenny : player.level().getEntitiesOfClass(JennyEntity.class, player.getBoundingBox().inflate(2))) {
            if (jenny.getState().isLocked() && jenny.isPartner(player)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen title
                && !AdultWarningScreen.acceptedThisSession
                && !ClientConfig.SKIP_ADULT_WARNING.get()) {
            event.setNewScreen(new AdultWarningScreen(title));
        }
    }
}
