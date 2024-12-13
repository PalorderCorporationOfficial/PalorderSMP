package com.palorder.smp.kotlin

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.core.Registry
import net.minecraft.network.chat.TextComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
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

@Mod("palordersmp")
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

        private val nukePendingConfirmation: MutableSet<UUID> = HashSet()
        private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
        val REVENGE_SOUND: ResourceLocation = ResourceLocation("palordersmp", "revenge")
        val REVENGE_SOUND_EVENT: SoundEvent = SoundEvent(REVENGE_SOUND)

        fun registerSounds(eventBus: IEventBus) {
            eventBus.addGenericListener(
                SoundEvent::class.java
            ) { event: RegistryEvent.Register<SoundEvent?> ->
                event.registry.register(
                    REVENGE_SOUND_EVENT.setRegistryName(
                        REVENGE_SOUND
                    )
                )
            }
        }


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
        @Throws(InterruptedException::class)
        fun onServerStopping(event: ServerStoppingEvent?) {
            scheduler.shutdown()
        }

        @SubscribeEvent
        fun oopsIdroppedAnuke(event: ServerChatEvent) {
            if (event.message == "Nuke Now!") {
                event.player.sendMessage(TextComponent("No you dont"), event.player.uuid)
            }
        }

        fun registerCommands(dispatcher: CommandDispatcher<CommandSourceStack?>) {
            // Register the /nuke command (owner only)
            dispatcher.register(
                Commands.literal("InitiateNukeProtocol")
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
                                TextComponent("You already have a pending nuke initiation confirmation! Use /confirmNukeInitiation to proceed or wait for it to expire."),
                                player.uuid
                            )
                        } else {
                            // Add the player's UUID to the pending confirmation set
                            nukePendingConfirmation.add(player.uuid)
                            player.sendMessage(
                                TextComponent("Are you sure you want to spawn 1,000 TNT? Type /confirmNukeInitiation to confirm. This will expire in 30 seconds."),
                                player.uuid
                            )

                            // Schedule removal of the UUID after 30 seconds
                            scheduler.schedule({
                                nukePendingConfirmation.remove(player.uuid)
                            }, 30, TimeUnit.SECONDS)
                        }
                        1
                    }
            )


            // Register the confirmNuke command
            dispatcher.register(
                Commands.literal("confirmNukeInitiation")
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
                                TextComponent("No pending Nuke Initiation Protocol"),
                                player.uuid
                            )
                        }
                        1
                    }
            )

            dispatcher.register(
                Commands.literal("GetTheF**kOutServer")
                    .requires { source: CommandSourceStack ->
                        try {
                            return@requires source.playerOrException.uuid == OWNER_UUID
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .executes { context: CommandContext<CommandSourceStack?>? ->
                        System.exit(1)
                        try {
                            val server = Minecraft.getInstance().level!!.server
                            server!!.halt(true)
                        } catch (e: Exception) {
                            throw RuntimeException(e)
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
                Commands.literal("TestNuke")
                    .requires { source: CommandSourceStack ->
                        try {
                            return@requires source.playerOrException.uuid == OWNER_UUID
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .executes { context: CommandContext<CommandSourceStack> ->
                        val player = context.source.playerOrException
                        player.sendMessage(
                            TextComponent("TestNuke? Nahh better a nuke should i say"),
                            player.uuid
                        )
                        spawnTNTNuke(player)
                        1
                    }
            )
            dispatcher.register(
                Commands.literal("creeperMusic")
                    .requires { source: CommandSourceStack ->
                        try {
                            return@requires source.playerOrException.uuid == OWNER_UUID
                        } catch (e: Exception) {
                            throw RuntimeException(e)
                        }
                    }
                    .executes { context: CommandContext<CommandSourceStack> ->
                        // Get the player executing the command
                        val player = context.source.playerOrException
                        val level: Level = player.getLevel()
                        val playerPos = player.blockPosition()

                        // Radius for the sound effect
                        val radius = 100

                        // Play the sound for all players within the radius
                        level.players().stream()
                            .filter { otherPlayer: Player? ->
                                val isWithinRadius =
                                    player.blockPosition().distSqr(otherPlayer!!.blockPosition()) <= radius * radius
                                println(
                                    "Player " + otherPlayer.name.string + " is " +
                                            (if (isWithinRadius) "within" else "out of") + " range."
                                )
                                isWithinRadius
                            }
                            .forEach { otherPlayer: Player? ->
                                level.playSound(
                                    null,
                                    playerPos,
                                    REVENGE_SOUND_EVENT,
                                    SoundSource.RECORDS,
                                    1.0f,
                                    1.0f
                                )
                            }

                        1 // Return statement to indicate success
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
            // Check if player is the owner
            if (event.player != null && event.player.uuid == OWNER_UUID) {
                event.player.sendMessage(
                    TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."),
                    event.player.uuid
                )
            }

            // Check if player's custom name matches "Dev"
            if (event.player != null && event.player.customName != null &&
                "Dev" == event.player.customName!!.string
            ) {
                event.player.sendMessage(
                    TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."),
                    event.player.uuid
                )
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

            val rings = 2
            val numTNT = 2000 // Number of TNT per ring
            val radius = 55.0 // Base radius of each ring
            val tntHeightOffset = 65.0 // Starting height of the first ring

            // Loop through the number of rings (layers stacked on top of each other)
            for (ring in 0 until rings) {
                val currentHeightOffset = tntHeightOffset + (ring * 1.0) // Increment height for each new ring

                for (i in 0 until numTNT) {
                    val angle = 2 * Math.PI * i / numTNT
                    val xOffset = radius * cos(angle)
                    val zOffset = radius * sin(angle)
                    val tntX = hitLocation.x + xOffset
                    val tntZ = hitLocation.z + zOffset
                    val tntY = hitLocation.y + currentHeightOffset

                    val tnt = EntityType.TNT.create(world)
                    if (tnt != null) {
                        tnt.setPos(tntX, tntY, tntZ)
                        tnt.fuse = 150 // Set fuse to 250 ticks (12.5 seconds)
                        world.addFreshEntity(tnt)
                    }
                }
            }
        }
    }
}