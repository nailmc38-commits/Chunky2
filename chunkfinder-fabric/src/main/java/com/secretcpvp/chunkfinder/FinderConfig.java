package com.secretcpvp.chunkfinder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.io.*;
public final class FinderConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("chunkfinder.json");
  public boolean enabled = true, showGeodes = true, showActivity = true, hudEnabled = true;
  public int sensitivity = 65, scanRadius = 6;
  public static FinderConfig load() {
    try(Reader r = Files.newBufferedReader(PATH)) {
      FinderConfig config = GSON.fromJson(r, FinderConfig.class);
      if(config != null) return config;
    } catch (Exception ignored) {}
    return new FinderConfig();
  }
  public void save() {
    scanRadius = Math.max(2, Math.min(12, scanRadius));
    sensitivity = Math.max(1, Math.min(100, sensitivity));
    try {
      Files.createDirectories(PATH.getParent());
      try(Writer w = Files.newBufferedWriter(PATH)) { GSON.toJson(this, w); }
    } catch(Exception ex) { System.err.println("[ChunkFinder] " + ex); }
  }
}