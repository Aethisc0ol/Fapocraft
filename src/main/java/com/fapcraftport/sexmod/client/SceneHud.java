package com.fapcraftport.sexmod.client;

import com.fapcraftport.sexmod.entity.GirlState;
import com.fapcraftport.sexmod.entity.JennyEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Pleasure meter, key hints and the post-finish black-screen fade. */
public final class SceneHud {
    private SceneHud() {}

    private static final int FADE_TICKS = 40;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        int w = g.guiWidth();
        int h = g.guiHeight();
        Font font = mc.font;

        JennyEntity jenny = ClientEvents.activeScene;
        if (jenny != null) {
            GirlState state = jenny.getState();
            if (state == GirlState.WAITDOGGY) {
                g.drawCenteredString(font, Component.translatable("sexmod.hud.hint.doggywait", jenny.getDisplayName()),
                        w / 2, h - 70, 0xFFFFFF);
            }
            if (state.isLocked()) {
                int barW = 120;
                int x = (w - barW) / 2;
                int y = h - 62;
                g.drawCenteredString(font, Component.translatable("sexmod.hud.meter"), w / 2, y - 11, 0xFFC0E0);
                g.fill(x - 1, y - 1, x + barW + 1, y + 7, 0xAA000000);
                g.fill(x, y, x + Mth.floor(barW * ClientHooks.meter), y + 6, 0xFFE0559A);

                int hy = y + 12;
                if (state.acceptsSpeed() || state.acceptsFinish()) {
                    g.drawCenteredString(font, Component.translatable("sexmod.hud.hint.speed",
                            ClientSetup.SPEED.getTranslatedKeyMessage()), w / 2 - 60, hy, 0xDDDDDD);
                }
                if (state.acceptsFinish()) {
                    int color = ClientHooks.meter >= 1.0F ? 0x55FF55 : 0x777777;
                    g.drawCenteredString(font, Component.translatable("sexmod.hud.hint.finish",
                            ClientSetup.FINISH.getTranslatedKeyMessage()), w / 2, hy, color);
                }
                g.drawCenteredString(font, Component.translatable("sexmod.hud.hint.leave",
                        ClientSetup.LEAVE.getTranslatedKeyMessage()), w / 2 + 60, hy, 0xDDDDDD);
            }
        }

        if (ClientHooks.blackScreenStart >= 0) {
            float t = (mc.level.getGameTime() - ClientHooks.blackScreenStart + delta.getGameTimeDeltaPartialTick(false)) / FADE_TICKS;
            if (t >= 1.0F) {
                ClientHooks.blackScreenStart = -1;
            } else {
                // Fade in fast, hold, fade out.
                float alpha = t < 0.15F ? t / 0.15F : t > 0.6F ? (1.0F - t) / 0.4F : 1.0F;
                int a = Mth.clamp((int) (alpha * 255), 0, 255);
                g.fill(0, 0, w, h, a << 24);
            }
        }
    }
}
