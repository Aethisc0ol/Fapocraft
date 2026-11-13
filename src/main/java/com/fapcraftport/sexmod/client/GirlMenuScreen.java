package com.fapcraftport.sexmod.client;

import com.fapcraftport.sexmod.entity.GirlAction;
import com.fapcraftport.sexmod.entity.JennyEntity;
import com.fapcraftport.sexmod.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Replacement for the original radial action menu: a simple column of buttons. */
public class GirlMenuScreen extends Screen {
    private final JennyEntity jenny;

    public GirlMenuScreen(JennyEntity jenny) {
        super(jenny.getDisplayName());
        this.jenny = jenny;
    }

    @Override
    protected void init() {
        List<GirlAction> actions = new ArrayList<>();
        actions.add(jenny.isNude() ? GirlAction.DRESS_UP : GirlAction.STRIP);
        actions.add(GirlAction.BLOWJOB);
        actions.add(GirlAction.BOOBJOB);
        actions.add(GirlAction.DOGGY);
        actions.add(minecraft != null && minecraft.player != null && jenny.isFollowing(minecraft.player)
                ? GirlAction.STOP_FOLLOW : GirlAction.FOLLOW);
        actions.add(GirlAction.SET_HOME);
        actions.add(GirlAction.GO_HOME);

        int w = 180;
        int h = 20;
        int gap = 4;
        int top = (height - actions.size() * (h + gap)) / 2 + 10;
        for (int i = 0; i < actions.size(); i++) {
            GirlAction action = actions.get(i);
            addRenderableWidget(Button.builder(label(action), b -> choose(action))
                    .bounds((width - w) / 2, top + i * (h + gap), w, h)
                    .build());
        }
    }

    private static Component label(GirlAction action) {
        Component name = Component.translatable(action.langKey);
        if (!action.isScene()) return name;
        return Component.translatable("sexmod.menu.cost", name, action.price.getDescription(), action.priceCount);
    }

    private void choose(GirlAction action) {
        PacketDistributor.sendToServer(new ModNetwork.Action(jenny.getId(), action.ordinal()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = children().isEmpty() ? height / 2 : ((Button) children().get(0)).getY();
        graphics.drawCenteredString(font, title, width / 2, top - 16, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
