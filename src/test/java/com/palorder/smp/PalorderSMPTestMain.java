package com.palorder.smp;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.palorder.smp.client.PalorderSMPMainClientJava;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
@Mod("palordersmp")
public class PalorderSMPTestMain {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "palordersmp");
    public static final RegistryObject<Item> REVIVAL_ITEM = ITEMS.register("revival_item", () -> new Item(new Item.Properties()));
    private static final UUID OWNER_UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c");
    public static final KeyMapping OPEN_OWNER_PANEL_KEY = new KeyMapping(
            "key.palordersmp.open_owner_panel",
            GLFW.GLFW_KEY_O, "key.categories.palordersmp"
    );

    private final Map<UUID, Long> deathBans = new HashMap<>();
    private final Map<UUID, Boolean> immortalityToggles = new HashMap<>();

    public PalorderSMPTestMain() {
        MinecraftForge.EVENT_BUS.register(this);
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();

        // Register commands
        registerCommands(dispatcher);
    }


    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nuke")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (CommandSyntaxException e) {
                        source.sendFailure(new TextComponent("Command error: " + e.getMessage()));
                        return false;
                    }
                })
                .executes(context -> {
                    spawnTNTNuke(context.getSource().getPlayerOrException());
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("undeathban")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (CommandSyntaxException e) {
                        source.sendFailure(new TextComponent("Command error: " + e.getMessage()));
                        return false;
                    }
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
                            return undeathbanPlayer(context.getSource(), targetPlayer);
                        })
                )
        );
    }

    private int undeathbanPlayer(CommandSourceStack source, ServerPlayer targetPlayer) {
        UUID targetUUID = targetPlayer.getUUID();
        if (deathBans.containsKey(targetUUID)) {
            deathBans.remove(targetUUID);
            source.sendSuccess(new TextComponent("Successfully undeathbanned " + targetPlayer.getName().getString()), true);
        } else {
            source.sendFailure(new TextComponent("Player is not deathbanned."));
        }
        return 1;
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer().getUUID().equals(OWNER_UUID)) {
            event.getPlayer().sendMessage(new TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."), event.getPlayer().getUUID());

        }
    }

    private void spawnTNTNuke(ServerPlayer player) {
        ServerLevel world = player.getLevel();
        Vec3 lookVec = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 targetPos = eyePos.add(lookVec.scale(100));
        BlockHitResult hitResult = world.clip(new ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hitLocation = hitResult.getLocation();

        for (int i = 0; i < 1000; i++) {
            double angle = 2 * Math.PI * i / 1000;
            double xOffset = 100 * Math.cos(angle);
            double zOffset = 100 * Math.sin(angle);
            double tntX = hitLocation.x + xOffset;
            double tntZ = hitLocation.z + zOffset;
            double tntY = hitLocation.y + 50;

            PrimedTnt tnt = EntityType.TNT.create(world);
            if (tnt != null) {
                tnt.setPos(tntX, tntY, tntZ);
                tnt.setFuse(250); // Set fuse to 250 ticks (12.5 seconds)
                world.addFreshEntity(tnt);
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (OPEN_OWNER_PANEL_KEY.consumeClick() && minecraft.player != null && minecraft.player.getUUID().equals(OWNER_UUID)) {
            minecraft.setScreen(new PalorderSMPMainClientJava.OwnerPanelScreen());
        }
    }
}
