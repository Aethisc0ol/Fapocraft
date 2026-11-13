package com.fapcraftport.sexmod.registry;

import com.fapcraftport.sexmod.SexMod;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The original registered one SoundEvent per .ogg file and picked from arrays in code.
 * Here each folder is one SoundEvent and sounds.json lists every variant, so Minecraft
 * picks a random variant itself. Goblin and kobold sounds are not included.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, SexMod.MODID);

    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BY_NAME = new HashMap<>();

    private static final String[] NAMES = {
            "girls.allie.aftersessionmoan",
            "girls.allie.ahh",
            "girls.allie.bjmoan",
            "girls.allie.giggle",
            "girls.allie.happyoh",
            "girls.allie.heavybreathing",
            "girls.allie.hmph",
            "girls.allie.huh",
            "girls.allie.lightbreathing",
            "girls.allie.lipsound",
            "girls.allie.mmm",
            "girls.allie.moan",
            "girls.allie.sadoh",
            "girls.allie.scawy",
            "girls.allie.sigh",
            "girls.bia.ahh",
            "girls.bia.bjmoan",
            "girls.bia.breath",
            "girls.bia.giggle",
            "girls.bia.hey",
            "girls.bia.huh",
            "girls.bia.mmm",
            "girls.ellie.aftersessionmoan",
            "girls.ellie.ahh",
            "girls.ellie.bjmoan",
            "girls.ellie.cometomommy",
            "girls.ellie.giggle",
            "girls.ellie.goodboy",
            "girls.ellie.happyoh",
            "girls.ellie.heavybreathing",
            "girls.ellie.hmph",
            "girls.ellie.huh",
            "girls.ellie.lightbreathing",
            "girls.ellie.lipsound",
            "girls.ellie.mmm",
            "girls.ellie.moan",
            "girls.ellie.mommyhorny",
            "girls.ellie.sadoh",
            "girls.ellie.sigh",
            "girls.galath.aaa",
            "girls.galath.ahh",
            "girls.galath.breathing",
            "girls.galath.dialog",
            "girls.galath.giggle",
            "girls.galath.hmph",
            "girls.galath.huh",
            "girls.galath.lightcharge",
            "girls.galath.moan",
            "girls.galath.orgasm",
            "girls.galath.strongcharge",
            "girls.galath.uuh",
            "girls.jenny.aftersessionmoan",
            "girls.jenny.aftersessionmoan.bjmoan",
            "girls.jenny.ahh",
            "girls.jenny.bjmoan",
            "girls.jenny.giggle",
            "girls.jenny.happyoh",
            "girls.jenny.heavybreathing",
            "girls.jenny.hmph",
            "girls.jenny.huh",
            "girls.jenny.lightbreathing",
            "girls.jenny.lipsound",
            "girls.jenny.mmm",
            "girls.jenny.moan",
            "girls.jenny.sadoh",
            "girls.jenny.sigh",
            "girls.luna.ahh",
            "girls.luna.cutenya",
            "girls.luna.giggle",
            "girls.luna.happyoh",
            "girls.luna.hmph",
            "girls.luna.horninya",
            "girls.luna.huh",
            "girls.luna.lightbreathing",
            "girls.luna.mmm",
            "girls.luna.moan",
            "girls.luna.ouu",
            "girls.luna.owo",
            "girls.luna.sadoh",
            "girls.luna.sigh",
            "girls.luna.singing",
            "misc.bedrustle",
            "misc.beew",
            "misc.belljingle",
            "misc.clap",
            "misc.cuminflation",
            "misc.eat",
            "misc.fart",
            "misc.flap",
            "misc.inserts",
            "misc.jump",
            "misc.plob",
            "misc.pounding",
            "misc.pyro",
            "misc.scream",
            "misc.shatter",
            "misc.slap",
            "misc.slide",
            "misc.smallinserts",
            "misc.touch",
            "misc.weoweo"
    };

    static {
        for (String name : NAMES) {
            BY_NAME.put(name, SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(SexMod.id(name))));
        }
    }

    /** @return the sound event for a group such as "girls.jenny.giggle", or null if unknown. */
    public static SoundEvent get(String name) {
        DeferredHolder<SoundEvent, SoundEvent> holder = BY_NAME.get(name);
        return holder == null ? null : holder.get();
    }

    private ModSounds() {}
}
