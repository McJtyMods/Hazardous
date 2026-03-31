package mcjty.hazardous.client;

import mcjty.hazardous.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;

public class PillsShowSoundController {

    private static boolean initialized = false;
    private static int lastSelectedSlot = -1;

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            initialized = false;
            lastSelectedSlot = -1;
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }

        int selectedSlot = player.getInventory().selected;
        if (!initialized) {
            initialized = true;
            lastSelectedSlot = selectedSlot;
            return;
        }
        if (selectedSlot == lastSelectedSlot) {
            return;
        }

        lastSelectedSlot = selectedSlot;
        ItemStack selectedStack = player.getInventory().getSelected();
        if (!isPills(selectedStack)) {
            return;
        }

        minecraft.level.playLocalSound(
                player.getX(),
                player.getY(),
                player.getZ(),
                Registration.PILLS_SHOW.get(),
                SoundSource.PLAYERS,
                0.6f,
                1.0f,
                false
        );
    }

    private static boolean isPills(ItemStack stack) {
        return stack.is(Registration.PILLS.get()) || stack.is(Registration.RESISTANCE_PILLS.get());
    }
}
