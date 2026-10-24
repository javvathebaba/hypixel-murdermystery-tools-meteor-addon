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
        .defaultValue(false)
        .visible(esp2::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode2 = sgGroup2.add(new EnumSetting.Builder<ShapeMode>()
        .name("group-2-shape-mode")
        .description("How Group 2 ESP shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(esp2::get)
        .build()
    );

    private final Setting<SettingColor> sideColor2 = sgGroup2.add(new ColorSetting.Builder()
        .name("group-2-side-color")
        .description("The side fill color of Group 2 ESP.")
        .defaultValue(new SettingColor(50, 200, 255, 40))
        .visible(() -> esp2.get() && shapeMode2.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor2 = sgGroup2.add(new ColorSetting.Builder()
        .name("group-2-line-color")
        .description("The outline and tracer color for Group 2.")
        .defaultValue(new SettingColor(50, 200, 255, 255))
        .build()
    );

    // Tracked player UUID sets per group
    private final Set<UUID> flaggedGroup1 = new HashSet<>();
    private final Set<UUID> flaggedGroup2 = new HashSet<>();

    public HeldItemTracker() {
        super(AddonTemplate.CATEGORY, "held-item-tracker", "Flags players holding configurable item sets with separate ESP, tracers, and custom chat alerts.");
    }

    private void clearTrackedPlayers() {
        flaggedGroup1.clear();
        flaggedGroup2.clear();
    }

    @Override
    public void onActivate() {
        clearTrackedPlayers();
    }

    @Override
    public void onDeactivate() {
        clearTrackedPlayers();
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        clearTrackedPlayers();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        clearTrackedPlayers();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundRespawnPacket packet) {
            if (mc.level != null && !mc.level.dimension().equals(packet.commonPlayerSpawnInfo().dimension())) {
                clearTrackedPlayers();
                if (notifyDimensionReset.get()) {
                    info("Switched dimensions — cleared tracked players.");
                }
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null) return;

        List<Item> items1 = targetItems1.get();
        List<Item> items2 = targetItems2.get();
        if (items1.isEmpty() && items2.isEmpty()) return;

        for (Player player : mc.level.players()) {
            if (ignoreSelf.get() && player == mc.player) continue;

            UUID uuid = player.getUUID();
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            // Check Group 1
            if (!items1.isEmpty() && !flaggedGroup1.contains(uuid)) {
                ItemStack matched1 = getMatchedStack(mainHand, offHand, items1);
                if (matched1 != null) {
                    flaggedGroup1.add(uuid);
                    if (notifyChat1.get()) {
                        sendCustomNotification(chatFormat1.get(), player, matched1);
                    }
                }
            }

            // Check Group 2
            if (!items2.isEmpty() && !flaggedGroup2.contains(uuid)) {
                ItemStack matched2 = getMatchedStack(mainHand, offHand, items2);
                if (matched2 != null) {
                    flaggedGroup2.add(uuid);
                    if (notifyChat2.get()) {
                        sendCustomNotification(chatFormat2.get(), player, matched2);
                    }
                }
            }
        }
    }

    private ItemStack getMatchedStack(ItemStack mainHand, ItemStack offHand, List<Item> targetList) {
        if (!mainHand.isEmpty() && targetList.contains(mainHand.getItem())) {
            return mainHand;
        }
        if (!offHand.isEmpty() && targetList.contains(offHand.getItem())) {
            return offHand;
        }
        return null;
    }

    private void sendCustomNotification(String template, Player player, ItemStack stack) {
        String playerName = player.getName().getString();
        String itemName = stack.getHoverName().getString();

        String formattedMessage = template
            .replace("{player}", playerName)
            .replace("{item}", itemName)
            .replace('&', '\u00a7');

        info(formattedMessage);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.level == null || (flaggedGroup1.isEmpty() && flaggedGroup2.isEmpty())) return;

        for (Player player : mc.level.players()) {
            UUID uuid = player.getUUID();
            boolean inGroup1 = flaggedGroup1.contains(uuid);
            boolean inGroup2 = flaggedGroup2.contains(uuid);

            if (!inGroup1 && !inGroup2) continue;

            double x = Mth.lerp(event.tickDelta, player.xOld, player.getX());
            double y = Mth.lerp(event.tickDelta, player.yOld, player.getY());
            double z = Mth.lerp(event.tickDelta, player.zOld, player.getZ());

            // If a player is in Group 1, render Group 1 visuals; otherwise render Group 2 visuals
            if (inGroup1) {
                renderPlayerVisuals(
                    event, player, x, y, z,
                    tracers1.get(), esp1.get(), wireframe1.get(),
                    shapeMode1.get(), sideColor1.get(), lineColor1.get()
                );
            } else {
                renderPlayerVisuals(
                    event, player, x, y, z,
                    tracers2.get(), esp2.get(), wireframe2.get(),
                    shapeMode2.get(), sideColor2.get(), lineColor2.get()
                );
            }
        }
    }

    private void renderPlayerVisuals(
        Render3DEvent event, Player player,
        double x, double y, double z,
        boolean drawTracer, boolean drawEsp, boolean useWireframe,
        ShapeMode mode, SettingColor sideCol, SettingColor lineCol
    ) {
        if (drawTracer) {
            double targetY = y + player.getEyeHeight();
            event.renderer.line(
                RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                x, targetY, z,
                lineCol
            );
        }

        if (drawEsp) {
            if (useWireframe) {
                WireframeEntityRenderer.render(
                    event, player, 1.0,
                    sideCol, lineCol, mode
                );
            } else {
                AABB box = player.getBoundingBox();
                double dx = x - player.getX();
                double dy = y - player.getY();
                double dz = z - player.getZ();

                event.renderer.box(
                    box.minX + dx, box.minY + dy, box.minZ + dz,
                    box.maxX + dx, box.maxY + dy, box.maxZ + dz,
                    sideCol, lineCol, mode, 0
                );
            }
        }
    }

    @Override
    public String getInfoString() {
        return flaggedGroup1.size() + " | " + flaggedGroup2.size();
    }
}
