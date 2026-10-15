package com.secretcpvp.chunkfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import java.util.*;

public final class ChunkScanner {
    public record Finding(String name, BlockPos pos, int matches, int accentColor) {}
    private record ChunkResult(long scannedAt, List<Finding> findings) {}
    private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();
    private final Set<Long> queued = new HashSet<>();
    private final Map<Long, ChunkResult> cache = new HashMap<>();
    private ClientLevel previousLevel;
    private int centerX = Integer.MIN_VALUE, centerZ = Integer.MIN_VALUE;
    private int lastRadius = -1, lastSensitivity = -1;
    private boolean lastGeodes, lastActivity;
    private long ticks;
    private int checked;

    public void reset() {
        queue.clear(); queued.clear(); cache.clear();
        centerX = Integer.MIN_VALUE; centerZ = Integer.MIN_VALUE;
        lastRadius = -1; checked = 0;
    }
    public int checked() { return checked; }
    public int pending() { return queue.size(); }

    public void tick(Minecraft mc, FinderConfig c) {
        ticks++;
        if (mc.level == null || mc.player == null) {
            previousLevel = null; reset(); return;
        }
        if (!c.enabled) return;
        if (mc.level != previousLevel) { reset(); previousLevel = mc.level; }
        int cx = mc.player.blockPosition().getX() >> 4;
        int cz = mc.player.blockPosition().getZ() >> 4;
        boolean changed = cx != centerX || cz != centerZ || c.scanRadius != lastRadius
            || c.sensitivity != lastSensitivity || c.showGeodes != lastGeodes
            || c.showActivity != lastActivity;
        if (changed || (ticks % 240 == 0 && queue.isEmpty())) {
            if (c.sensitivity != lastSensitivity || c.showGeodes != lastGeodes
                    || c.showActivity != lastActivity) cache.clear();
            centerX = cx; centerZ = cz; lastRadius = c.scanRadius;
            lastSensitivity = c.sensitivity; lastGeodes = c.showGeodes;
            lastActivity = c.showActivity; rebuildQueue(c.scanRadius);
        }
        if (ticks % 3 != 0 || queue.isEmpty()) return;
        ChunkPos p = queue.removeFirst();
        queued.remove(p.toLong());
        LevelChunk chunk = mc.level.getChunkSource().getChunk(p.x, p.z, ChunkStatus.FULL, false);
        if (chunk == null) return;
        cache.put(p.toLong(), new ChunkResult(ticks, scanChunk(chunk, mc.level, c)));
        checked++;
    }
    private void rebuildQueue(int radius) {
        queue.clear(); queued.clear();
        cache.keySet().removeIf(k -> {
            ChunkPos p = new ChunkPos(k);
            return Math.abs(p.x - centerX) > radius || Math.abs(p.z - centerZ) > radius;
        });
        checked = 0;
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    ChunkPos p = new ChunkPos(centerX + dx, centerZ + dz);
                    ChunkResult old = cache.get(p.toLong());
                    if (old != null && ticks - old.scannedAt < 1200) continue;
                    if (queued.add(p.toLong())) queue.addLast(p);
                }
            }
        }
    }
    private static List<Finding> scanChunk(LevelChunk chunk, ClientLevel level, FinderConfig c) {
        int geodes = 0, activity = 0;
        BlockPos geoPos = null, tracePos = null;
        LevelChunkSection[] sections = chunk.getSections();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;
            int y0 = level.getSectionYFromSectionIndex(i) * 16;
            if (!section.maybeHas(s -> isGeo(s.getBlock()) || isActivity(s.getBlock()))) continue;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        Block b = state.getBlock();
                        if (c.showGeodes && isGeo(b)) {
                            geodes++;
                            if (geoPos == null || b == Blocks.BUDDING_AMETHYST)
                                geoPos = new BlockPos(x0 + x, y0 + y, z0 + z);
                        }
                        if (c.showActivity && isActivity(b)) {
                            activity++;
                            if (tracePos == null) tracePos = new BlockPos(x0 + x, y0 + y, z0 + z);
                        }
                    }
                }
            }
        }
        List<Finding> found = new ArrayList<>(2);
        int minGeo = 26 - c.sensitivity * 22 / 100;
        int minActivity = 9 - c.sensitivity * 8 / 100;
        if (c.showGeodes && geoPos != null && geodes >= minGeo)
            found.add(new Finding("Possible geode", geoPos, geodes, 0xFFD58CFF));
        if (c.showActivity && tracePos != null && activity >= minActivity)
            found.add(new Finding("Activity clue", tracePos, activity, 0xFFFFC57A));
        return found;
    }
    private static boolean isGeo(Block b) {
        return b == Blocks.AMETHYST_BLOCK || b == Blocks.BUDDING_AMETHYST ||
            b == Blocks.CALCITE || b == Blocks.AMETHYST_CLUSTER ||
            b == Blocks.LARGE_AMETHYST_BUD || b == Blocks.MEDIUM_AMETHYST_BUD ||
            b == Blocks.SMALL_AMETHYST_BUD;
    }
    private static boolean isActivity(Block b) {
        return b == Blocks.CRAFTING_TABLE || b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE
            || b == Blocks.SMOKER || b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST
            || b == Blocks.BARREL || b == Blocks.ENDER_CHEST || b == Blocks.HOPPER
            || b == Blocks.BEACON || b == Blocks.ENCHANTING_TABLE || b == Blocks.ANVIL
            || b == Blocks.CHIPPED_ANVIL || b == Blocks.DAMAGED_ANVIL
            || b == Blocks.REDSTONE_TORCH || b == Blocks.REPEATER || b == Blocks.COMPARATOR
            || b == Blocks.OAK_SIGN || b == Blocks.OAK_WALL_SIGN || b == Blocks.LECTERN
            || b == Blocks.SHULKER_BOX || b == Blocks.TORCH || b == Blocks.WALL_TORCH;
    }
    public List<Finding> nearest(Minecraft mc, FinderConfig c, int limit) {
        if (mc.player == null || mc.level == null || !c.enabled) return List.of();
        BlockPos pos = mc.player.blockPosition();
        List<Finding> found = new ArrayList<>();
        for (ChunkResult r : cache.values()) found.addAll(r.findings());
        found.sort(Comparator.comparingDouble(f -> f.pos().distSqr(pos)));
        return found.subList(0, Math.min(found.size(), limit));
    }
}
