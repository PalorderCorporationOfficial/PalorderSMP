package com.palorder.smp.java;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod("palordersmp")
@Mod.EventBusSubscriber(modid = "palordersmp",value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PalorderSMPMainJava {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "palordersmp");
    public static final RegistryObject<Item> REVIVAL_ITEM = ITEMS.register("revival_item", () -> new Item(new Item.Properties()));

    private static final UUID OWNER_UUID = UUID.fromString("78d8e34d-5d1a-4b2d-85e2-f0792d9e1a6c");


    private static final Map<UUID, Long> deathBans = new HashMap<>();

    private static final Set<UUID> nukePendingConfirmation = new HashSet<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public static final ResourceLocation REVENGE_SOUND = new ResourceLocation("palordersmp", "revenge");
    public static final SoundEvent REVENGE_SOUND_EVENT = new SoundEvent(REVENGE_SOUND);

    public static void registerSounds(IEventBus eventBus) {
        eventBus.addGenericListener(SoundEvent.class, (RegistryEvent.Register<SoundEvent> event) -> {
            event.getRegistry().register(REVENGE_SOUND_EVENT.setRegistryName(REVENGE_SOUND));
        });
    }


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
        Registry.register(Registry.SOUND_EVENT, new ResourceLocation("palordersmp", "revenge"), REVENGE_SOUND_EVENT);
    }

    private static final Map<String, ItemStack> chatItemRewards = new HashMap<>();

    static {
        // Add chat triggers and corresponding item rewards
        chatItemRewards.put("gimme natherite blocks ples", new ItemStack(Items.NETHERITE_BLOCK, 64));
        chatItemRewards.put("i need food ples give me food 2 stacks ples", new ItemStack(Items.GOLDEN_CARROT, 128));
        chatItemRewards.put("gimme natherite blocks ples adn i want 2 stacks ples", new ItemStack(Items.NETHERITE_BLOCK, 128));
        chatItemRewards.put("i need food ples give me food ples", new ItemStack(Items.GOLDEN_CARROT, 64));
    }

    @SubscribeEvent
    public static void handleChatItemRequests(ServerChatEvent event) {
        String message = event.getMessage();
        ServerPlayer player = event.getPlayer();

        // Check if the message matches a reward
        if (chatItemRewards.containsKey(message)) {
            ItemStack reward = chatItemRewards.get(message);
            player.getInventory().add(reward);
        }
    }
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) throws InterruptedException {
        scheduler.shutdown();
    }
    @SubscribeEvent
    public static void oopsIdroppedAnuke(ServerChatEvent event) {
        if (event.getMessage().equals("Nuke Now!")) {
            event.getPlayer().sendMessage(new TextComponent("No you dont"), event.getPlayer().getUUID());
        }
    }
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register the /nuke command (owner only)
        dispatcher.register(Commands.literal("InitiateNukeProtocol")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // Check if the player already has a pending confirmation
                    if (nukePendingConfirmation.contains(player.getUUID())) {
                        player.sendMessage(new TextComponent("You already have a pending nuke initiation confirmation! Use /confirmNukeInitiation to proceed or wait for it to expire."), player.getUUID());
                    } else {
                        // Add the player's UUID to the pending confirmation set
                        nukePendingConfirmation.add(player.getUUID());
                        player.sendMessage(new TextComponent("Are you sure you want to spawn 1,000 TNT? Type /confirmNukeInitiation to confirm. This will expire in 30 seconds."), player.getUUID());

                        // Schedule removal of the UUID after 30 seconds
                        scheduler.schedule(() -> {
                            nukePendingConfirmation.remove(player.getUUID());
                        }, 30, TimeUnit.SECONDS);
                    }

                    return 1;
                })
        );


