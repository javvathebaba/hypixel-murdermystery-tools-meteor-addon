package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class HotbarKeybinds extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> targetItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("target-items")
        .description("Items to bring to your hotbar when the keybind is held.")
        .build()
    );

    private final Setting<Integer> hotbarSlot = sgGeneral.add(new IntSetting.Builder()
        .name("hotbar-slot")
        .description("The hotbar slot (1-9) to pull the item into.")
        .defaultValue(1)
        .min(1)
        .max(9)
        .build()
    );

    private final Setting<Boolean> searchInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("search-inventory")
        .description("Search main inventory if the item is not already in the hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ActionType> actionType = sgGeneral.add(new EnumSetting.Builder<ActionType>()
        .name("action")
        .description("What to do once the item is selected.")
        .defaultValue(ActionType.SelectOnly)
        .build()
    );

    private final Setting<Boolean> returnOnRelease = sgGeneral.add(new BoolSetting.Builder()
        .name("return-on-release")
        .description("Swap the item back to its original slot when the keybind is released.")
        .defaultValue(true)
        .build()
    );

    public enum ActionType {
        SelectOnly,
        RightClick,
        LeftClick
    }

    private int previousSlot = -1;
    private int originalItemSlot = -1;
    private boolean wasPressed = false;
    private boolean hasActuated = false;

    public HotbarKeybinds() {
        super(AddonTemplate.CATEGORY, "hotbar-keybinds", "Automatically equips and uses items from your hotbar or inventory via keybinds.");
    }

    @Override
    public void onDeactivate() {
        resetState();
    }

    private void resetState() {
        previousSlot = -1;
        originalItemSlot = -1;
        wasPressed = false;
        hasActuated = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;

        List<Item> items = targetItems.get();
        if (items.isEmpty()) return;

        // Meteor modules with keybinds assigned check if the module key is currently pressed
        boolean isKeyHeld = this.isActive(); // Or tied to keybind state if active

        if (isKeyHeld) {
            if (!wasPressed) {
                wasPressed = true;
                previousSlot = mc.player.getInventory().selectedSlot;

                // Find item in hotbar or inventory
                int foundSlot = findTargetSlot(items);
                if (foundSlot != -1) {
                    originalItemSlot = foundSlot;
                    int targetHotbarIndex = hotbarSlot.get() - 1;

                    if (foundSlot != targetHotbarIndex) {
                        InvUtils.move().from(foundSlot).toHotbar(targetHotbarIndex);
                        originalItemSlot = targetHotbarIndex;
                    }

                    mc.player.
