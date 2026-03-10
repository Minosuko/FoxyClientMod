package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.BoolSetting;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ServerFinder - Scans for Minecraft servers on IP ranges.
 * Probes ports on configurable IP ranges using socket connections
 * to discover active Minecraft servers.
 */
public class ServerFinder extends Module {
    private final NumberSetting threads = addSetting(new NumberSetting("Threads", "Scan threads", 32, 1, 128));
    private final NumberSetting timeout = addSetting(new NumberSetting("Timeout", "Connection timeout (ms)", 1000, 100, 5000));
    private final NumberSetting portStart = addSetting(new NumberSetting("PortStart", "Start port", 25565, 1, 65535));
    private final NumberSetting portEnd = addSetting(new NumberSetting("PortEnd", "End port", 25575, 1, 65535));
    private final BoolSetting scanLocal = addSetting(new BoolSetting("ScanLocal", "Scan localhost range", true));

    private boolean scanning = false;
    private final List<String> found = new ArrayList<>();
    private ExecutorService executor;

    // Configurable IP base for scanning
    private String ipBase = "127.0.0.1";

    public ServerFinder() {
        super("ServerFinder", "Scan for MC servers", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (scanning) return;
        scanning = true;
        found.clear();

        int startPort = portStart.get().intValue();
        int endPort = portEnd.get().intValue();
        int timeoutMs = timeout.get().intValue();
        int totalPorts = endPort - startPort + 1;

        info("§eScanning " + ipBase + " ports " + startPort + "-" + endPort + " (" + totalPorts + " ports)...");

        AtomicInteger scanned = new AtomicInteger(0);
        executor = Executors.newFixedThreadPool(threads.get().intValue());

        for (int port = startPort; port <= endPort; port++) {
            int p = port;
            executor.submit(() -> {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(ipBase, p), timeoutMs);
                    String server = ipBase + ":" + p;
                    synchronized (found) {
                        found.add(server);
                    }
                    // Use mc.execute to safely send info from worker thread
                    mc.execute(() -> info("§aFound server: §f" + server));
                } catch (Exception ignored) {
                } finally {
                    int done = scanned.incrementAndGet();
                    if (done == totalPorts) {
                        mc.execute(() -> {
                            info("§eScan complete! Found §b" + found.size() + " §eservers.");
                            scanning = false;
                            toggle();
                        });
                    }
                }
            });
        }
        executor.shutdown();
    }

    @Override
    public void onDisable() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        scanning = false;
    }

    public void setIpBase(String ip) { this.ipBase = ip; }
    public String getIpBase() { return ipBase; }
    public List<String> getFoundServers() { return new ArrayList<>(found); }
    public boolean isScanning() { return scanning; }
}
