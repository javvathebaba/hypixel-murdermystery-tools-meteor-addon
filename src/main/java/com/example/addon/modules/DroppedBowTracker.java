package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DroppedBowTracker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // ==================== General Settings ====================
    private final Setting<Boolean> notifyChat = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-notification")
        .description("Alerts you in chat when a dropped detective bow appears.")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> chatFormat = sgGeneral.add(new StringSetting.Builder()
        .name("chat-message")
        .description("Custom chat alert when a bow is dropped. Use {item} as a placeholder and & for color codes.")
        .defaultValue("&6[Bow Tracker] &eA detective bow has been dropped!")
        .visible(notifyChat::get)
        .build()
    );

    // ==================== Render Settings ====================
    private final Setting<Boolean> tracers = sgRender.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Draws a tracer line to dropped detective bows.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> esp = sgRender.add(new BoolSetting.Builder()
        .name("esp")
        .description("Draws an ESP box around dropped detective bows.")
        .defaultValue(true)
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
        .defaultValue(new SettingColor(255, 215, 0, 40))
        .visible(() -> esp.get() && shapeMode.get().sides())
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The outline and tracer color.")
        .defaultValue(new SettingColor(255, 215, 0, 255))
        .build()
    );

    private final Set<UUID> notifiedBows = new HashSet<>();

    public DroppedBowTracker() {
        super(AddonTemplate.CATEGORY, "dropped-bow-tracker", "Tracks dropped Hypixel detective bow armor stands with configurable ESP, tracers, and chat alerts.");
    }

    @Override
    public void onActivate() {
        notifiedBows.clear();
    }

    @Override
    public void onDeactivate() {
        notifiedBows.clear();
    }

    private ItemStack getBowStack(ArmorStand armorStand) {
        if (armorStand.getMainHandItem().is(Items.BOW)) return armorStand.getMainHandItem();
        if (armorStand.getOffhandItem().is(Items.BOW)) return armorStand.getOffhandItem();
        ItemStack headStack = armorStand.getItemBySlot(EquipmentSlot.HEAD);
        if (headStack.is(Items.BOW)) return headStack;
        return null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand) {
                ItemStack bowStack = getBowStack(armorStand);
                if (bowStack != null) {
                    UUID uuid = armorStand.getUUID();
                    if (!notifiedBows.contains(uuid)) {
                        notifiedBows.add(uuid);
                        if (notifyChat.get()) {
                            sendCustomNotification(chatFormat.get(), bowStack);
                        }
                    }
                }
            }
        }
    }

    private void sendCustomNotification(String template, ItemStack stack) {
        String itemName = stack.getHoverName().getString();
        String formattedMessage = template
            .replace("{item}", itemName)
            .replace('&', '\u00a7');

        info(formattedMessage);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.level == null) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand) {
                if (getBowStack(armorStand) != null) {
                    double x = Mth.lerp(event.tickDelta, armorStand.xOld, armorStand.getX());
                    double y = Mth.lerp(event.tickDelta, armorStand.yOld, armorStand.getY());
                    double z = Mth.lerp(event.tickDelta, armorStand.zOld, armorStand.getZ());

                    if (tracers.get()) {
                        event.renderer.line(
                            RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z,
                            x, y + 0.5, z,
                            lineColor.get()
                        );
                    }

                    if (esp.get()) {
                        AABB box = armorStand.getBoundingBox();
                        double dx = x - armorStand.getX();
                        double dy = y - armorStand.getY();
                        double dz = z - armorStand.getZ();

                        event.renderer.box(
                            box.minX + dx, box.minY + dy, box.minZ + dz,
                            box.maxX + dx, box.maxY + dy, box.maxZ + dz,
                            sideColor.get(), lineColor.get(), shapeMode.get(), 0
                        );
                    }
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        int count = 0;
        if (mc.level != null) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof ArmorStand armorStand && getBowStack(armorStand) != null) {
                    count++;
                }
            }
        }
        return Integer.toString(count);
    }
}
