package com.secretcpvp.amethystglow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.Reader;
import java.io.Writer;

public final class GlowConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("amethyst-glow.json");

    public boolean enabled = true;
    public boolean showBlocks = true;
    public boolean showCrystals = true;
    public boolean showHud = true;
    public int radius = 24;
    public int opacity = 35;

    public static GlowConfig load() {
        try {
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH)) {
                    GlowConfig result = GSON.fromJson(reader, GlowConfig.class);
                    if (result != null) { result.clamp(); return result; }
                }
            }
        } catch (Exception ex) {
            System.err.println("[Amethyst Glow] Config could not be read: " + ex);
        }
        return new GlowConfig();
    }

    public void clamp() {
        radius = Math.max(8, Math.min(48, radius));
        opacity = Math.max(10, Math.min(80, opacity));
    }

    public void save() {
        clamp();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) { GSON.toJson(this, writer); }
        } catch (Exception ex) {
            System.err.println("[Amethyst Glow] Config could not be saved: " + ex);
        }
    }
}
