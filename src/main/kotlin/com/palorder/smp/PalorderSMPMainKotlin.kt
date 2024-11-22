package com.palorder.smp

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.palorder.smp.client.PalorderSMPMainClientJava
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraftforge.client.event.InputEvent.KeyInputEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFW
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Mod("palordersmp")
class PalorderSMPMainKotlin {
    private val deathBans: MutableMap<UUID, Long> = HashMap()

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        // Get the Minecraft server from the event
        val server = event.server

        // Ensure the server is not null before proceeding
        if (server != null) {
            // Get the command dispatcher for registering commands
            val dispatcher = server.commands.dispatcher

            // Register your commands with the dispatcher
            registerCommands(dispatcher)
        } else {
            // Log a warning if the server is null (shouldn't happen, but just in case)
            val LOGGER = LogManager.getLogger()
            LOGGER.warn("Minecraft server instance is null during onServerStarting how the fuck did that happen?")
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        // Register the /nuke command (owner only)
        dispatcher.register(
            Commands.literal("nuke")
                .requires { source: CommandSourceStack ->
                    try {
                        return@requires source.playerOrException.uuid == OWNER_UUID
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
                .executes { context: CommandContext<CommandSourceStack> ->
                    val player = context.source.playerOrException
                    spawnTNTNuke(player)
                    1
                }
        )

        // Register the /undeathban <player> command (owner only)
        dispatcher.register(
            Commands.literal("undeathban")
                .requires { source: CommandSourceStack ->
                    try {
                        return@requires source.playerOrException.uuid == OWNER_UUID
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .executes { context: CommandContext<CommandSourceStack> ->
                            val targetPlayer = EntityArgument.getPlayer(context, "player")
                            undeathbanPlayer(context.source, targetPlayer)
                        }
                ))
        dispatcher.register(
            Commands.literal("TestMessage")
                .requires { source: CommandSourceStack ->
                    try {
                        return@requires source.playerOrException.uuid == OWNER_UUID
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
                .executes { context: CommandContext<CommandSourceStack> ->
                    val player = context.source.playerOrException
                    player.sendMessage(TextComponent("hello"), player.uuid)
                    1
                }
        )
    }

    // Undeathban method to handle removing players from the death ban list
    private fun undeathbanPlayer(source: CommandSourceStack, targetPlayer: ServerPlayer): Int {
        val targetUUID = targetPlayer.uuid

        if (deathBans.containsKey(targetUUID)) {
            deathBans.remove(targetUUID) // Remove from deathban list
            source.sendSuccess(TextComponent("Successfully undeathbanned The TargetPlayer"), true)
        } else {
            source.sendFailure(TextComponent("Player is not deathbanned."))
        }

        return 1
    }

    // Send a custom greeting when the owner logs in
    @SubscribeEvent
    fun onPlayerLogin(event: PlayerLoggedInEvent) {
        if (event.player.uuid == OWNER_UUID) {
            event.player.sendMessage(
                TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."),
                event.player.uuid
            )
        }
    }

    private fun spawnTNTNuke(player: ServerPlayer) {
        val world = player.level as ServerLevel
        val lookVec = player.lookAngle
        val eyePos = player.getEyePosition(1.0f)
        val targetPos = eyePos.add(lookVec.scale(100.0))
        val hitResult =
            world.clip(ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
        val hitLocation = hitResult.location

        val numTNT = 1000
        val radius = 100.0
        val tntHeightOffset = 50.0

        for (i in 0 until numTNT) {
            val angle = 2 * Math.PI * i / numTNT
            val xOffset = radius * cos(angle)
            val zOffset = radius * sin(angle)
            val tntX = hitLocation.x + xOffset
            val tntZ = hitLocation.z + zOffset
            val tntY = hitLocation.y + tntHeightOffset

            val tnt = EntityType.TNT.create(world)
            if (tnt != null) {
                tnt.setPos(tntX, tntY, tntZ)
                tnt.fuse = 250 // Set fuse to 250 ticks (12.5 seconds)
                world.addFreshEntity(tnt)
            }
        }
    }

    @SubscribeEvent
    fun onKeyInput(event: KeyInputEvent?) {
        val minecraft = Minecraft.getInstance()
        if (OPEN_OWNER_PANEL_KEY.consumeClick()) {
            if (minecraft.player != null && minecraft.player!!.uuid == OWNER_UUID) {
                minecraft.setScreen(PalorderSMPMainClientJava.OwnerPanelScreen())
            }
        }
    }

    companion object {
        val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, "palordersmp")
        val REVIVAL_ITEM: RegistryObject<Item> = ITEMS.register(
            "revival_item"
        ) { Item(Item.Properties()) }

        private val OWNER_UUID: UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c")
        val OPEN_OWNER_PANEL_KEY: KeyMapping = KeyMapping(
            "key.palordersmp.open_owner_panel",
            GLFW.GLFW_KEY_O, "key.categories.palordersmp"
        )

        private val immortalityToggles: MutableMap<UUID, Boolean> = HashMap()

        private fun displayCustomClientMessage() {
        }

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

        fun toggleImmortality(playerUUID: UUID) {
            val currentState = immortalityToggles.getOrDefault(playerUUID, false)
            immortalityToggles[playerUUID] =
                !currentState // Toggle immortality state
            val message = if (currentState) "Immortality disabled." else "Immortality enabled."
            Minecraft.getInstance().player!!.sendMessage(TextComponent(message), Minecraft.getInstance().player!!.uuid)
        }
    }
}