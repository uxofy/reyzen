package com.example.rayzen;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class RayzenConfig {
    public static boolean enabled = true;
    public static boolean swapIfCharged = true;
    public static int glowSlot = 0;   // 0 = авто, 1..9 = слот хотбара
    public static int swordSlot = 0;  // 0 = авто, 1..9 = слот хотбара

    private RayzenConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("rayzen.properties");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (var in = Files.newInputStream(f)) { p.load(in); }
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            swapIfCharged = Boolean.parseBoolean(p.getProperty("swapIfCharged", "true"));
            glowSlot = clamp(Integer.parseInt(p.getProperty("glowSlot", "0")));
            swordSlot = clamp(Integer.parseInt(p.getProperty("swordSlot", "0")));
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("enabled", String.valueOf(enabled));
            p.setProperty("swapIfCharged", String.valueOf(swapIfCharged));
            p.setProperty("glowSlot", String.valueOf(glowSlot));
            p.setProperty("swordSlot", String.valueOf(swordSlot));
            try (var out = Files.newOutputStream(file())) { p.store(out, "Rayzen"); }
        } catch (Exception ignored) {
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(9, v));
    }
}
