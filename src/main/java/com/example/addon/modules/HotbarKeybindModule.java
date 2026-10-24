package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class HotbarKeybindModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Keybind> switchKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("switch-key")
        .description("Hold this key to switch to the target item, and release to swap back.")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<Item> targetItem = sgGeneral.add(new ItemSetting.Builder()
        .name("target-item")
        .description("The item to switch to when the keybind is pressed.")
        .defaultValue(Items.ENDER_PEARL)
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
        .description("The hotbar slot (1-9) to temporarily move the inventory item into.")
        .defaultValue(1)
        .min(1)
        .max(9)
        .visible(searchInventory::get)
        .build()
    );

    private int previousSlot = -1;
    private int pulledFromSlot = -1;
    private boolean wasPressed = false;

    public HotbarKeybindModule() {
        super(AddonTemplate.CATEGORY, "hotbar-keybind-switcher", "Automatically switches to and uses a specified item while a keybind is held, returning it afterwards.");
    }

    @Override
    public void onDeactivate() {
        wasPressed = false;
        restoreOriginalSlot();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.gameMode == null || mc.screen != null) return;

        boolean isKeyHeld = switchKey.get().isPressed();

        if (isKeyHeld && !wasPressed) {
            wasPressed = true;
            previousSlot = mc.player.getInventory().getSelectedSlot();

            int slotToSwitch = targetHotbarSlot.get() - 1;
            Item target = targetItem.get();

            int hotbarSlot = findItemInHotbar(target);
            if (hotbarSlot != -1) {
                InvUtils.swap(hotbarSlot, false);
                pulledFromSlot = -1;
            } else if (searchInventory.get()) {
                int invSlot = findItemInInventory(target);
                if (invSlot != -1) {
                    pulledFromSlot = invSlot;
                    InvUtils.move().from(invSlot).toHotbar(slotToSwitch);
                    InvUtils.swap(slotToSwitch, false);
                }
            }

            if (autoUse.get() && mc.player.getMainHandItem().is(target)) {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }

        } else if (!isKeyHeld && wasPressed) {
            wasPressed = false;
            restoreOriginalSlot();
        }
    }

    private void restoreOriginalSlot() {
        if (mc.player == null) return;

        if (pulledFromSlot != -1) {
            int hotbarSlot = targetHotbarSlot.get() - 1;
            InvUtils.move().fromHotbar(hotbarSlot).to(pulledFromSlot);
            pulledFromSlot = -1;
        }

        if (previousSlot != -1) {
            InvUtils.swap(previousSlot, false);
            previousSlot = -1;
        }
    }

    private int findItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findItemInInventory(Item item) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) {
                return i;
            }
        }
        return -1;
    }
}
