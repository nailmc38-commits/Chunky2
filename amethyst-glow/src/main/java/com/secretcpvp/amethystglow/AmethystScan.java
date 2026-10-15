package com.secretcpvp.amethystglow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;

public final class AmethystScan {
    private ClientLevel oldWorld;
    private int centerX, centerY, centerZ;
    private int radius;
    private int index = 0;
    private int width = 0;
    private boolean scanning;
    private int ticks = 0;
    private int finishedTick = -10000;
    private final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    private ArrayList<BlockPos> next = new ArrayList<>();
    private List<BlockPos> current = List.of();

    public List<BlockPos> positions() { return current; }
    public boolean scanning() { return scanning; }
    public int count() { return current.size(); }

    public void reset() {
        oldWorld = null;
        scanning = false;
        index = 0;
        current = List.of();
        next = new ArrayList<>();
        finishedTick = -10000;
    }

    private void begin(Minecraft mc, GlowConfig config) {
        centerX = mc.player.blockPosition().getX();
        centerY = mc.player.blockPosition().getY();
        centerZ = mc.player.blockPosition().getZ();
        radius = config.radius;
        width = radius * 2 + 1;
        index = 0;
        next = new ArrayList<>();
        scanning = true;
    }

    public void tick(Minecraft mc, GlowConfig config) {
        ticks++;
        if (mc.level == null || mc.player == null) {
            reset();
            return;
        }
        if (mc.level != oldWorld) {
            reset();
            oldWorld = mc.level;
        }
        if (!config.enabled || (!config.showBlocks && !config.showCrystals)) {
            current = List.of();
            scanning = false;
            return;
        }
        var player = mc.player.blockPosition();
        if (!scanning &&
                (ticks - finishedTick > 100
                        || Math.abs(centerX - player.getX()) > 4
                        || Math.abs(centerY - player.getY()) > 4
                        || Math.abs(centerZ - player.getZ()) > 4
                        || config.radius != radius)) {
            begin(mc, config);
        }
        if (!scanning) return;

        int performed = 0;
        int volume = width * width * width;
        int radSq = radius * radius;
        while (performed < 5000 && index < volume) {
            int t = index++;
            int dx = t % width - radius;
            t /= width;
            int dz = t % width - radius;
            int dy = t / width - radius;
            if (dx * dx + dy * dy + dz * dz > radSq) continue;
            performed++;
            int x = centerX + dx;
            int y = centerY + dy;
            int z = centerZ + dz;
            if (!mc.level.getChunkSource().hasChunk(x >> 4, z >> 4)) continue;
            mutable.set(x, y, z);
            if (mc.level.isOutsideBuildHeight(mutable)) continue;
            Block block = mc.level.getBlockState(mutable).getBlock();
            if ((config.showBlocks && isAmethystBlock(block))
                || (config.showCrystals && isAmethystCrystal(block))) {
                next.add(mutable.immutable());
            }
        }
        if (index >= volume) {
            current = List.copyOf(next);
            next = new ArrayList<>();
            scanning = false;
            finishedTick = ticks;
        }
    }

    private static boolean isAmethystBlock(Block block) {
        return block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST;
    }

    private static boolean isAmethystCrystal(Block block) {
        return block == Blocks.AMETHYST_CLUSTER || block == Blocks.LARGE_AMETHYST_BUD
            || block == Blocks.MEDIUM_AMETHYST_BUD || block == Blocks.SMALL_AMETHYST_BUD;
    }
}
