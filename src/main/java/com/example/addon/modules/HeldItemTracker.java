package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HeldItemTracker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgGroup1 = settings.createGroup("Item Group 1");
    private final SettingGroup sgGroup2 = settings.createGroup("Item Group 2");

    // ==================== General Settings ====================
    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Do not track yourself.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> notifyDimensionReset = sgGeneral.add(new BoolSetting.Builder()
        .name("notify-dimension-reset")
        .description("Sends a chat message when tracked players are cleared upon switching dimensions.")
        .defaultValue(false)
        .build()
    );

    // ==================== Group 1 Settings ====================
    private final Setting<List<Item>> targetItems1 = sgGroup1.add(new ItemListSetting.Builder()
        .name("group-1-items")
        .description("First set of items that trigger player tracking when held.")
        .build()
    );

    private final Setting<Boolean> notifyChat1 = sgGroup1.add(new BoolSetting.Builder()
        .name("group-1-chat-notify")
        .description("Alerts you in chat when a player first holds a Group 1 item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> chatFormat1 = sgGroup1.add(new StringSetting.Builder()
        .name("group-1-chat-message")
        .description("Custom chat alert for Group 1. Use {player} and {item} as placeholders, and & for color codes.")
        .defaultValue("&c[Group 1] &f{player} &7held &6{item}&7!")
        .visible(notifyChat1::get)
        .build()
    );

    private final Setting<Boolean> tracers1 = sgGroup1.add(new BoolSetting.Builder()
        .name("group-1-tracers")
        .description("Draws a tracer line to Group 1 players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> esp1 = sgGroup1.add(new BoolSetting.Builder()
        .name("group-1-esp")
        .description("Draws an ESP box around Group 1 players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> wireframe1 = sgGroup1.add(new BoolSetting.Builder()
        .name("group-1-wireframe")
        .description("Renders a full 3D wireframe model for Group 1 instead of a bounding box.")
        .defaultValue(false)
        .visible(esp1::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode1 = sgGroup1.add(new EnumSetting.Builder<ShapeMode>()
        .name("group-1-shape-mode")
        .description("How Group 1 ESP shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(esp1::get)
        .build()
    );

    private final Setting<SettingColor> sideColor1 = sgGroup1.add(new ColorSetting.Builder()
        .name("group-1-side-color")
        .description("The side fill color of Group 1 ESP.")
        .defaultValue(new SettingColor(255, 50, 50, 40))
        .visible(() -> esp1.get() && shapeMode1.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor1 = sgGroup1.add(new ColorSetting.Builder()
        .name("group-1-line-color")
        .description("The outline and tracer color for Group 1.")
        .defaultValue(new SettingColor(255, 50, 50, 255))
        .build()
    );

    // ==================== Group 2 Settings ====================
    private final Setting<List<Item>> targetItems2 = sgGroup2.add(new ItemListSetting.Builder()
        .name("group-2-items")
        .description("Second set of items that trigger player tracking when held.")
        .build()
    );

    private final Setting<Boolean> notifyChat2 = sgGroup2.add(new BoolSetting.Builder()
        .name("group-2-chat-notify")
        .description("Alerts you in chat when a player first holds a Group 2 item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> chatFormat2 = sgGroup2.add(new StringSetting.Builder()
        .name("group-2-chat-message")
        .description("Custom chat alert for Group 2. Use {player} and {item} as placeholders, and & for color codes.")
        .defaultValue("&b[Group 2] &f{player} &7held &e{item}&7!")
        .visible(notifyChat2::get)
        .build()
    );

    private final Setting<Boolean> tracers2 = sgGroup2.add(new BoolSetting.Builder()
        .name("group-2-tracers")
        .description("Draws a tracer line to Group 2 players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> esp2 = sgGroup2.add(new BoolSetting.Builder()
        .name("group-2-esp")
        .description("Draws an ESP box around Group 2 players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> wireframe2 = sgGroup2.add(new BoolSetting.Builder()
        .name("group-2-wireframe")
        .description("Renders a full 3D wireframe model for Group 2 instead of a bounding box.")
