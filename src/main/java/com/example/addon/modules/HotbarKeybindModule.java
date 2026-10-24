package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class HotbarKeybindModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Item> targetItem = sgGeneral.add(new ItemSetting.Builder()
        .name("target-item")
        .description("The item to switch to when the keybind is pressed.")
        .defaultValue(net.minecraft.item.Items.ENDER_PEARL)
        .build()
    );

    private final Setting<Boolean> searchInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("search-inventory")
        .description("If the item is in your main inventory, pull it into your hotbar while holding the key.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoUse = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-use")
        .description("Immediately use (right-click) the item when switched.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> targetHotbarSlot = sgGeneral.add(new IntSetting.Builder()
        .name("hotbar-slot")
        .description("The hotbar slot (1-9) to use for switching.")
        .defaultValue(1)
        .min(1)
        .max(9)
        .build()
    );

    private int previousSlot = -1;
    private int pulledFromSlot = -1;
    private boolean wasPressed = false;

    public HotbarKeybindModule() {
        super(AddonTemplate.CATEGORY, "hotbar-keybind-switcher", "Automatically switches to and uses a specified item when a keybind is held, returning it afterwards.");
    }

    @Override
    public void onDeactivate() {
        restoreOriginalSlot();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.interactionManager == null) return;

        // Check if the module's bind key is currently being held down
        boolean isKeyHeld = isActive() && mc.options.keyUse.isPressed(); // Or custom bind listener if integrated with Meteor KeybindSetting

        // For a dedicated keybind setting, you can also use KeybindSetting:
        // (If assigned to a key, we check if that key code is pressed)

        if (isKeyHeld && !wasPressed) {
            // Key just pressed down
            wasPressed = true;
            previousSlot = mc.player.getInventory().selectedSlot;
            
            int slotToSwitch = targetHotbarSlot.get() - 1;
            Item target = targetItem.get();

            // 1. Check if item is already in hotbar
            int hotbarSlot = findItemInHotbar(target);
            if (hotbarSlot != -1) {
                mc.player.getInventory().selectedSlot = hotbarSlot;
                pulledFromSlot = -1;
            } else if (searchInventory.get()) {
                // 2. Search main inventory and swap to target hotbar slot
                int invSlot = findItemInInventory(target);
                if (invSlot != -1) {
                    pulledFromSlot = invSlot;
                    // Swap inventory item into target hotbar slot
                    InvUtils.move().from(invSlot).toHotbar(slotToSwitch);
                    mc.player.getInventory().selectedSlot = slotToSwitch;
                }
            }

            if (autoUse.get() && mc.player.getMainHandStack().getItem() == target) {
                mc.interactionManager.interactItem(mc.player, mc.player.getActiveHand());
            }

        } else if (!isKeyHeld && wasPressed) {
            // Key released — restore previous state
            wasPressed = false;
            restoreOriginalSlot();
        }
    }

    private void restoreOriginalSlot() {
        if (mc.player == null) return;

        if (previousSlot != -1) {
            mc.player.getInventory().selectedSlot = previousSlot;
            previousSlot = -1;
        }

        // If we pulled an item from main inventory into the hotbar, swap it back
        if (pulledFromSlot != -1) {
            int hotbarSlot = targetHotbarSlot.get() - 1;
            InvUtils.move().fromHotbar(hotbarSlot).to(pulledFromSlot);
            pulledFromSlot = -1;
        }
    }

    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findItemInInventory(Item item) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}
