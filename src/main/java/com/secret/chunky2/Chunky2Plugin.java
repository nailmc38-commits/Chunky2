package com.secret.chunky2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Chunky2Plugin extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<ChunkKey> keptChunks = new HashSet<>();

    private int chunkRadius;
    private long scanInterval;
    private long saveInterval;
    private int trackedWolfCount;

    private BukkitTask scanTask;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        readSettings();

        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("chunkywolves") != null) {
            getCommand("chunkywolves").setExecutor(this);
        }

        restoreRememberedChunks();

        scanTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                refreshWolfChunks();
            }
        }, 1L, scanInterval);

        saveTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                saveRememberedChunks();
            }
        }, saveInterval, saveInterval);

        getLogger().info("Chunky2 enabled. Tamed wolves now keep chunks loaded. Radius: " + chunkRadius);
    }

    @Override
    public void onDisable() {
        if (scanTask != null) scanTask.cancel();
        if (saveTask != null) saveTask.cancel();
        saveRememberedChunks();
        keptChunks.clear();
    }

    private void readSettings() {
        chunkRadius = Math.max(0, Math.min(4, getConfig().getInt("chunk-radius", 1)));
        scanInterval = Math.max(10L, getConfig().getLong("scan-interval-ticks", 40L));
        saveInterval = Math.max(100L, getConfig().getLong("save-interval-ticks", 600L));
    }

    private void refreshWolfChunks() {
        Set<ChunkKey> wanted = new HashSet<>();
        int wolves = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Wolf)) continue;

                Wolf wolf = (Wolf) entity;
                if (!wolf.isTamed() || wolf.isDead()) continue;

                wolves++;
                int centerX = wolf.getLocation().getBlockX() >> 4;
                int centerZ = wolf.getLocation().getBlockZ() >> 4;

                for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                    for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                        int chunkX = centerX + dx;
                        int chunkZ = centerZ + dz;
                        ChunkKey key = new ChunkKey(world.getUID(), chunkX, chunkZ);
                        wanted.add(key);

                        if (!world.isChunkLoaded(chunkX, chunkZ)) {
                            world.loadChunk(chunkX, chunkZ, true);
                        }
                    }
                }
            }
        }

        keptChunks.clear();
        keptChunks.addAll(wanted);
        trackedWolfCount = wolves;
    }

    private void restoreRememberedChunks() {
        List<String> remembered = getConfig().getStringList("remembered-chunks");
        int restored = 0;

        for (String encoded : remembered) {
            ChunkKey key = ChunkKey.decode(encoded);
            if (key == null) continue;

            World world = Bukkit.getWorld(key.worldId);
            if (world == null) continue;

            keptChunks.add(key);
            if (!world.isChunkLoaded(key.x, key.z)) {
                world.loadChunk(key.x, key.z, true);
            }
            restored++;
        }

        if (restored > 0) {
            getLogger().info("Restored " + restored + " remembered wolf-loaded chunks.");
        }
    }

    private void saveRememberedChunks() {
        List<String> encoded = new ArrayList<>();
        for (ChunkKey key : keptChunks) {
            encoded.add(key.encode());
        }
        getConfig().set("remembered-chunks", encoded);
        saveConfig();
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        ChunkKey key = new ChunkKey(event.getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ());
        if (keptChunks.contains(key)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWolfTamed(EntityTameEvent event) {
        if (event.getEntity() instanceof Wolf) {
            scheduleRefresh();
        }
    }

    @EventHandler
    public void onWolfSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Wolf) {
            scheduleRefresh();
        }
    }

    @EventHandler
    public void onWolfDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wolf) {
            scheduleRefresh();
        }
    }

    private void scheduleRefresh() {
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                refreshWolfChunks();
            }
        }, 1L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(ChatColor.AQUA + "Chunky2" + ChatColor.GRAY + " | Tamed wolves: "
                    + ChatColor.GREEN + trackedWolfCount + ChatColor.GRAY + " | Kept chunks: "
                    + ChatColor.GREEN + keptChunks.size() + ChatColor.GRAY + " | Radius: "
                    + ChatColor.GREEN + chunkRadius);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            readSettings();

            if (scanTask != null) scanTask.cancel();
            if (saveTask != null) saveTask.cancel();

            refreshWolfChunks();

            scanTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    refreshWolfChunks();
                }
            }, scanInterval, scanInterval);

            saveTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    saveRememberedChunks();
                }
            }, saveInterval, saveInterval);

            sender.sendMessage(ChatColor.GREEN + "Chunky2 reloaded. Radius: " + chunkRadius);
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /chunkywolves [status|reload]");
        return true;
    }

    private static final class ChunkKey {
        private final UUID worldId;
        private final int x;
        private final int z;

        private ChunkKey(UUID worldId, int x, int z) {
            this.worldId = worldId;
            this.x = x;
            this.z = z;
        }

        private String encode() {
            return worldId.toString() + "|" + x + "|" + z;
        }

        private static ChunkKey decode(String value) {
            if (value == null) return null;
            String[] parts = value.split("\\|");
            if (parts.length != 3) return null;

            try {
                return new ChunkKey(UUID.fromString(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ChunkKey)) return false;
            ChunkKey key = (ChunkKey) other;
            return x == key.x && z == key.z && worldId.equals(key.worldId);
        }

        @Override
        public int hashCode() {
            int result = worldId.hashCode();
            result = 31 * result + x;
            result = 31 * result + z;
            return result;
        }
    }
}