// Register the confirmNuke command
        dispatcher.register(Commands.literal("confirmNukeInitiation")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // Check if the player's UUID is in the pending confirmation set
                    if (nukePendingConfirmation.remove(player.getUUID())) {
                        // Execute the nuke
                        spawnTNTNuke(player);
                        player.sendMessage(new TextComponent("Nuke initiated!"), player.getUUID());
                    } else {
                        player.sendMessage(new TextComponent("No pending Nuke Initiation Protocol"), player.getUUID());
                    }

                    return 1;
                })
        );

        dispatcher.register(Commands.literal("GetTheF**kOutServer")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    System.exit(1);
                    try {
                        MinecraftServer server = Minecraft.getInstance().level.getServer();
                        server.halt(true);
                    } catch (Exception e) {
                        throw new RuntimeException(e);

                    }
                    return 1;

                })
        );
        // Register the /undeathban <player> command (owner only)
        dispatcher.register(Commands.literal("undeathban")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (Exception e) {
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
        dispatcher.register(Commands.literal("TestNuke")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                })
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    player.sendMessage(new TextComponent("TestNuke? Nahh better a nuke should i say"), player.getUUID());
                    spawnTNTNuke(player);
                    return 1;
                })
        );
        dispatcher.register(Commands.literal("creeperMusic")
                .requires(source -> {
                    try {
                        return source.getPlayerOrException().getUUID().equals(OWNER_UUID);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .executes(context -> {
                    // Get the player and coordinates
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String command = String.format(
                            "playsound palordersmp:revenge master @a[x=%.2f,y=%.2f,z=%.2f,distance=..100] 1 1",
                            player.getX(), player.getY(), player.getZ()
                    );

                    // Debugging: Print the command string
                    System.out.println("Executing command: " + command);

                    // Execute the command
                    int success = context.getSource().getServer().getCommands().performCommand(context.getSource(), command);

                    // Debugging: Log success status
                    System.out.println("Command executed: " + success);

                    // Return an integer based on the success status
                    return success > 0 ? 1 : 0;
                })
        );



    }

        // Undeathban method to handle removing players from the death ban list
    public static int undeathbanPlayer(CommandSourceStack source, ServerPlayer targetPlayer) {
        UUID targetUUID = targetPlayer.getUUID();

        if (deathBans.containsKey(targetUUID)) {
            deathBans.remove(targetUUID); // Remove from deathban list
            source.sendSuccess(new TextComponent("Successfully undeathbanned The TargetPlayer"), true);
        } else {
            source.sendFailure(new TextComponent("Player is not deathbanned."));
        }

        return 1;
    }

    // Send a custom greeting when the owner logs in
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Check if player is the owner
        if (event.getPlayer() != null && event.getPlayer().getUUID().equals(OWNER_UUID)) {
            event.getPlayer().sendMessage(
                    new TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."),
                    event.getPlayer().getUUID()
            );
        }

        // Check if player's custom name matches "Dev"
        if (event.getPlayer() != null &&
                event.getPlayer().getCustomName() != null &&
                "Dev".equals(event.getPlayer().getCustomName().getString())) {
            event.getPlayer().sendMessage(
                    new TextComponent("Server: Welcome Back Sir! Press 'O' to get ready to shutdown the server for updates, etc."),
                    event.getPlayer().getUUID()
            );
        }
    }



    public static void spawnTNTNuke(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level;
        Vec3 lookVec = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 targetPos = eyePos.add(lookVec.scale(100));
        BlockHitResult hitResult = world.clip(new ClipContext(eyePos, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 hitLocation = hitResult.getLocation();

        int rings = 2;
        int numTNT = 2000; // Number of TNT per ring
        double radius = 55.0; // Base radius of each ring
        double tntHeightOffset = 65.0; // Starting height of the first ring

        // Loop through the number of rings (layers stacked on top of each other)
        for (int ring = 0; ring < rings; ring++) {
            double currentHeightOffset = tntHeightOffset + (ring * 1.0); // Increment height for each new ring

            for (int i = 0; i < numTNT; i++) {
                double angle = 2 * Math.PI * i / numTNT;
                double xOffset = radius * Math.cos(angle);
                double zOffset = radius * Math.sin(angle);
                double tntX = hitLocation.x + xOffset;
                double tntZ = hitLocation.z + zOffset;
                double tntY = hitLocation.y + currentHeightOffset;

                PrimedTnt tnt = EntityType.TNT.create(world);
                if (tnt != null) {
                    tnt.setPos(tntX, tntY, tntZ);
                    tnt.setFuse(150); // Set fuse to 250 ticks (12.5 seconds)
                    world.addFreshEntity(tnt);
                }
            }
        }
    }
}