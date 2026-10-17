package com.example.rayzen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RayzenScreen extends Screen {
    private final Screen parent;

    public RayzenScreen(Screen parent) {
        super(Component.literal("Rayzen"));
        this.parent = parent;
    }

    private static String onOff(boolean v) { return v ? "ВКЛ" : "ВЫКЛ"; }
    private static String slot(int v) { return v == 0 ? "Авто" : String.valueOf(v); }

    @Override
    protected void init() {
        int w = 240, h = 20, x = this.width / 2 - w / 2, y = this.height / 4, gap = 26;

        addRenderableWidget(Button.builder(Component.literal("Мод: " + onOff(RayzenConfig.enabled)), b -> {
            RayzenConfig.enabled = !RayzenConfig.enabled;
            b.setMessage(Component.literal("Мод: " + onOff(RayzenConfig.enabled)));
        }).bounds(x, y, w, h).build());

        addRenderableWidget(Button.builder(
                Component.literal("Свап на меч при заряженном: " + onOff(RayzenConfig.swapIfCharged)), b -> {
            RayzenConfig.swapIfCharged = !RayzenConfig.swapIfCharged;
            b.setMessage(Component.literal("Свап на меч при заряженном: " + onOff(RayzenConfig.swapIfCharged)));
        }).bounds(x, y + gap, w, h).build());

        addRenderableWidget(Button.builder(Component.literal("Слот светокамня: " + slot(RayzenConfig.glowSlot)), b -> {
            RayzenConfig.glowSlot = (RayzenConfig.glowSlot + 1) % 10;
            b.setMessage(Component.literal("Слот светокамня: " + slot(RayzenConfig.glowSlot)));
        }).bounds(x, y + gap * 2, w, h).build());

        addRenderableWidget(Button.builder(Component.literal("Слот меча: " + slot(RayzenConfig.swordSlot)), b -> {
            RayzenConfig.swordSlot = (RayzenConfig.swordSlot + 1) % 10;
            b.setMessage(Component.literal("Слот меча: " + slot(RayzenConfig.swordSlot)));
        }).bounds(x, y + gap * 3, w, h).build());

        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(x, y + gap * 4 + 8, w, h).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        g.drawCenteredString(this.font,
                Component.literal("Клавиши: Настройки > Управление > Rayzen"),
                this.width / 2, this.height - 30, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        RayzenConfig.save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }
}
