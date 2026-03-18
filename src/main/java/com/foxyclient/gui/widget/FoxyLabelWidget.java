package com.foxyclient.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class FoxyLabelWidget extends ClickableWidget {
    private final TextRenderer textRenderer;

    public FoxyLabelWidget(TextRenderer textRenderer, int x, int y, String text) {
        super(x, y, textRenderer.getWidth(text), 10, Text.literal(text));
        this.textRenderer = textRenderer;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawTextWithShadow(textRenderer, getMessage(), getX(), getY(), 0xFFFFFFFF | ((int)(this.getAlpha() * 255) << 24));
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    }
}
