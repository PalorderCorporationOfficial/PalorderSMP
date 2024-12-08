package com.palorder.smp.java.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.commons.logging.Log;
import org.apache.logging.log4j.LogManager;
import net.minecraft.client.Minecraft;

import static com.palorder.smp.java.client.PalorderSMPMainClientJava.OPEN_OWNER_PANEL_KEY;

@Mod.EventBusSubscriber(modid = "palordersmp", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {


    private static final Log LOGGER = (Log) LogManager.getLogger();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PalorderSMPMainClientJava.registerKeyBindings();
        LOGGER.warn("Keybinding registered!");
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (OPEN_OWNER_PANEL_KEY.isDown()) {
            // Action to perform when 'O' is pressed
            Minecraft.getInstance().setScreen(new PalorderSMPMainClientJava.OwnerPanelScreen());
            LOGGER.info("Key 'O' was pressed!");
        }
    }
}

