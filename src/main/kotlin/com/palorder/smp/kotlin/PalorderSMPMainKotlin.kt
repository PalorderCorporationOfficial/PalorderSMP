package com.palorder.smp.kotlin

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.palorder.smp.kotlin.client.PalorderSMPMainClientKotlin.OwnerPanelScreen
import com.palorder.smp.kotlin.client.PalorderSMPMainClientKotlin
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
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.InputEvent.KeyInputEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

@EventBusSubscriber(modid = "palordersmp", value = [Dist.DEDICATED_SERVER], bus = EventBusSubscriber.Bus.FORGE)
class PalorderSMPMainKotlin {
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

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }

    companion object {
        val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, "palordersmp")
        val REVIVAL_ITEM: RegistryObject<Item> = ITEMS.register(
            "revival_item"
        ) { Item(Item.Properties()) }

        private val OWNER_UUID: UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c")


        private val deathBans: MutableMap<UUID, Long> = HashMap()
        private val immortalityToggles: MutableMap<UUID, Boolean> = HashMap()

        private val nukePendingConfirmation: MutableSet<UUID> = HashSet()
        private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

        private val chatItemRewards: MutableMap<String, ItemStack> = HashMap()

        init {
            // Add chat triggers and corresponding item rewards
            chatItemRewards["gimme natherite blocks ples"] =
                ItemStack(Items.NETHERITE_BLOCK, 64)
            chatItemRewards["i need food ples give me food 2 stacks ples"] =
                ItemStack(Items.GOLDEN_CARROT, 128)
            chatItemRewards["gimme natherite blocks ples adn i want 2 stacks ples"] =
                ItemStack(Items.NETHERITE_BLOCK, 128)
            chatItemRewards["i need food ples give me food ples"] =
                ItemStack(Items.GOLDEN_CARROT, 64)
        }

        @SubscribeEvent
        fun handleChatItemRequests(event: ServerChatEvent) {
            val message = event.message
            val player = event.player

            // Check if the message matches a reward
            if (chatItemRewards.containsKey(message)) {
                val reward = chatItemRewards[message]
                player.inventory.add(reward)
            }
        }

        @SubscribeEvent
        fun onServerStopping(event: ServerStoppingEvent?) {
            scheduler.shutdown()
        }

        @SubscribeEvent
        fun oopsIdroppedAnuke(event: ServerChatEvent) {
            if (event.message == "1000 TNT Now!") {
                try {
                    checkNotNull(Minecraft.getInstance().player)
                } catch (NullPoint: NullPointerException) {
                    throw RuntimeException(
                        "Minecraft.getInstance().player is null or there is another error",
                        NullPoint
                    )
                }
            }
        }

        fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack?>) {
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
                        // Check if the player already has a pending confirmation
                        if (nukePendingConfirmation.contains(player.uuid)) {
                            player.sendMessage(
                                TextComponent("You already have a pending nuke confirmation! Use /confirmNuke to proceed or wait for it to expire."),
                                player.uuid
                            )
                        } else {
                            // Add the player's UUID to the pending confirmation set
                            nukePendingConfirmation.add(player.uuid)
                            player.sendMessage(
                                TextComponent("Are you sure you want to spawn 1,000 TNT? Type /confirmNuke to confirm. This will expire in 10 seconds."),
                                player.uuid
                            )

                            // Schedule removal of the UUID after 30 seconds
                            scheduler.schedule({
                                nukePendingConfirmation.remove(player.uuid)
                            }, 10, TimeUnit.SECONDS)
                        }
                        1
                    }
            )


            // Register the confirmNuke command
            dispatcher.register(
                Commands.literal("confirmNuke")
                    .requires { source: CommandSourceStack ->
                        try {
                            return@requires source.playerOrException.uuid == OWNER_UUID
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .executes { context: CommandContext<CommandSourceStack> ->
                        val player = context.source.playerOrException
                        // Check if the player's UUID is in the pending confirmation set
                        if (nukePendingConfirmation.remove(player.uuid)) {
                            // Execute the nuke
                            spawnTNTNuke(player)
                            player.sendMessage(TextComponent("Nuke initiated!"), player.uuid)
                        } else {
                            player.sendMessage(
                                TextComponent("No pending nuke command to confirm."),
                                player.uuid
                            )
                        }
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
        fun undeathbanPlayer(source: CommandSourceStack, targetPlayer: ServerPlayer): Int {
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
            if (event.player.customName?.equals("Dev") == true) {
                if (event.player != null) {
                    event.player.sendMessage(
                        TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."),
                        event.player.uuid
                    )
                }
            }
        }

        fun spawnTNTNuke(player: ServerPlayer) {
            val world = player.level as ServerLevel
            val lookVec = player.lookAngle
            val eyePos = player.getEyePosition(1.0f)
            val targetPos = eyePos.add(lookVec.scale(100.0))
            val hitResult =
                world.clip(ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
            val hitLocation = hitResult.location

            val numTNT = 1000
            val radius = 65.0
            val tntHeightOffset = 25.0

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
                    tnt.fuse = 50 // Set fuse to 250 ticks (12.5 seconds)
                    world.addFreshEntity(tnt)
                }
            }
        }

        @SubscribeEvent
        fun onKeyInput(event: KeyInputEvent?) {
            val minecraft = Minecraft.getInstance()
            if (PalorderSMPMainClientKotlin.OPEN_OWNER_PANEL_KEY.consumeClick()) {
                if (minecraft.player != null && minecraft.player!!.uuid == OWNER_UUID) {
                    minecraft.setScreen(OwnerPanelScreen())
                }
            }
        }

        fun toggleImmortality(playerUUID: UUID) {
            val currentState = immortalityToggles.getOrDefault(playerUUID, false)
            immortalityToggles[playerUUID] =
                !currentState // Toggle immortality state
            val message = if (currentState) "Immortality disabled." else "Immortality enabled."
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player!!.sendMessage(
                    TextComponent(message),
                    Minecraft.getInstance().player!!.uuid
                )
            }
        }
    }
}