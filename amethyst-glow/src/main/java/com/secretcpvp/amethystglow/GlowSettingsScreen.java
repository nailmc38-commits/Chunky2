package com.secretcpvp.amethystglow;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;

public final class GlowSettingsScreen extends Screen {
    private int left, top;
    public GlowSettingsScreen() {
        super(Component.literal("Amethyst Glow"));
    }

    @Override
    protected void init() {
        GlowConfig cfg = AmethystGlow.CONFIG;
        left = (width - 318) / 2;
        top = (height - 285) / 2;
        int x = left + 14, w = 290;

        addButton(x, top + 56, w, "Glow: " + onOff(cfg.enabled), () -> {
            cfg.enabled = !cfg.enabled;
            changed();
        });
        addButton(x, top + 83, 140, "Blocks: " + onOff(cfg.showBlocks), () -> {
            cfg.showBlocks = !cfg.showBlocks;
            changed();
        });
        addButton(x + 150, top + 83, 140, "Crystals: " + onOff(cfg.showCrystals), () -> {
            cfg.showCrystals = !cfg.showCrystals;
            changed();
        });
        addRenderableWidget(new IntegerSlider(x, top + 120, w, "Search radius", 8, 48,
                cfg.radius, " blocks", value -> { cfg.radius = value; changed(); }));
        addRenderableWidget(new IntegerSlider(x, top + 153, w, "Purple opacity", 10, 80,
                cfg.opacity, "%", value -> { cfg.opacity = value; cfg.save(); }));
        addButton(x, top + 187, w, "HUD counter: " + onOff(cfg.showHud), () -> {
            cfg.showHud = !cfg.showHud; changed();
        });
        addButton(x, top + 242, w, "Done", this::onClose);
    }

    private static String onOff(boolean yes) { return yes ? "ON" : "OFF"; }

    private void changed() {
        AmethystGlow.CONFIG.save();
        AmethystGlow.SCAN.reset();
        rebuildWidgets();
    }

    private void addButton(int x, int y, int w, String text, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), ignored -> action.run())
                .bounds(x, y, w, 20).build());
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        renderBackground(g, mouseX, mouseY, partialTicks);
        g.fill(left - 2, top - 2, left + 320, top + 287, 0xE9080510);
        g.fill(left, top, left + 318, top + 285, 0xF0191424);
        g.fill(left, top, left + 318, top + 3, 0xFFBB70EE);
        g.drawString(font, "AMETHYST GLOW  /  VISUALS", left + 14, top + 15, 0xFFE9BEFF);
        g.drawString(font, "Fabric 1.21.11 - purple highlights", left + 14, top + 32, 0xFFBAB1C9);
        g.drawString(font, "H = toggle     F10 = settings", left + 14, top + 214, 0xFFD3A8F8);
        g.drawString(font, "Only scans blocks sent by the server.", left + 14, top + 227, 0xFFAAA3B8);
        super.render(g, mouseX, mouseY, partialTicks);
    }

    private static final class IntegerSlider extends AbstractSliderButton {
        private final String title, suffix;
        private final int min, max;
        private final IntConsumer apply;
        IntegerSlider(int x, int y, int width, String title, int min, int max,
                      int initial, String suffix, IntConsumer apply) {
            super(x, y, width, 20, Component.empty(), (double) (initial - min) / (max - min));
            this.title = title;
            this.suffix = suffix;
            this.min = min;
            this.max = max;
            this.apply = apply;
            updateMessage();
        }
        private int number() { return min + (int) Math.round(value * (max - min)); }
        @Override protected void updateMessage() {
            setMessage(Component.literal(title + ": " + number() + suffix));
        }
        @Override protected void applyValue() { apply.accept(number()); }
    }
}
