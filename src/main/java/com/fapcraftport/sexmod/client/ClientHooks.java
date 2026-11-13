package com.fapcraftport.sexmod.client;

import com.fapcraftport.sexmod.entity.GirlAction;
import com.fapcraftport.sexmod.entity.JennyEntity;
import com.fapcraftport.sexmod.network.ModNetwork;
import com.fapcraftport.sexmod.registry.ModSounds;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only logic. Only ever called from code paths that run on the client
 * (GeckoLib keyframe handlers, animation predicates, client payload handlers).
 */
public final class ClientHooks {
    private ClientHooks() {}

    /** The "horny meter" from the original sex UI. 0..1, filled by scene keyframes. */
    static float meter;
    /** Game time when the post-finish black-screen fade started, or -1. */
    static long blackScreenStart = -1;

    /** Keyframe events whose outcome the server needs to know about. */
    private static final Set<String> SERVER_EVENTS = Set.of(
            "paymentDone", "becomeNude", "stripDone",
            "bjiDone", "bjtDone", "bjcDone",
            "doggyGoOnBedDone", "doggystartDone", "doggyfastDone", "doggyCumDone",
            "paizuri_startDone", "paizuri_fastDone", "paizuri_cumDone");

    public static void openMenu(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(entityId);
        if (e instanceof JennyEntity jenny) mc.setScreen(new GirlMenuScreen(jenny));
    }

    public static boolean isLocalPartner(JennyEntity jenny) {
        Player player = Minecraft.getInstance().player;
        return player != null && jenny.isPartner(player);
    }

    public static boolean useHardVariant(JennyEntity jenny) {
        return isLocalPartner(jenny) && meter > 0.5F;
    }

