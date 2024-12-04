package com.palorder.smp.kotlin.client

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.InputEvent.KeyInputEvent
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.glfw.GLFW
import java.util.*

@EventBusSubscriber(modid = "palordersmp", value = [Dist.CLIENT], bus = EventBusSubscriber.Bus.MOD)
class PalorderSMPMainClientKotlin {
    private val deathBans: Map<UUID, Long> = HashMap()

    init {
        instance = this
    }

    @SubscribeEvent
    fun onPlayerLogin(event: PlayerLoggedInEvent) {
        if (event.player.uuid == OWNER_UUID) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player!!.sendMessage(
                    TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."),
                    event.player.uuid
                )
            }
        }
        if (event.player.customName?.equals("dev") == true) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player!!.sendMessage(
                    TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."),
                    event.player.uuid
                )
            }
        }
    }

    @SubscribeEvent
    fun TestCommandMessage(event: PlayerEvent) {
        event.player.sendMessage(TextComponent("Hi"), event.player.uuid)
    }

    @SubscribeEvent
    fun onKeyInput(event: KeyInputEvent?) {
        if (OPEN_OWNER_PANEL_KEY.consumeClick()) {
            val minecraft = Minecraft.getInstance()
            if (minecraft.player != null && minecraft.player!!.uuid == OWNER_UUID) {
                minecraft.setScreen(OwnerPanelScreen())
            }
        }
    }

    @SubscribeEvent
    fun OnClientStartingEvent(event: FMLClientSetupEvent?) {
        LOGGER.warn("Client Loaded")
    }

    class OwnerPanelScreen : Screen(TextComponent("Owner Panel")) {
        private var inputField: EditBox? = null

        override fun init() {
            super.init()

            inputField = EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20, TextComponent("Enter Command"))
            inputField!!.setMaxLength(100)
            addRenderableWidget(inputField)

            addRenderableWidget(
                Button(
                    width / 2 - 100, height / 2 + 20, 200, 20, TextComponent("Initiate Shutdown Protocol")
                ) { button: Button? ->
                    Minecraft.getInstance().setScreen(
                        ShutdownProtocolConfirmationScreen(
                            this
                        )
                    )
                })

            addRenderableWidget(
                Button(
                    width / 2 - 100, height / 2 + 50, 200, 20, TextComponent("Shutdown Server")
                ) { button: Button? ->
                    Minecraft.getInstance().setScreen(
                        NormalShutdownConfirmationScreen(
                            this
                        )
                    )
                })

            addRenderableWidget(
                Button(
                    width / 2 - 100, height / 2 + 80, 200, 20, TextComponent("Toggle Immortality")
                ) { button: Button? ->
                    if (Minecraft.getInstance().player!!.uuid != null) {
                        toggleImmortality(Minecraft.getInstance().player!!.uuid)
                    }
                })
        }

        override fun render(matrices: PoseStack, mouseX: Int, mouseY: Int, delta: Float) {
            renderBackground(matrices)
            super.render(matrices, mouseX, mouseY, delta)

            if (inputField != null) {
                inputField!!.render(matrices, mouseX, mouseY, delta)
            }
        }
    }

    internal class ShutdownProtocolConfirmationScreen(private val parentScreen: OwnerPanelScreen) :
        Screen(TextComponent("Confirm Shutdown Protocol")) {
        private var hashInputField: EditBox? = null

        override fun init() {
            super.init()
            hashInputField =
                EditBox(font, width / 2 - 100, height / 2 + 20, 200, 20, TextComponent("Rav is my best friend"))
            hashInputField!!.setMaxLength(100)
            addRenderableWidget(hashInputField)

            addRenderableWidget(
                Button(
                    width / 2 - 100, height / 2 + 50, 200, 20, TextComponent("Confirm")
                ) { button: Button? ->
                    if (hashInputField!!.value == "Rav is my best friend") {
                        val LOGGER = LogManager.getLogger()
                        val server = Minecraft.getInstance().level!!.server

                        if (server == null) {
                            LOGGER.error("Server instance is null, attempting to fail gracefully.")
                            try {
                                Thread.sleep(5000)
                            } catch (e: InterruptedException) {
                                LOGGER.fatal("Error during shutdown delay.", e)
                                if (server != null) {
                                    server.halt(true)
                                }
                            }
                        }

                        if (server != null) {
                            for (player in server.playerList.players) {
                                player.sendMessage(
                                    TextComponent("Shutdown protocol initiated!"),
                                    player.uuid
                                )
                            }

                            server.execute(Runnable {
                                try {
                                    Thread.sleep(5000)
                                    server.halt(true)
                                    server.stopServer()
                                } catch (e: InterruptedException) {
                                    LOGGER.error("Shutdown Error Please Try Again Later", e)
                                }
                            })
                        } else {
                            LOGGER.warn("Server instance is null. Unable to initiate shutdown.")
                        }
                    }
                })

            addRenderableWidget(
                Button(
                    width / 2 - 100, height / 2 + 80, 200, 20, TextComponent("Cancel")
                ) { button: Button? ->
                    Minecraft.getInstance().setScreen(parentScreen)
                })
        }
    }

    private class NormalShutdownConfirmationScreen(private val parentScreen: OwnerPanelScreen) :
        Screen(TextComponent("Confirm Normal Shutdown")) {
        override fun init() {
            super.init()

            addRenderableWidget(
                Button(
                    width / 2 - 100, height / 2 + 20, 200, 20, TextComponent("Confirm")
                ) { button: Button? ->
                    Minecraft.getInstance().close()
                })

            addRenderableWidget(
                Button(
                    width / 2 - 100, height / 2 + 50, 200, 20, TextComponent("Cancel")
                ) { button: Button? ->
                    Minecraft.getInstance().setScreen(parentScreen)
                })
        }
    }

    companion object {
        private val LOGGER: Logger = LogManager.getLogger()

        val OPEN_OWNER_PANEL_KEY: KeyMapping = KeyMapping(
            "key.palordersmp.open_owner_panel",
            GLFW.GLFW_KEY_O, "key.categories.palordersmp"
        )

        private val OWNER_UUID: UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c")
        lateinit var instance: PalorderSMPMainClientKotlin

        private val immortalityToggles: MutableMap<UUID, Boolean> = HashMap()

        @SubscribeEvent
        fun giveItems(event: ServerChatEvent) {
            if (event.message == "gimme natherite blocks ples") {
                event.player.inventory.add(ItemStack(Items.NETHERITE_BLOCK, 64))
            }
        }

        @SubscribeEvent
        fun giveItems2(event: ServerChatEvent) {
            if (event.message == "i need food ples give me food 2 stacks ples") {
                event.player.inventory.add(ItemStack(Items.GOLDEN_CARROT, 128))
            }
        }

        @SubscribeEvent
        fun giveItems3(event: ServerChatEvent) {
            if (event.message == "gimme natherite blocks ples adn i want 2 stacks ples") {
                event.player.inventory.add(ItemStack(Items.NETHERITE_BLOCK, 128))
            }
        }

        @SubscribeEvent
        fun giveItems4(event: ServerChatEvent) {
            if (event.message == "i need food ples give me food ples") {
                event.player.inventory.add(ItemStack(Items.GOLDEN_CARROT, 64))
            }
        }

        fun toggleImmortality(playerUUID: UUID) {
            val currentState = immortalityToggles.getOrDefault(playerUUID, false)
            immortalityToggles[playerUUID] = !currentState
            val message = if (currentState) "Immortality disabled." else "Immortality enabled."
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player!!.sendMessage(TextComponent(message), playerUUID)
            }
        }
    }
}