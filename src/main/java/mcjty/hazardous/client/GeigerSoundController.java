package mcjty.hazardous.client;

import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;

import java.util.OptionalDouble;

public class GeigerSoundController {

    private static GeigerLoopSound activeLoop;
    private static SoundLevel activeLevel = SoundLevel.NONE;

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) {
            stopLoop();
            return;
        }

        OptionalDouble radiationValue = RadiationOverlayRenderer.getDisplayedRadiation(minecraft.player);
        SoundLevel wantedLevel = pickSoundLevel(radiationValue);
        if (wantedLevel == activeLevel && activeLoop != null && !activeLoop.isStopped()) {
            return;
        }
        if (wantedLevel == activeLevel && wantedLevel == SoundLevel.NONE) {
            return;
        }

        stopLoop();
        if (wantedLevel != SoundLevel.NONE) {
            SoundEvent sound = wantedLevel == SoundLevel.HIGH ? Registration.GEIGER_HIGHDOSE.get() : Registration.GEIGER_MEDIUMDOSE.get();
            activeLoop = new GeigerLoopSound(sound);
            minecraft.getSoundManager().play(activeLoop);
            activeLevel = wantedLevel;
        }
    }

    private static SoundLevel pickSoundLevel(OptionalDouble radiationValue) {
        if (radiationValue.isEmpty()) {
            return SoundLevel.NONE;
        }
        double radiation = radiationValue.getAsDouble();
        double mediumMin = Math.max(0.0, Config.GEIGER_SOUND_MEDIUM_MIN_RADIATION.get());
        double highMin = Math.max(mediumMin, Config.GEIGER_SOUND_HIGH_MIN_RADIATION.get());
        if (radiation >= highMin) {
            return SoundLevel.HIGH;
        }
        if (radiation >= mediumMin) {
            return SoundLevel.MEDIUM;
        }
        return SoundLevel.NONE;
    }

    private static void stopLoop() {
        if (activeLoop != null) {
            activeLoop.stopLoop();
            activeLoop = null;
        }
        activeLevel = SoundLevel.NONE;
    }

    private enum SoundLevel {
        NONE,
        MEDIUM,
        HIGH
    }

    private static class GeigerLoopSound extends AbstractTickableSoundInstance {

        private GeigerLoopSound(SoundEvent soundEvent) {
            super(soundEvent, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = getConfiguredVolume();
            this.pitch = 1.0f;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
        }

        @Override
        public void tick() {
            this.volume = getConfiguredVolume();
        }

        private void stopLoop() {
            stop();
        }

        private float getConfiguredVolume() {
            return (float) Math.max(0.0, Math.min(1.0, Config.GEIGER_SOUND_VOLUME.get()));
        }
    }
}
