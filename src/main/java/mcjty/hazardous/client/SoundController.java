package mcjty.hazardous.client;

import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;

import java.util.OptionalDouble;

public class SoundController {

    private static GeigerLoopSound activeGeigerLoop;
    private static SoundLevel activeGeigerLevel = SoundLevel.NONE;
    private static DosimeterLoopSound activeDosimeterLoop;
    private static boolean pillsSelectionInitialized = false;
    private static int lastSelectedSlot = -1;

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        updateGeigerSound(minecraft);
        updateDosimeterSound(minecraft);
        updatePillsShowSound(minecraft);
    }

    private static void updateGeigerSound(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) {
            stopGeigerLoop();
            return;
        }

        OptionalDouble radiationValue = RadiationOverlayRenderer.getDisplayedRadiation(minecraft.player);
        SoundLevel wantedLevel = pickGeigerSoundLevel(radiationValue);
        if (wantedLevel == activeGeigerLevel && activeGeigerLoop != null && !activeGeigerLoop.isStopped()) {
            return;
        }
        if (wantedLevel == activeGeigerLevel && wantedLevel == SoundLevel.NONE) {
            return;
        }

        stopGeigerLoop();
        if (wantedLevel == SoundLevel.NONE) {
            return;
        }

        SoundEvent sound = wantedLevel == SoundLevel.HIGH ? Registration.GEIGER_HIGHDOSE.get() : Registration.GEIGER_MEDIUMDOSE.get();
        activeGeigerLoop = new GeigerLoopSound(sound);
        minecraft.getSoundManager().play(activeGeigerLoop);
        activeGeigerLevel = wantedLevel;
    }

    private static void updateDosimeterSound(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) {
            stopDosimeterLoop();
            return;
        }

        OptionalDouble doseValue = RadiationOverlayRenderer.getDisplayedDose(minecraft.player);
        double mediumDose = Math.max(0.0, Config.DOSIMETER_MEDIUM_DOSE.get());
        boolean shouldPlay = doseValue.isPresent() && doseValue.getAsDouble() >= mediumDose;

        if (!shouldPlay) {
            stopDosimeterLoop();
            return;
        }
        if (activeDosimeterLoop != null && !activeDosimeterLoop.isStopped()) {
            return;
        }

        activeDosimeterLoop = new DosimeterLoopSound();
        minecraft.getSoundManager().play(activeDosimeterLoop);
    }

    private static void updatePillsShowSound(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            pillsSelectionInitialized = false;
            lastSelectedSlot = -1;
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }

        int selectedSlot = player.getInventory().selected;
        if (!pillsSelectionInitialized) {
            pillsSelectionInitialized = true;
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

    private static SoundLevel pickGeigerSoundLevel(OptionalDouble radiationValue) {
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

    private static void stopGeigerLoop() {
        if (activeGeigerLoop != null) {
            activeGeigerLoop.stopLoop();
            activeGeigerLoop = null;
        }
        activeGeigerLevel = SoundLevel.NONE;
    }

    private static void stopDosimeterLoop() {
        if (activeDosimeterLoop != null) {
            activeDosimeterLoop.stopLoop();
            activeDosimeterLoop = null;
        }
    }

    private static boolean isPills(ItemStack stack) {
        return stack.is(Registration.PILLS.get()) || stack.is(Registration.RESISTANCE_PILLS.get());
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
