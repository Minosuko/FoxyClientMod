package com.foxyclient.gui;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.misc.Proxy;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Proxy Manager screen — configure SOCKS proxy settings.
 */
public class ProxyScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget hostField;
    private TextFieldWidget portField;
    private TextFieldWidget usernameField;
    private TextFieldWidget passwordField;

    public ProxyScreen(Screen parent) {
        super(Text.literal("Proxy Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        Proxy proxyModule = FoxyClient.INSTANCE.getModuleManager().getModule(Proxy.class);
        if (proxyModule == null) return;

        int y = 50;

        // Host field
        hostField = new TextFieldWidget(this.textRenderer, centerX - 100, y, 200, 20, Text.literal("Host"));
        hostField.setText(proxyModule.host.get());
        hostField.setPlaceholder(Text.literal("Proxy host (e.g. 127.0.0.1)"));
        this.addDrawableChild(hostField);
        y += 28;

        // Port field
        portField = new TextFieldWidget(this.textRenderer, centerX - 100, y, 200, 20, Text.literal("Port"));
        portField.setText(String.valueOf(proxyModule.port.get().intValue()));
        portField.setPlaceholder(Text.literal("Port (e.g. 1080)"));
        this.addDrawableChild(portField);
        y += 28;

        // Username field
        usernameField = new TextFieldWidget(this.textRenderer, centerX - 100, y, 200, 20, Text.literal("Username"));
        usernameField.setText(proxyModule.username.get());
        usernameField.setPlaceholder(Text.literal("Username (optional)"));
        this.addDrawableChild(usernameField);
        y += 28;

        // Password field
        passwordField = new TextFieldWidget(this.textRenderer, centerX - 100, y, 200, 20, Text.literal("Password"));
        passwordField.setText(proxyModule.password.get());
        passwordField.setPlaceholder(Text.literal("Password (optional)"));
        this.addDrawableChild(passwordField);
        y += 28;

        // Type toggle button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Type: " + proxyModule.type.get()), button -> {
            proxyModule.type.cycle();
            button.setMessage(Text.literal("Type: " + proxyModule.type.get()));
        }).dimensions(centerX - 100, y, 95, 20).build());

        // Auth toggle
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Auth: " + (proxyModule.auth.get() ? "ON" : "OFF")), button -> {
            proxyModule.auth.set(!proxyModule.auth.get());
            button.setMessage(Text.literal("Auth: " + (proxyModule.auth.get() ? "ON" : "OFF")));
        }).dimensions(centerX + 5, y, 95, 20).build());
        y += 28;

        // Enable/Disable toggle
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(proxyModule.isEnabled() ? "\u00A7aProxy: ENABLED" : "\u00A7cProxy: DISABLED"),
            button -> {
                proxyModule.toggle();
                button.setMessage(Text.literal(proxyModule.isEnabled() ? "\u00A7aProxy: ENABLED" : "\u00A7cProxy: DISABLED"));
            }
        ).dimensions(centerX - 100, y, 200, 20).build());
        y += 28;

        // Save & Close
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"), button -> {
            applySettings(proxyModule);
            this.client.setScreen(parent);
        }).dimensions(centerX - 100, y, 200, 20).build());

        // Back button at bottom
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> {
            this.client.setScreen(parent);
        }).dimensions(centerX - 100, this.height - 30, 200, 20).build());
    }

    private void applySettings(Proxy proxyModule) {
        proxyModule.host.set(hostField.getText().trim());
        try {
            int port = Integer.parseInt(portField.getText().trim());
            proxyModule.port.set((double) port);
        } catch (NumberFormatException ignored) {}
        proxyModule.username.set(usernameField.getText().trim());
        proxyModule.password.set(passwordField.getText().trim());
        FoxyClient.INSTANCE.getModuleManager().saveConfig();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, "\u00A7b\u00A7lProxy Manager", centerX, 10, 0xFF00E5CC);

        // Labels
        int y = 42;
        context.drawTextWithShadow(this.textRenderer, "\u00A77Host:", centerX - 100, y, 0xFFAAAAAA);
        y += 28;
        context.drawTextWithShadow(this.textRenderer, "\u00A77Port:", centerX - 100, y, 0xFFAAAAAA);
        y += 28;
        context.drawTextWithShadow(this.textRenderer, "\u00A77Username:", centerX - 100, y, 0xFFAAAAAA);
        y += 28;
        context.drawTextWithShadow(this.textRenderer, "\u00A77Password:", centerX - 100, y, 0xFFAAAAAA);
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
