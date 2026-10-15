package com.secretcpvp.chunkfinder;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;

public final class ChunkFinderScreen extends Screen {
    private static final int ACCENT = 0xFFB96EF0;
    private int left, top;
    public ChunkFinderScreen() { super(Component.literal("Chunk Finder - Visuals")); }
    private void addButton(int x, int y, int w, String text, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(text), b -> {
            action.run();
            rebuildWidgets();
        }).bounds(x, y, w, 20).build());
    }
    @Override
    protected void init() {
        left = (width - 330) / 2;
        top = (height - 290) / 2;
        int x = left + 17;
        int w = 296;
        FinderConfig c = ChunkFinderClient.CONFIG;
        addButton(x, top + 58, w, "Chunk Finder: " + (c.enabled ? "ON" : "OFF"), () -> {
            c.enabled = !c.enabled; c.save(); ChunkFinderClient.SCANNER.reset();
        });
        addButton(x, top + 85, 143, "Geodes: " + (c.showGeodes ? "ON" : "OFF"), () -> {
            c.showGeodes = !c.showGeodes; c.save(); ChunkFinderClient.SCANNER.reset();
        });
        addButton(x + 153, top + 85, 143, "Activity: " + (c.showActivity ? "ON" : "OFF"), () -> {
            c.showActivity = !c.showActivity; c.save(); ChunkFinderClient.SCANNER.reset();
        });
        addRenderableWidget(new IntSlider(x, top + 118, w, "Sensitivity", 1, 100,
                c.sensitivity, val -> { c.sensitivity = val; c.save(); }));
        addRenderableWidget(new IntSlider(x, top + 150, w, "Scan distance", 2, 12,
                c.scanRadius, val -> { c.scanRadius = val; c.save(); }));
        addButton(x, top + 186, 143, "HUD: " + (c.hudEnabled ? "ON" : "OFF"), () -> {
            c.hudEnabled = !c.hudEnabled; c.save();
        });
        addButton(x + 153, top + 186, 143, "Rescan chunks", () -> ChunkFinderClient.SCANNER.reset());
        addButton(x, top + 250, w, "Done", this::onClose);
    }
    @Override
    public boolean isPauseScreen() { return false; }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(left - 2, top - 2, left + 332, top + 292, 0xD7080710);
        graphics.fill(left, top, left + 330, top + 290, 0xEE1C1828);
        graphics.fill(left, top, left + 330, top + 3, ACCENT);
        graphics.drawString(font, "VISUALS / CHUNK FINDER", left + 17, top + 15, 0xFFF1E9FF);
        graphics.drawString(font, "Fabric 1.21.11 - Client-side scanner", left + 17, top + 30, 0xFFB4AEC5);
        graphics.drawString(font, "Higher sensitivity detects smaller clusters", left + 17, top + 174, 0xFFB4AEC5);
        graphics.drawString(font, "Range only checks already-loaded chunks", left + 17, top + 218, 0xFFB4AEC5);
        graphics.drawString(font, "Activity clues do not prove a player base", left + 17, top + 231, 0xFFB4AEC5);
        super.render(graphics, mouseX, mouseY, delta);
    }
    private static final class IntSlider extends AbstractSliderButton {
        private final String name;
        private final int min, max;
        private final IntConsumer change;
        IntSlider(int x, int y, int width, String name, int min, int max, int current, IntConsumer change) {
            super(x, y, width, 20, Component.empty(), (double) (current - min) / (max - min));
            this.name = name; this.min = min; this.max = max; this.change = change;
            updateMessage();
        }
        private int intValue() { return min + (int) Math.round(value * (max - min)); }
        @Override
        protected void updateMessage() {
            setMessage(Component.literal(name + ": " + intValue() +
                (name.equals("Scan distance") ? " chunks" : "%")));
        }
        @Override
        protected void applyValue() { change.accept(intValue()); }
    }
}
