package com.palorder.smp.java.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.ServerChatEvent;
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
@Mod.EventBusSubscriber(modid = "palordersmp", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)

public class PalorderSMPMainClientJava {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final KeyMapping OPEN_OWNER_PANEL_KEY = new KeyMapping(
            "key.palordersmp.open_owner_panel",
            GLFW.GLFW_KEY_O, "key.categories.palordersmp"
    );

    private static final UUID OWNER_UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c");
    private static com.palorder.smp.java.client.PalorderSMPMainClientJava instance;

    private final Map<UUID, Long> deathBans = new HashMap<>();
    private static final Map<UUID, Boolean> immortalityToggles = new HashMap<>();

    public PalorderSMPMainClientJava() {
        instance = this;
    }

    public static com.palorder.smp.java.client.PalorderSMPMainClientJava getInstance() {
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
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_OWNER_PANEL_KEY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.player.getUUID().equals(OWNER_UUID)) {
                minecraft.setScreen(new OwnerPanelScreen());
            }
        }
    }

    @SubscribeEvent
    public static void giveItems(ServerChatEvent event) {
        if (event.getMessage().equals("gimme natherite blocks ples")) {
            event.getPlayer().getInventory().add(new ItemStack(Items.NETHERITE_BLOCK, 64));
        }
    }

    @SubscribeEvent
    public static void giveItems2(ServerChatEvent event) {
        if (event.getMessage().equals("i need food ples give me food 2 stacks ples")) {
            event.getPlayer().getInventory().add(new ItemStack(Items.GOLDEN_CARROT, 128));
        }
    }

    @SubscribeEvent
    public static void giveItems3(ServerChatEvent event) {
        if (event.getMessage().equals("gimme natherite blocks ples adn i want 2 stacks ples")) {
            event.getPlayer().getInventory().add(new ItemStack(Items.NETHERITE_BLOCK, 128));
        }
    }

    @SubscribeEvent
    public static void giveItems4(ServerChatEvent event) {
        if (event.getMessage().equals("i need food ples give me food ples")) {
            event.getPlayer().getInventory().add(new ItemStack(Items.GOLDEN_CARROT, 64));
        }
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

            inputField = new EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20, new TextComponent("Enter Command"));
            inputField.setMaxLength(100);
            addRenderableWidget(inputField);

            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 20, 200, 20, new TextComponent("Initiate Shutdown Protocol"), button -> {Minecraft.getInstance().setScreen(new ShutdownProtocolConfirmationScreen(this));}));

            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 50, 200, 20, new TextComponent("Shutdown Server"), button -> {
                Minecraft.getInstance().setScreen(new NormalShutdownConfirmationScreen(this));
            }));

            addRenderableWidget(new Button(width / 2 - 100, height / 2 + 80, 200, 20, new TextComponent("Toggle Immortality"), button -> {
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
                            } catch (InterruptedException e) {
                                LOGGER.error("Shutdown Error Please Try Again Later", e);
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
        String message = currentState ? "Immortality disabled." : "Immortality enabled.";
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendMessage(new TextComponent(message), playerUUID);
        }
    }
}
