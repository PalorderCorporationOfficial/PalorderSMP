package com.palorder.smp.java.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Mod("palordersmp")
@Mod.EventBusSubscriber(modid = "palordersmp", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)

public class PalorderSMPMainClientJava {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final KeyMapping OPEN_OWNER_PANEL_KEY = new KeyMapping(
            I18n.get("key.palordersmp.open_owner_panel"),  // Use translated key name
            GLFW.GLFW_KEY_O,  // Default key 'O'
            I18n.get("key.categories.palordersmp")  // Use translated category name
    );


    // Register the keybinding
    public static void registerKeyBindings() {
        ClientRegistry.registerKeyBinding(OPEN_OWNER_PANEL_KEY);
    }
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        com.palorder.smp.java.client.PalorderSMPMainClientJava.registerKeyBindings();
        LOGGER.warn("Keybinding registered!");
    }
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        // Get the current player
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getUUID().equals(OWNER_UUID)) {
            // Only show the owner panel if the player is the owner
            if (OPEN_OWNER_PANEL_KEY.isDown()) {
                // Action to perform when 'O' is pressed and player is the owner
                minecraft.setScreen(new com.palorder.smp.java.client.PalorderSMPMainClientJava.OwnerPanelScreen());
                LOGGER.info("Key 'O' was pressed by the owner!");
            }
        }
    }

    private static final UUID OWNER_UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c");
    private static com.palorder.smp.java.client.PalorderSMPMainClientJava instance;

    private final Map<UUID, Long> deathBans = new HashMap<>();
    private static final Map<UUID, Boolean> immortalityToggles = new HashMap<>();

    public static com.palorder.smp.java.client.PalorderSMPMainClientJava getInstance() {
        if (instance == null) {
            instance = new com.palorder.smp.java.client.PalorderSMPMainClientJava();
        }
        return instance;
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer().getUUID().equals(OWNER_UUID)) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendMessage(new TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."), event.getPlayer().getUUID());
            }
        }
        if (event.getPlayer().getCustomName().equals("Dev")) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendMessage(new TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."), event.getPlayer().getUUID());
            }
        }
    }

    @SubscribeEvent
    public void TestCommandMessage(PlayerEvent event) {
        event.getPlayer().sendMessage(new TextComponent("Hi"), event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public void OnClientStartingEvent(FMLClientSetupEvent event) {
        LOGGER.warn("Client Loaded");
    }

    public static class OwnerPanelScreen extends Screen {
        private EditBox inputField;

        public OwnerPanelScreen() {
            super(new TextComponent("Owner Panel"));
        }

        @Override
        protected void init() {
            super.init();

            inputField = new EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20, new TextComponent(I18n.get("screen.owner_panel.title")));
            inputField.setMaxLength(100);
            addRenderableWidget(inputField);

            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 20, 200, 20, new TextComponent(I18n.get("button.confirm")), button -> {
                Minecraft.getInstance().setScreen(new ShutdownProtocolConfirmationScreen(this));
            }));

            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 50, 200, 20, new TextComponent(I18n.get("button.cancel")), button -> {
                Minecraft.getInstance().setScreen(new NormalShutdownConfirmationScreen(this));
            }));

            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 80, 200, 20, new TextComponent(I18n.get("button.toggle_immortality")), button -> {
                if (Minecraft.getInstance().player.getUUID() != null) {
                    toggleImmortality(Minecraft.getInstance().player.getUUID());
                }
            }));
        }


        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            renderBackground(matrices);
            super.render(matrices, mouseX, mouseY, delta);

            if (inputField != null) {
                inputField.render(matrices, mouseX, mouseY, delta);
            }
        }
    }

    static class ShutdownProtocolConfirmationScreen extends Screen {
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
                    MinecraftServer server = Minecraft.getInstance().level.getServer();

                    if (server == null) {
                        LOGGER.error("Server instance is null, attempting to fail gracefully.");
                        try {
                            Thread.sleep(5000);
                            if (Minecraft.getInstance().isLocalServer()) {
                                Minecraft.getInstance().close();
                            }
                        } catch (InterruptedException e) {
                            LOGGER.fatal("Error during shutdown delay.", e);
                            if (server != null) {
                                server.halt(true);
                            }
                        }
                    }

                    if (server != null) {
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                            player.sendMessage(new TextComponent("Shutdown protocol initiated!"), player.getUUID());
                        }

                        server.execute(() -> {
                            try {
                                Thread.sleep(5000);
                                server.halt(true);
                                server.stopServer();
                                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                                    player.sendMessage(new TextComponent(I18n.get("message.shutdown.initiated")), player.getUUID());
                                }
                            } catch (InterruptedException e) {
                                LOGGER.error("Shutdown Error Please Try Again Later", e);
                            }
                        });
                    }}}));
            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 80, 200, 20, new TextComponent("Cancel"), button -> {
                Minecraft.getInstance().setScreen(parentScreen);
            }));
        }
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

    public static void toggleImmortality(UUID playerUUID) {
        boolean currentState = immortalityToggles.getOrDefault(playerUUID, false);
        immortalityToggles.put(playerUUID, !currentState);
        String messageKey = currentState ? "message.immortality.disabled" : "message.immortality.enabled";

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendMessage(new TextComponent(I18n.get(messageKey)), playerUUID);
        }
    }
}
