package com.example.rayzen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;

public class RayzenClient implements ClientModInitializer {
    private static boolean busy = false;

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (busy || !level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.gameMode == null || player != mc.player) {
                return InteractionResult.PASS;
            }

            BlockState state = level.getBlockState(hit.getBlockPos());
            if (!state.is(Blocks.RESPAWN_ANCHOR)) return InteractionResult.PASS;
            // Якорь уже полный (4/4) - обычное поведение
            if (state.getValue(RespawnAnchorBlock.CHARGE) >= 4) return InteractionResult.PASS;

            Inventory inv = mc.player.getInventory();
            int original = inv.getSelectedSlot();
            // Светокамень уже в руке - ванильное поведение
            if (inv.getItem(original).is(Items.GLOWSTONE)) return InteractionResult.PASS;

            int glow = -1;
            int sword = -1;
            for (int i = 0; i < 9; i++) {
                var stack = inv.getItem(i);
                if (glow == -1 && stack.is(Items.GLOWSTONE)) glow = i;
                if (sword == -1 && stack.is(ItemTags.SWORDS)) sword = i;
            }
            if (glow == -1) return InteractionResult.PASS; // нет светокамня в хотбаре
            if (sword == -1) sword = original;              // нет меча - вернуть прежний слот

            busy = true;
            try {
                inv.setSelectedSlot(glow);                  // 1. светокамень
                InteractionResult r = mc.gameMode.useItemOn(mc.player, hand, hit); // 2. +1 заряд
                if (r.consumesAction()) mc.player.swing(hand);
            } finally {
                inv.setSelectedSlot(sword);                 // 3. меч
                busy = false;
            }
            return InteractionResult.FAIL; // отменяем исходный клик, чтобы не было двойного
        });
    }
}
