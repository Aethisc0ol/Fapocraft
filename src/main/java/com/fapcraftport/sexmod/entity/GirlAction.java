package com.fapcraftport.sexmod.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Menu actions. Prices come from the original renderer, which drew these
 * items in Jenny's hand during the payment animation.
 */
public enum GirlAction {
    STRIP("action.names.strip", Items.GOLD_INGOT, 1, "jenny.dialogue.showBobsandveganapls"),
    BLOWJOB("action.names.blowjob", Items.EMERALD, 3, "jenny.dialogue.giveblowjob"),
    BOOBJOB("action.names.boobjob", Items.ENDER_PEARL, 2, "jenny.dialogue.givebooba"),
    DOGGY("action.names.doggy", Items.DIAMOND, 2, "jenny.dialogue.givesex"),
    DRESS_UP("action.names.dressup", null, 0, null),
    FOLLOW("action.names.followme", null, 0, null),
    STOP_FOLLOW("action.names.stopfollowme", null, 0, null),
    SET_HOME("action.names.setnewhome", null, 0, null),
    GO_HOME("action.names.gohome", null, 0, null);

    public final String langKey;
    public final Item price;
    public final int priceCount;
    /** What the player says when paying (shown as "&lt;player&gt; ..."). */
    public final String requestLine;

    GirlAction(String langKey, Item price, int priceCount, String requestLine) {
        this.langKey = langKey;
        this.price = price;
        this.priceCount = priceCount;
        this.requestLine = requestLine;
    }

    public boolean isScene() {
        return price != null;
    }

    private static final GirlAction[] VALUES = values();

    public static GirlAction byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : null;
    }
}
