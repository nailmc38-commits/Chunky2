package com.secretcpvp.chunkfinder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import java.util.List;

public final class ChunkFinderClient implements ClientModInitializer {
    public static final FinderConfig CONFIG = FinderConfig.load();
    public static final ChunkScanner SCANNER = new ChunkScanner();
    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("chunkfinder", "visuals"));
        KeyMapping menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.chunkfinder.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_F9, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.consumeClick()) client.setScreen(new ChunkFinderScreen());
            SCANNER.tick(client, CONFIG);
        });
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("chunkfinder", "scan_overlay"),
                (graphics, tickCounter) -> drawHud(graphics));
    }
    private static void drawHud(GuiGraphics gg) {
        Minecraft mc = Minecraft.getInstance();
        if (!CONFIG.enabled || !CONFIG.hudEnabled || mc.player == null || mc.level == null
                || mc.options.hideGui || mc.screen != null) return;
        List<ChunkScanner.Finding> findings = SCANNER.nearest(mc, CONFIG, 6);
        int x = gg.guiWidth() - 206;
        int y = 12;
        int h = 35 + findings.size() * 13;
        gg.fill(x - 8, y - 7, x + 198, y + h, 0xCE151322);
        gg.fill(x - 8, y - 7, x + 198, y - 5, 0xFFB864F1);
        gg.drawString(mc.font, "CHUNK FINDER", x, y, 0xFFDDA3FF);
        gg.drawString(mc.font, "R " + CONFIG.scanRadius + " chunks | Scan " + SCANNER.checked()
                + " | Queue " + SCANNER.pending(), x, y + 12, 0xFFB4AEC5);
        if (findings.isEmpty()) {
            gg.drawString(mc.font, "No matches in scanned chunks", x, y + 25, 0xFF898396);
        } else {
            for (int i = 0; i < findings.size(); i++) {
                ChunkScanner.Finding found = findings.get(i);
                int dx = found.pos().getX() - mc.player.blockPosition().getX();
                int dz = found.pos().getZ() - mc.player.blockPosition().getZ();
                int distance = (int) Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
                gg.drawString(mc.font, found.name() + " " + distance + "m [" +
                    found.pos().getX() + ", " + found.pos().getZ() + "]",
                    x, y + 25 + i * 13, found.accentColor());
            }
        }
    }
}
