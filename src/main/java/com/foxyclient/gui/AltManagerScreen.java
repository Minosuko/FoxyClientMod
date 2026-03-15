package com.foxyclient.gui;

import com.foxyclient.FoxyClient;
import com.foxyclient.util.AltAccount;
import com.foxyclient.util.AltManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Alt Manager screen — add, remove, and switch between alt accounts.
 */
public class AltManagerScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget usernameField;
    private int scrollOffset = 0;

    public AltManagerScreen(Screen parent) {
        super(Text.literal("Alt Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // Username input field
        usernameField = new TextFieldWidget(this.textRenderer, centerX - 100, 35, 200, 20, Text.literal("Username"));
        usernameField.setPlaceholder(Text.literal("Enter username..."));
        usernameField.setMaxLength(16);
        this.addDrawableChild(usernameField);

        // Add button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Alt"), button -> {
            String name = usernameField.getText().trim();
            if (!name.isEmpty()) {
                FoxyClient.INSTANCE.getAltManager().addAlt(name);
                usernameField.setText("");
            }
        }).dimensions(centerX - 100, 60, 95, 20).build());

        // Login button (login with typed name directly)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Quick Login"), button -> {
            String name = usernameField.getText().trim();
            if (!name.isEmpty()) {
                FoxyClient.INSTANCE.getAltManager().login(name);
            }
        }).dimensions(centerX + 5, 60, 95, 20).build());

        // Back button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
            this.client.setScreen(parent);
        }).dimensions(centerX - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, "\u00A7b\u00A7lAlt Manager", this.width / 2, 10, 0xFF00E5CC);

        // Current session info
        String current = "Current: \u00A7a" + this.client.getSession().getUsername();
        context.drawCenteredTextWithShadow(this.textRenderer, current, this.width / 2, 24, 0xFFFFFFFF);

        // Alt list
        AltManager altManager = FoxyClient.INSTANCE.getAltManager();
        List<AltAccount> alts = altManager.getAlts();

        int listY = 90;
        int centerX = this.width / 2;

        if (alts.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "\u00A77No alts saved", centerX, listY, 0xFF888888);
        } else {
            for (int i = scrollOffset; i < alts.size() && listY < this.height - 45; i++) {
                AltAccount alt = alts.get(i);
                boolean hover = mouseX >= centerX - 120 && mouseX <= centerX + 120 &&
                                mouseY >= listY - 2 && mouseY <= listY + 18;

                // Background
                context.fill(centerX - 120, listY - 2, centerX + 120, listY + 18, hover ? 0x66FFFFFF : 0x44000000);

                // Name
                context.drawTextWithShadow(this.textRenderer, "\u00A7f" + alt.getUsername(), centerX - 115, listY + 2, 0xFFFFFFFF);

                // Login button area
                context.fill(centerX + 60, listY, centerX + 100, listY + 16, 0xFF00AA88);
                context.drawCenteredTextWithShadow(this.textRenderer, "Login", centerX + 80, listY + 4, 0xFFFFFFFF);

                // Remove button area
                context.fill(centerX + 102, listY, centerX + 118, listY + 16, 0xFFAA3333);
                context.drawCenteredTextWithShadow(this.textRenderer, "X", centerX + 110, listY + 4, 0xFFFFFFFF);

                listY += 22;
            }
        }
    }
    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;

        AltManager altManager = FoxyClient.INSTANCE.getAltManager();
        List<AltAccount> alts = altManager.getAlts();
        int centerX = this.width / 2;
        int listY = 90;
        double mouseX = click.x();
        double mouseY = click.y();

        for (int i = scrollOffset; i < alts.size() && listY < this.height - 45; i++) {
            AltAccount alt = alts.get(i);

            // Login button
            if (mouseX >= centerX + 60 && mouseX <= centerX + 100 &&
                mouseY >= listY && mouseY <= listY + 16) {
                altManager.login(alt);
                return true;
            }

            // Remove button
            if (mouseX >= centerX + 102 && mouseX <= centerX + 118 &&
                mouseY >= listY && mouseY <= listY + 16) {
                altManager.removeAlt(alt.getUsername());
                return true;
            }

            listY += 22;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, FoxyClient.INSTANCE.getAltManager().getAlts().size() - 10);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
