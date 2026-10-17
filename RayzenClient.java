package com.example.rayzen;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

public class RayzenClient implements ClientModInitializer {
    private static boolean busy = false;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rayzen", "main"));
    private static KeyMapping toggleKey;
    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        RayzenConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.rayzen.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.rayzen.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggleKey.consumeClick()) {
                RayzenConfig.enabled = !RayzenConfig.enabled;
                RayzenConfig.save();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            Component.literal("Rayzen: " + (RayzenConfig.enabled ? "ВКЛ" : "ВЫКЛ")), true);
                }
            }
            while (menuKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new RayzenScreen(null));
            }
        });

        // /rayzen - включить/выключить мод
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("rayzen").executes(ctx -> {
                RayzenConfig.enabled = !RayzenConfig.enabled;
                RayzenConfig.save();
                ctx.getSource().sendFeedback(
                        Component.literal("Rayzen: " + (RayzenConfig.enabled ? "ВКЛ" : "ВЫКЛ")));
                return 1;
            })));

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!RayzenConfig.enabled || busy || !level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.gameMode == null || player != mc.player) {
                return InteractionResult.PASS;
            }

            BlockState state = level.getBlockState(hit.getBlockPos());
            if (!state.is(Blocks.RESPAWN_ANCHOR)) return InteractionResult.PASS;

            int charge = state.getValue(RespawnAnchorBlock.CHARGE);
            Inventory inv = mc.player.getInventory();
            int original = inv.getSelectedSlot();

            int sword = pickSword(inv);

            // Якорь уже заряжен: светокамень не добавляем, только свап на меч
            if (charge >= 1) {
                if (!RayzenConfig.swapIfCharged) return InteractionResult.PASS;
                if (sword == -1 || sword == original) return InteractionResult.PASS;
                inv.setSelectedSlot(sword);
                return InteractionResult.FAIL; // клик отменяем, взрыва нет
            }

            // Якорь пустой: светокамень -> +1 заряд -> меч
            int glow = inv.getItem(original).is(Items.GLOWSTONE)
                    ? original
                    : pickGlow(inv);
            if (glow == -1) return InteractionResult.PASS;
            if (sword == -1) sword = original;

            busy = true;
            try {
                inv.setSelectedSlot(glow);
                InteractionResult r = mc.gameMode.useItemOn(mc.player, hand, hit);
                if (r.consumesAction()) mc.player.swing(hand);
            } finally {
                inv.setSelectedSlot(sword);
                busy = false;
            }
            return InteractionResult.FAIL;
        });
    }

    private static int pickGlow(Inventory inv) {
        return pick(inv, RayzenConfig.glowSlot, s -> s.is(Items.GLOWSTONE), true);
    }

    private static int pickSword(Inventory inv) {
        return pick(inv, RayzenConfig.swordSlot, s -> s.is(ItemTags.SWORDS), false);
    }

    private static int pick(Inventory inv, int cfg, Predicate<ItemStack> test, boolean requireMatch) {
        if (cfg > 0) {
            int i = cfg - 1;
            if (!requireMatch || test.test(inv.getItem(i))) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (test.test(inv.getItem(i))) return i;
        }
        return -1;
    }
}
