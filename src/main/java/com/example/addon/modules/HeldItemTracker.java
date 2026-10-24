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
import net.minecraft.ChatFormatting;
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
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Item>> targetItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("target-items")
        .description("Items that trigger player tracking when held.")
        .build()
    );

    private final Setting<Boolean> notifyChat = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-notification")
        .description("Alerts you in chat when a player first holds a target item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Do not track yourself.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> tracers = sgRender.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draws a tracer line to flagged players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> esp = sgRender.add(new BoolSetting.Builder()
        .name("esp")
        .description("Draws an ESP box around flagged players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> wireframe = sgRender.add(new BoolSetting.Builder()
        .name("wireframe-model")
        .description("Renders a full 3D wireframe model instead of a bounding box.")
        .defaultValue(false)
        .visible(esp::get)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the ESP shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(esp::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side fill color of the ESP.")
        .defaultValue(new SettingColor(255, 50, 50, 40))
        .visible(() -> esp.get() && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The outline and tracer color.")
        .defaultValue(new SettingColor(255, 50, 50, 255))
        .build()
    );

    private final Set<UUID> flaggedPlayers = new HashSet<>();

    public HeldItemTracker() {
        super(AddonTemplate.CATEGORY, "held-item-tracker", "Flags players who hold specific items with ESP and tracers.");
    }

    @Override
    public void onActivate() {
        flaggedPlayers.clear();
    }

    @Override
    public void onDeactivate() {
        flaggedPlayers.clear();
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        flaggedPlayers.clear();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        flaggedPlayers.clear();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundRespawnPacket packet) {
            if (mc.level != null && !mc.level.dimension().equals(packet.commonPlayerSpawnInfo().dimension())) {
                flaggedPlayers.clear();
                if (notifyChat.get()) {
                    info("Switched dimensions — cleared tracked players.");
                }
            }
        }
    }

@EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null || mc.player == null) return;
        List<Item> items = targetItems.get();
        if (items.isEmpty()) return;

        for (Player player : mc.level.players()) {
            if (ignoreSelf.get() && player == mc.player) continue;
            if (flaggedPlayers.contains(player.getUUID())) continue;

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            ItemStack matchedStack = null;
            if (!mainHand.isEmpty() && items.contains(mainHand.getItem())) {
                matchedStack = mainHand;
            } else if (!offHand.isEmpty() && items.contains(offHand.getItem())) {
                matchedStack = offHand;
            }

            if (matchedStack != null) {
                flaggedPlayers.add(player.getUUID());
                if (notifyChat.get()) {
                    info("Player %s%s%s held %s%s%s!",
                        ChatFormatting.RED, player.getName().getString(), ChatFormatting.GRAY,
                        ChatFormatting.GOLD, matchedStack.getHoverName().getString(), ChatFormatting.GRAY
                    );
                }
            }
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.level == null || flaggedPlayers.isEmpty()) return;

        for (Player player : mc.level.players()) {
            if (!flaggedPlayers.contains(player.getUUID())) continue;

            double x = Mth.lerp(event.tickDelta, player.xOld, player.getX());
            double y = Mth.lerp(event.tickDelta, player.yOld, player.getY());
            double z = Mth.lerp(event.tickDelta, player.zOld, player.getZ());

            if (tracers.get()) {
                double targetY = y + player.getEyeHeight();
                event.renderer.line(
                    RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                    x, targetY, z,
                    lineColor.get()
                );
            }

            if (esp.get()) {
                if (wireframe.get()) {
                    WireframeEntityRenderer.render(
                        event, player, 1.0,
                        sideColor.get(), lineColor.get(), shapeMode.get()
                    );
                } else {
                    AABB box = player.getBoundingBox();
                    double dx = x - player.getX();
                    double dy = y - player.getY();
                    double dz = z - player.getZ();

                    event.renderer.box(
                        box.minX + dx, box.minY + dy, box.minZ + dz,
                        box.maxX + dx, box.maxY + dy, box.maxZ + dz,
                        sideColor.get(), lineColor.get(), shapeMode.get(), 0
                    );
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        return Integer.toString(flaggedPlayers.size());
    }
}
