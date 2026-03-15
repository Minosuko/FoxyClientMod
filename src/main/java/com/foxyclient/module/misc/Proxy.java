package com.foxyclient.module.misc;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.StringSetting;

/**
 * Proxy - Route server connections through a SOCKS proxy.
 * Supports SOCKS4 and SOCKS5 with optional authentication.
 */
public class Proxy extends Module {
    public final ModeSetting type = addSetting(new ModeSetting("Type", "Proxy protocol", "SOCKS5", "SOCKS4", "SOCKS5"));
    public final StringSetting host = addSetting(new StringSetting("Host", "Proxy host address", "127.0.0.1"));
    public final NumberSetting port = addSetting(new NumberSetting("Port", "Proxy port", 1080, 1, 65535));
    public final BoolSetting auth = addSetting(new BoolSetting("Auth", "Enable authentication", false));
    public final StringSetting username = addSetting(new StringSetting("Username", "Proxy username", ""));
    public final StringSetting password = addSetting(new StringSetting("Password", "Proxy password", "", true));

    public Proxy() {
        super("Proxy", "Route connections through SOCKS proxy", Category.MISC);
    }

    @Override
    public void onEnable() {
        info("Proxy enabled: " + type.get() + " " + host.get() + ":" + port.get().intValue());
    }

    @Override
    public void onDisable() {
        info("Proxy disabled. Direct connection will be used.");
    }

    public java.net.Proxy getJavaProxy() {
        if (!isEnabled()) return java.net.Proxy.NO_PROXY;
        return new java.net.Proxy(
            java.net.Proxy.Type.SOCKS,
            new java.net.InetSocketAddress(host.get(), port.get().intValue())
        );
    }

    public boolean needsAuth() {
        return auth.get() && !username.get().isEmpty();
    }
}
