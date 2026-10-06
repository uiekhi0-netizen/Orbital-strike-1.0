package com.example.orbitalstrike;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.mojang.brigadier.arguments.IntegerArgumentType;

public class OrbitalStrikeMod implements ModInitializer {
    public static final String MOD_ID = "orbitalstrike";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("orbital")
                .then(CommandManager.literal("nuke")
                    .then(CommandManager.literal("give")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    ItemStack rod = createOrbitalRod("nuke", "§c§l[Nuke Strike Caller]§r", amount);
                                    player.getInventory().insertStack(rod);
                                    player.sendMessage(Text.literal("§a[Orbital Strike] §fได้รับ Nuke Strike Caller จำนวน " + amount + " ชิ้น!"), false);
                                }
                                return 1;
                            }))))
                .then(CommandManager.literal("stab")
                    .then(CommandManager.literal("give")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    ItemStack rod = createOrbitalRod("stab", "§b§l[Stab Strike Caller]§r", amount);
                                    player.getInventory().insertStack(rod);
                                    player.sendMessage(Text.literal("§a[Orbital Strike] §fได้รับ Stab Strike Caller จำนวน " + amount + " ชิ้น!"), false);
                                }
                                return 1;
                            })))));
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(Items.FISHING_ROD)) {
                NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
                if (customData != null && customData.contains("OrbitalType")) {
                    if (!world.isClient) {
                        HitResult hit = player.raycast(120.0D, 0.0F, false);
                        if (hit.getType() == HitResult.Type.BLOCK) {
                            BlockPos target = ((BlockHitResult) hit).getBlockPos();
                            String type = customData.copyNbt().getString("OrbitalType");

                            if ("nuke".equals(type)) {
                                spawnNukeCircles(world, target);
                                player.sendMessage(Text.literal("§c§l[!] NUKE ORBITAL STRIKE INCOMING!"), true);
                            } else if ("stab".equals(type)) {
                                spawnStabPillar(world, target);
                                player.sendMessage(Text.literal("§b§l[!] STAB ORBITAL STRIKE INCOMING!"), true);
                            }

                            stack.decrement(1);
                        }
                    }
                    return TypedActionResult.success(stack);
                }
            }
            return TypedActionResult.pass(stack);
        });
    }

    private static ItemStack createOrbitalRod(String type, String displayName, int count) {
        ItemStack rod = new ItemStack(Items.FISHING_ROD, count);
        rod.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName));
        NbtCompound tag = new NbtCompound();
        tag.putString("OrbitalType", type);
        rod.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
        return rod;
    }

    private void spawnNukeCircles(World world, BlockPos center) {
        // เสก TNT ซ้อนกัน 10 วงรัศมี
        for (int ring = 1; ring <= 10; ring++) {
            int tntInRing = ring * 4;
            for (int i = 0; i < tntInRing; i++) {
                double angle = 2 * Math.PI * i / tntInRing;
                double x = center.getX() + 0.5 + ring * Math.cos(angle);
                double z = center.getZ() + 0.5 + ring * Math.sin(angle);
                TntEntity tnt = new TntEntity(world, x, center.getY() + 15, z, null);
                tnt.setFuse(40 + (ring * 2));
                world.spawnEntity(tnt);
            }
        }
    }

    private void spawnStabPillar(World world, BlockPos target) {
        // ระเบิดเสาแนวดิ่งจาก Y=120 ถึง Y=-58
        for (int y = 120; y >= -58; y -= 3) {
            TntEntity tnt = new TntEntity(world, target.getX() + 0.5, y, target.getZ() + 0.5, null);
            // ดีเลย์ไล่ระดับจากฟ้าลงสู่ใต้ดิน
            tnt.setFuse(20 + (120 - y) / 3);
            world.spawnEntity(tnt);
        }
    }
}
