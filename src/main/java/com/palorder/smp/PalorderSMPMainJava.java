package com.palorder.smp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod("palordersmp")
public class PalorderSMPMainJava {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "palordersmp");
    public static final RegistryObject<Item> REVIVAL_ITEM = ITEMS.register("revival_item", () -> new Item(new Item.Properties()));

    private static final UUID OWNER_UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c");
    public static final KeyMapping OPEN_OWNER_PANEL_KEY = new KeyMapping(
            "key.palordersmp.open_owner_panel",
            GLFW.GLFW_KEY_O, "key.categories.palordersmp"
    );

    private final Map<UUID, Long> deathBans = new HashMap<>();
    private static final Map<UUID, Boolean> immortalityToggles = new HashMap<>();

    public PalorderSMPMainJava() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Get the Minecraft server from the event
        MinecraftServer server = event.getServer();
        
        // Ensure the server is not null before proceeding
        if (server != null) {
            // Get the command dispatcher for registering commands
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();

            // Register your commands with the dispatcher
            registerCommands(dispatcher);
        } else {
            // Log a warning if the server is null (shouldn't happen, but just in case)
            final Logger LOGGER = LogManager.getLogger();
            LOGGER.warn("Minecraft server instance is null during onServerStarting how the fuck did that happen?");
        }
    }


    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register the /nuke command (owner only)
        dispatcher.register(Commands.literal("nuke")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    spawnTNTNuke(player);
                    return 1;
                })
        );

        // Register the /undeathban <player> command (owner only)
        dispatcher.register(Commands.literal("undeathban")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> {
                                    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
                                    return undeathbanPlayer(context.getSource(), targetPlayer);
                                }
                        )
                ));
    }

    // Undeathban method to handle removing players from the death ban list
    private int undeathbanPlayer(CommandSourceStack source, ServerPlayer targetPlayer) {
        UUID targetUUID = targetPlayer.getUUID();

        if (deathBans.containsKey(targetUUID)) {
            deathBans.remove(targetUUID); // Remove from deathban list
            source.sendSuccess(new TextComponent("Successfully undeathbanned " + targetPlayer.getName().getString()), true);
        } else {
            source.sendFailure(new TextComponent("Player is not deathbanned."));
        }

        return 1;
    }

    // Send a custom greeting when the owner logs in
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer().getUUID().equals(OWNER_UUID)) {
            event.getPlayer().sendMessage(new TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."), event.getPlayer().getUUID());
        }
    }

    private void spawnTNTNuke(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level;
        Vec3 lookVec = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 targetPos = eyePos.add(lookVec.scale(100));
        BlockHitResult hitResult = world.clip(new ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hitLocation = hitResult.getLocation();

        int numTNT = 1000;
        double radius = 100.0;
        double tntHeightOffset = 50.0;

        for (int i = 0; i < numTNT; i++) {
            double angle = 2 * Math.PI * i / numTNT;
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            double tntX = hitLocation.x + xOffset;
            double tntZ = hitLocation.z + zOffset;
            double tntY = hitLocation.y + tntHeightOffset;

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
        if (OPEN_OWNER_PANEL_KEY.consumeClick()) {
            if (minecraft.player != null && minecraft.player.getUUID().equals(OWNER_UUID)) {
                minecraft.setScreen(new OwnerPanelScreen());

            }
        }
    }

    private static class OwnerPanelScreen extends Screen {
        private EditBox inputField;

        protected OwnerPanelScreen() {
            super(new TextComponent("Owner Panel"));
        }

        @Override
        protected void init() {
            super.init();

            // Hash input field for protocol initiation
            inputField = new EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20, new TextComponent("Enter Command"));
            inputField.setMaxLength(100);
            addRenderableWidget(inputField);

            // Button for initiating the shutdown protocol
            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 20, 200, 20, new TextComponent("Initiate Shutdown Protocol"), button -> {
                Minecraft.getInstance().setScreen(new ShutdownProtocolConfirmationScreen(this));
            }));

            // Button for normal server shutdown
            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 50, 200, 20, new TextComponent("Shutdown Server"), button -> {
                Minecraft.getInstance().setScreen(new ShutdownProtocolConfirmationScreen.NormalShutdownConfirmationScreen(this));
            }));

            // Button for toggling immortality
            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 80, 200, 20, new TextComponent("Toggle Immortality"), button -> {
                toggleImmortality(Minecraft.getInstance().player.getUUID());
            }));
        }


        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            renderBackground(matrices);
            super.render(matrices, mouseX, mouseY, delta);

            // Ensure inputField is properly instantiated before calling render
            if (inputField != null) {
                inputField.render(matrices, mouseX, mouseY, delta);
            }
        }
    }

    private static void toggleImmortality(UUID playerUUID) {
        boolean currentState = immortalityToggles.getOrDefault(playerUUID, false);
        immortalityToggles.put(playerUUID, !currentState); // Toggle immortality state
        String message = currentState ? "Immortality disabled." : "Immortality enabled.";
        Minecraft.getInstance().player.sendMessage(new TextComponent(message), Minecraft.getInstance().player.getUUID());
    }

    private static class ShutdownProtocolConfirmationScreen extends Screen {
        private final OwnerPanelScreen parentScreen;
        private EditBox hashInputField;

        protected ShutdownProtocolConfirmationScreen(OwnerPanelScreen parentScreen) {
            super(new TextComponent("Confirm Shutdown Protocol"));
            this.parentScreen = parentScreen;
        }

        @Override
        protected void init() {
            super.init();
            hashInputField = new EditBox(font, width / 2 - 100, height / 2 + 20, 200, 20, new TextComponent("Rav is my best friend"));
            hashInputField.setMaxLength(100);
            addRenderableWidget(hashInputField);

            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 50, 200, 20, new TextComponent("Confirm"), button -> {
                if (hashInputField.getValue().equals("Rav is my best friend")) {
                    final Logger LOGGER = LogManager.getLogger();

                    // Get the MinecraftServer instance from the current server
                    MinecraftServer server = Minecraft.getInstance().level.getServer();
                    if (server == null) {
                    LOGGER.error(new TextComponent("Uh oh It seems like the getServer Agrument Was NULL so i made a fail mechanism so that the server and the players dont get kicked because  the errors"));
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    LOGGER.fatal(new TextComponent("How the hell did a error come? in this code well thats strange so i will Kill this server Session in 5 seconds"));
                    server.halt(true);
                    if (!server.isShutdown()){
                    server.halt(true);
                    }

                    }
                    }
                    if (server != null) {
                        // Send shutdown message to all players
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                            player.sendMessage(new TextComponent("Shutdown protocol initiated!"), player.getUUID());
                        }

                        // Schedule the shutdown after a 5-second delay
                        server.execute(() -> {
                            try {
                                Thread.sleep(5000); // 5-second delay
                                server.halt(true); // Shut down the server
                            } catch (InterruptedException e) {
                                LOGGER.error("Shutdown delay interrupted", e);
                            }
                        });
                    } else {
                        LOGGER.warn("Server instance is null. Unable to initiate shutdown.");
                    }
                }
            }));




            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 80, 200, 20, new TextComponent("Cancel"), button -> {
                Minecraft.getInstance().setScreen(parentScreen);
            }));
        }

        private static class NormalShutdownConfirmationScreen extends Screen {
            private final OwnerPanelScreen parentScreen;

            protected NormalShutdownConfirmationScreen(OwnerPanelScreen parentScreen) {
                super(new TextComponent("Confirm Normal Shutdown"));
                this.parentScreen = parentScreen;
            }

            @Override
            protected void init() {
                super.init();

                addRenderableWidget(new Button(width / 2 - 100, height / 2 + 20, 200, 20, new TextComponent("Confirm"), button -> {
                    Minecraft.getInstance().close();
                }));

                addRenderableWidget(new Button(width / 2 - 100, height / 2 + 50, 200, 20, new TextComponent("Cancel"), button -> {
                    Minecraft.getInstance().setScreen(parentScreen);
                }));
            }
        }
    }
    }
