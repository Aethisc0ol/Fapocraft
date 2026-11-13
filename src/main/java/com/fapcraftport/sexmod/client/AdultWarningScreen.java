package com.fapcraftport.sexmod.client;

import com.fapcraftport.sexmod.config.ClientConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 18+ gate shown before the title screen. Unlike the original, the under-18 button
 * only closes the game; it does not delete any files.
 */
public class AdultWarningScreen extends Screen {
    static boolean acceptedThisSession;

    private final Screen next;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private Checkbox dontAskAgain;

    public AdultWarningScreen(Screen next) {
        super(Component.translatable("window.pornwarning.title"));
        this.next = next;
    }

    @Override
    protected void init() {
        lines.clear();
        int textWidth = Math.min(360, width - 40);
        String raw = Component.translatable("window.pornwarning.text").getString();
        for (String paragraph : raw.split("<br>")) {
            lines.addAll(font.split(Component.literal(paragraph.trim()), textWidth));
        }
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.translatable("window.pornwarning.notice"), textWidth));

        int y = 50 + lines.size() * 10 + 10;
        dontAskAgain = addRenderableWidget(Checkbox.builder(Component.translatable("window.pornwarning.dontaskagain"), font)
                .pos(width / 2 - 75, y)
                .selected(false)
                .build());
        y += 28;
        addRenderableWidget(Button.builder(Component.translatable("window.pornwarning.am18"), b -> accept())
                .bounds(width / 2 - 155, y, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("window.pornwarning.not18"), b -> decline())
                .bounds(width / 2 + 5, y, 150, 20).build());
    }

    private void accept() {
        acceptedThisSession = true;
        if (dontAskAgain != null && dontAskAgain.selected()) {
            ClientConfig.SKIP_ADULT_WARNING.set(true);
            ClientConfig.SKIP_ADULT_WARNING.save();
        }
        if (minecraft != null) minecraft.setScreen(next);
    }

    private void decline() {
        if (minecraft != null) minecraft.stop();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 30, 0xFF5555);
        int y = 50;
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(font, line, width / 2, y, 0xFFFFFF);
            y += 10;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
