package com.secretcpvp.amethystglow;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public final class AmethystGlow implements ClientModInitializer {
    public static final GlowConfig CONFIG = GlowConfig.load();
    public static final AmethystScan SCAN = new AmethystScan();

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("amethystglow", "visuals"));
        KeyMapping toggle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.amethystglow.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_H, category));
        KeyMapping settings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.amethystglow.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_F10, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggle.consumeClick()) {
                CONFIG.enabled = !CONFIG.enabled;
                CONFIG.save();
                SCAN.reset();
            }
            while (settings.consumeClick()) client.setScreen(new GlowSettingsScreen());
            SCAN.tick(client, CONFIG);
        });
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("amethystglow", "nearby_counter"),
                (graphics, delta) -> hud(graphics));

        GlowRenderer renderer = new GlowRenderer();
        renderer.register();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private static void hud(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!CONFIG.showHud || !CONFIG.enabled || mc.level == null || mc.player == null
                || mc.options.hideGui || mc.screen != null) return;
        int x = graphics.guiWidth() - 196;
        int y = 44;
        graphics.fill(x - 7, y - 6, x + 187, y + 25, 0xC9141020);
        graphics.fill(x - 7, y - 6, x + 187, y - 4, 0xFFBD75EB);
        graphics.drawString(mc.font, "AMETHYST GLOW", x, y, 0xFFE3B4FF);
        graphics.drawString(mc.font, SCAN.count() + " amethyst blocks  |  " +
                CONFIG.radius + "m" + (SCAN.scanning() ? "  scanning..." : ""), x,
                y + 12, 0xFFC8BDD4);
    }
}
