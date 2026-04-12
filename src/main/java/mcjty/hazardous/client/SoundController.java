package mcjty.hazardous.client;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.items.GasmaskItem;
import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.OptionalDouble;

public class SoundController {

    private static GeigerLoopSound activeGeigerLoop;
    private static SoundLevel activeGeigerLevel = SoundLevel.NONE;
    private static DosimeterLoopSound activeDosimeterLoop;
    private static GasmaskLoopSound activeGasmaskLoop;
    private static GasmaskLoopType activeGasmaskLoopType = GasmaskLoopType.NONE;
    private static boolean pillsSelectionInitialized = false;
    private static int lastSelectedSlot = -1;
    private static boolean gasmaskStateInitialized = false;
    private static int lastEquippedGasmaskDamage = -1;
    private static boolean filterRepairPending = false;
    private static int filterRepairTimeout = 0;

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (Config.getClampedMasterSoundVolume() <= 0.0f) {
            stopGeigerLoop();
            stopDosimeterLoop();
            stopGasmaskLoop();
            filterRepairPending = false;
            filterRepairTimeout = 0;
            return;
        }
        updateGeigerSound(minecraft);
        updateDosimeterSound(minecraft);
        updateGasmaskBreathingSound(minecraft);
        updateFilterRepairSound(minecraft);
        updatePillsShowSound(minecraft);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        PlayerInteractEvent.RightClickItem rightClick = event;
        if (!(rightClick.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (!rightClick.getItemStack().is(Registration.FILTER.get())) {
            return;
        }
        if (Config.getClampedMasterSoundVolume() <= 0.0f) {
            return;
        }
        if (getFilterReplenishVolume() <= 0.0f) {
            return;
        }
        if (GasmaskItem.findEquippedGasmask(player, stack -> stack.getDamageValue() > 0).isEmpty()) {
            return;
        }

        filterRepairPending = true;
        filterRepairTimeout = 10;
    }

    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }

        ResourceLocation location = sound.getLocation();
        if (!Hazardous.MODID.equals(location.getNamespace())) {
            return;
        }

        float masterVolume = Config.getClampedMasterSoundVolume();
        if (masterVolume <= 0.0f) {
            event.setSound(null);
            return;
        }

        if (sound instanceof TickableSoundInstance) {
            return;
        }