    /**
     * Port of the original sound-keyframe handler. Sounds play for everyone watching;
     * dialogue, the meter and server notifications only happen for the partner.
     */
    public static void onKeyframe(JennyEntity jenny, String event) {
        boolean partner = isLocalPartner(jenny);
        if (partner && SERVER_EVENTS.contains(event)) {
            PacketDistributor.sendToServer(new ModNetwork.SceneEvent(jenny.getId(), event));
        }
        RandomSource rand = jenny.getRandom();

        switch (event) {
            case "stripMSG1" -> line(jenny, partner, "jenny.dialogue.hihi", "giggle");
            case "paymentMSG1" -> line(jenny, partner, "jenny.dialogue.huh", "huh");
            case "paymentMSG2" -> {
                play(jenny, "misc.plob");
                GirlAction action = jenny.getPendingAction();
                if (partner && action != null && action.requestLine != null) playerLine(action.requestLine);
            }
            case "paymentMSG3" -> line(jenny, partner, "jenny.dialogue.hehe", "giggle");
            case "paymentMSG4" -> play(jenny, "misc.plob");

            case "bjiMSG1" -> line(jenny, partner, "jenny.dialogue.blowjobtext1", "mmm");
            case "bjiMSG2" -> line(jenny, partner, "jenny.dialogue.blowjobtext2", "lightbreathing");
            case "bjiMSG3" -> line(jenny, partner, "jenny.dialogue.blowjobtext3", "aftersessionmoan");
            case "bjiMSG4" -> play(jenny, "misc.belljingle");
            case "bjiMSG5" -> line(jenny, partner, "jenny.dialogue.blowjobtext4", "hmph");
            case "bjiMSG6" -> line(jenny, partner, "jenny.dialogue.blowjobtext5", "lightbreathing");
            case "bjiMSG7" -> line(jenny, partner, "jenny.dialogue.blowjobtext6", "giggle");
            case "bjiMSG8" -> {
                if (partner) playerLine("jenny.dialogue.blowjobtext7");
                play(jenny, "misc.plob");
            }
            case "bjiMSG9" -> line(jenny, partner, "jenny.dialogue.blowjobtext8", "giggle");
            case "bjiMSG11" -> {
                girl(jenny, "lipsound");
                fill(partner, 0.02F);
            }
            case "bjiMSG12" -> {
                if (rand.nextInt(3) == 0) girl(jenny, "bjmoan");
                girl(jenny, "lipsound");
                fill(partner, 0.02F);
            }
            case "bjtMSG1" -> {
                girl(jenny, "mmm");
                girl(jenny, "lipsound");
                fill(partner, 0.04F);
            }
            case "bjcMSG1" -> girl(jenny, "bjmoan");
            case "bjcMSG2" -> {
                girl(jenny, "bjmoan");
                if (partner) meter = 0.0F;
            }
            case "bjcMSG3" -> girl(jenny, "aftersessionmoan");
            case "bjcMSG4", "bjcMSG5", "bjcMSG6", "bjcMSG7" -> girl(jenny, "lightbreathing");
            case "bjcBlackScreen" -> {
                if (partner) blackScreenStart = jenny.level().getGameTime();
            }

            case "doggyGoOnBedMSG1" -> play(jenny, "misc.bedrustle");
            case "doggyGoOnBedMSG2" -> line(jenny, partner, "jenny.dialogue.doggytext1", "lightbreathing");
            case "doggyGoOnBedMSG3" -> line(jenny, partner, "jenny.dialogue.doggytext2", "giggle");
            case "doggyGoOnBedMSG4" -> play(jenny, "misc.slap");
            case "doggystartMSG1", "doggystartMSG2" -> play(jenny, "misc.touch");
            case "doggystartMSG3" -> play(jenny, "misc.bedrustle");
            case "doggystartMSG4" -> {
                play(jenny, "misc.smallinserts");
                girl(jenny, "mmm");
            }
            case "doggystartMSG5" -> {
                play(jenny, "misc.pounding");
                girl(jenny, "moan");
            }
            case "doggyslowMSG1" -> {
                play(jenny, "misc.pounding");
                girl(jenny, pick(rand, "mmm", "moan", "heavybreathing"));
                fill(partner, 0.02F);
            }
            case "doggyslowMSG2" -> {
                if (rand.nextBoolean()) girl(jenny, "lightbreathing");
            }
            case "doggyfastMSG1" -> {
                play(jenny, "misc.pounding");
                girl(jenny, pick(rand, "moan", "heavybreathing", "ahh"));
                fill(partner, 0.04F);
            }
            case "doggycumMSG1" -> {
                play(jenny, "misc.cuminflation");
                play(jenny, "misc.pounding");
                girl(jenny, "moan");
                if (partner) meter = 0.0F;
            }
            case "doggycumMSG2", "doggycumMSG3", "doggycumMSG4", "doggycumMSG5" -> girl(jenny, "heavybreathing");

            case "paizuriStartMSG1" -> {
                play(jenny, "misc.pounding");
                fill(partner, 0.02F);
            }
            // Not handled in the original; added so the slow loop also fills the meter.
            case "paizuriSlowMSG1" -> fill(partner, 0.02F);
            case "paizuriFastMSG1" -> {
                play(jenny, "misc.pounding");
                girl(jenny, pick(rand, "mmm", "ahh"));
                fill(partner, 0.04F);
            }
            default -> { }
        }
    }

    private static String pick(RandomSource rand, String... options) {
        return options[rand.nextInt(options.length)];
    }

    private static void fill(boolean partner, float amount) {
        if (partner) meter = Math.min(1.0F, meter + amount);
    }

    private static void girl(JennyEntity jenny, String sound) {
        play(jenny, "girls.jenny." + sound);
    }

    private static void play(JennyEntity jenny, String group) {
        SoundEvent sound = ModSounds.get(group);
        if (sound == null) return;
        jenny.level().playLocalSound(jenny.getX(), jenny.getY(), jenny.getZ(), sound, SoundSource.NEUTRAL,
                1.0F, 1.0F, false);
    }

    /** Jenny speaks: chat line for the partner plus a voice sound for everyone nearby. */
    private static void line(JennyEntity jenny, boolean partner, String key, String sound) {
        girl(jenny, sound);
        if (partner) {
            Minecraft.getInstance().gui.getChat().addMessage(
                    Component.literal("<").append(jenny.getDisplayName()).append("> ").append(Component.translatable(key)));
        }
    }

    /** The local player "says" something, like the original's fake chat lines. */
    private static void playerLine(String key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.gui.getChat().addMessage(
                Component.literal("<").append(mc.player.getDisplayName()).append("> ").append(Component.translatable(key)));
    }
}
