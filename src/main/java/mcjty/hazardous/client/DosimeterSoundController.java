package mcjty.hazardous.client;

import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;

import java.util.OptionalDouble;

public class DosimeterSoundController {

    private static DosimeterLoopSound activeLoop;

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) {
            stopLoop();
            return;
        }

        OptionalDouble doseValue = RadiationOverlayRenderer.getDisplayedDose(minecraft.player);
        double mediumDose = Math.max(0.0, Config.DOSIMETER_MEDIUM_DOSE.get());
        boolean shouldPlay = doseValue.isPresent() && doseValue.getAsDouble() >= mediumDose;

        if (!shouldPlay) {
            stopLoop();
            return;
        }
        if (activeLoop != null && !activeLoop.isStopped()) {
            return;
        }

        activeLoop = new DosimeterLoopSound();
        minecraft.getSoundManager().play(activeLoop);
    }

    private static void stopLoop() {
        if (activeLoop != null) {
            activeLoop.stopLoop();
            activeLoop = null;
        }
    }

    private static class DosimeterLoopSound extends AbstractTickableSoundInstance {

        private DosimeterLoopSound() {
            super(Registration.DOSIMETER_BEEP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0f;
            this.pitch = 1.0f;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
        }

        @Override
        public void tick() {
        }

        private void stopLoop() {
            stop();
        }
    }
}