        event.setSound(new MasterVolumeSoundInstance(sound));
    }

    private static void updateGeigerSound(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) {
            stopGeigerLoop();
            return;
        }
        if (getGeigerVolume() <= 0.0f) {
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
        if (getDosimeterVolume() <= 0.0f) {
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

    private static void updateGasmaskBreathingSound(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            stopGasmaskLoop();
            return;
        }

        GasmaskLoopType wantedLoopType = pickGasmaskLoopType(player);
        if (wantedLoopType == activeGasmaskLoopType && activeGasmaskLoop != null && !activeGasmaskLoop.isStopped()) {
            return;
        }
        if (wantedLoopType == GasmaskLoopType.NONE) {
            stopGasmaskLoop();
            return;
        }

        stopGasmaskLoop();
        activeGasmaskLoop = new GasmaskLoopSound(wantedLoopType.soundEvent());
        minecraft.getSoundManager().play(activeGasmaskLoop);
        activeGasmaskLoopType = wantedLoopType;
    }

    private static void updateFilterRepairSound(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            resetGasmaskTracking();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        if (getFilterReplenishVolume() <= 0.0f) {
            filterRepairPending = false;
            filterRepairTimeout = 0;
            return;
        }

        int currentDamage = GasmaskItem.findEquippedGasmask(player)
                .map(ItemStack::getDamageValue)
                .orElse(-1);

        if (!gasmaskStateInitialized) {
            gasmaskStateInitialized = true;
            lastEquippedGasmaskDamage = currentDamage;
            return;
        }

        if (filterRepairPending && currentDamage >= 0 && lastEquippedGasmaskDamage >= 0 && currentDamage < lastEquippedGasmaskDamage) {
            playLocalSound(player, Registration.FILTER_REPLENISH.get(), getFilterReplenishVolume(), 1.0f);
            filterRepairPending = false;
            filterRepairTimeout = 0;
        } else if (filterRepairPending && --filterRepairTimeout <= 0) {
            filterRepairPending = false;
            filterRepairTimeout = 0;
        }

        lastEquippedGasmaskDamage = currentDamage;
    }

    private static void updatePillsShowSound(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            pillsSelectionInitialized = false;
            lastSelectedSlot = -1;
            resetGasmaskTracking();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        float pillsShowVolume = getPillsShowVolume();
        if (pillsShowVolume <= 0.0f) {
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
                pillsShowVolume,
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

    private static void stopGasmaskLoop() {
        if (activeGasmaskLoop != null) {
            activeGasmaskLoop.stopLoop();
            activeGasmaskLoop = null;
        }
        activeGasmaskLoopType = GasmaskLoopType.NONE;
    }

    private static void resetGasmaskTracking() {
        stopGasmaskLoop();
        gasmaskStateInitialized = false;
        lastEquippedGasmaskDamage = -1;
        filterRepairPending = false;
        filterRepairTimeout = 0;
    }

    private static boolean isPills(ItemStack stack) {
        return stack.is(Registration.PILLS.get()) || stack.is(Registration.RESISTANCE_PILLS.get());
    }

    private static float getGasmaskBreathingVolume() {
        return applyMasterVolume((float) Math.max(0.0, Math.min(1.0, Config.GASMASK_BREATHING_VOLUME.get())));
    }

    private static float getFilterReplenishVolume() {
        return (float) Math.max(0.0, Math.min(1.0, Config.FILTER_REPLENISH_VOLUME.get()));
    }

    private static float getGeigerVolume() {
        return applyMasterVolume((float) Math.max(0.0, Math.min(1.0, Config.GEIGER_SOUND_VOLUME.get())));
    }

    private static float getDosimeterVolume() {
        return applyMasterVolume(1.0f);
    }

    private static float getPillsShowVolume() {
        return 0.6f;
    }

    private static GasmaskLoopType pickGasmaskLoopType(LocalPlayer player) {
        if (getGasmaskBreathingVolume() <= 0.0f) {
            return GasmaskLoopType.NONE;
        }

        return GasmaskItem.findEquippedGasmask(player)
                .map(stack -> GasmaskItem.getRemainingDurability(stack) > 0 ? GasmaskLoopType.BREATHING : GasmaskLoopType.CHOKING)
                .orElse(GasmaskLoopType.NONE);
    }

    private static void playLocalSound(LocalPlayer player, SoundEvent sound, float volume, float pitch) {
        if (volume <= 0.0f) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        minecraft.level.playLocalSound(
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                pitch,
                false
        );
    }

    private static float applyMasterVolume(float volume) {
        return volume * Config.getClampedMasterSoundVolume();
    }

    private enum SoundLevel {
        NONE,
        MEDIUM,
        HIGH
    }

    private enum GasmaskLoopType {
        NONE(null),
        BREATHING(Registration.GASMASK_BREATHING.get()),
        CHOKING(Registration.GASMASK_CHOKING.get());

        private final SoundEvent soundEvent;

        GasmaskLoopType(SoundEvent soundEvent) {
            this.soundEvent = soundEvent;
        }

        private SoundEvent soundEvent() {
            return soundEvent;
        }
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
            return getGeigerVolume();
        }
    }

    private static class DosimeterLoopSound extends AbstractTickableSoundInstance {

        private DosimeterLoopSound() {
            super(Registration.DOSIMETER_BEEP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = getDosimeterVolume();
            this.pitch = 1.0f;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
        }

        @Override
        public void tick() {
            this.volume = getDosimeterVolume();
        }

        private void stopLoop() {
            stop();
        }
    }

    private static class GasmaskLoopSound extends AbstractTickableSoundInstance {

        private GasmaskLoopSound(SoundEvent soundEvent) {
            super(soundEvent, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = getGasmaskBreathingVolume();
            this.pitch = 1.0f;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
        }

        @Override
        public void tick() {
            this.volume = getGasmaskBreathingVolume();
        }

        private void stopLoop() {
            stop();
        }
    }

    private static class MasterVolumeSoundInstance implements SoundInstance {
        private final SoundInstance delegate;

        private MasterVolumeSoundInstance(SoundInstance delegate) {
            this.delegate = delegate;
        }

        @Override
        public ResourceLocation getLocation() {
            return delegate.getLocation();
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager soundManager) {
            return delegate.resolve(soundManager);
        }

        @Override
        public Sound getSound() {
            return delegate.getSound();
        }

        @Override
        public SoundSource getSource() {
            return delegate.getSource();
        }

        @Override
        public boolean isLooping() {
            return delegate.isLooping();
        }

        @Override
        public boolean isRelative() {
            return delegate.isRelative();
        }

        @Override
        public int getDelay() {
            return delegate.getDelay();
        }

        @Override
        public float getVolume() {
            return delegate.getVolume() * Config.getClampedMasterSoundVolume();
        }

        @Override
        public float getPitch() {
            return delegate.getPitch();
        }

        @Override
        public double getX() {
            return delegate.getX();
        }

        @Override
        public double getY() {
            return delegate.getY();
        }

        @Override
        public double getZ() {
            return delegate.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return delegate.getAttenuation();
        }

        @Override
        public boolean canPlaySound() {
            return delegate.canPlaySound();
        }

        @Override
        public boolean canStartSilent() {
            return delegate.canStartSilent();
        }
    }
}
