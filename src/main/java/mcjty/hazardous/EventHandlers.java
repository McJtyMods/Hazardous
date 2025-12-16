package mcjty.hazardous;

import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.data.HazardManager;
import mcjty.hazardous.data.PlayerDoseData;
import mcjty.hazardous.data.PlayerDoseDispatcher;
import mcjty.hazardous.data.objects.EffectEntry;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.hazardous.network.PacketRadiationAtPos;
import mcjty.hazardous.setup.DoseSetup;
import mcjty.hazardous.setup.Messages;
import mcjty.lib.varia.Tools;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;

public class EventHandlers {

    public static void commandRegister(RegisterCommandsEvent event) {
        mcjty.hazardous.commands.ModCommands.register(event.getDispatcher());
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerDoseData.class);
    }

    public static void onEntityConstructing(AttachCapabilitiesEvent<Entity> event){
        if (event.getObject() instanceof Player) {
            if (!event.getCapabilities().containsKey(DoseSetup.PLAYER_DOSE_KEY) && !event.getObject().getCapability(DoseSetup.PLAYER_DOSE).isPresent()) {
                event.addCapability(DoseSetup.PLAYER_DOSE_KEY, new PlayerDoseDispatcher());
            } else {
                throw new IllegalStateException(event.getObject().toString());
            }
        }
    }

    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(DoseSetup.PLAYER_DOSE).ifPresent(oldStore -> {
                event.getEntity().getCapability(DoseSetup.PLAYER_DOSE).ifPresent(newStore -> {
                    newStore.copyFrom(oldStore);
                });
            });
        }
    }

    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player.level().isClientSide) {
            return;
        }
        Level level = event.player.level();
        Registry<HazardType> types = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);

        PlayerDoseDispatcher.getPlayerDose(event.player).ifPresent(store -> {
            long gameTime = level.getGameTime();
            Map<ResourceLocation, Double> forClient = new HashMap<>();
            for (HazardType type : types) {
                ResourceLocation typeId = types.getKey(type);
                if (typeId == null) {
                    continue;
                }
                int intervalTicks = type.exposure().applyIntervalTicks();
                if (intervalTicks > 0 && gameTime % intervalTicks != 0) {
                    // Skip dose and effect handling this tick for this hazard type
                    continue;
                }

                double input = HazardManager.getHazardValue(type, level, event.player);
                double current = store.getDose(typeId);
                double value = type.exposure().calculate(input, current);

                forClient.put(typeId, input);

                store.setDose(typeId, value);

                for (EffectEntry effect : type.effects()) {
                    // Evaluate trigger with the current dose value and apply action if it fires
                    if (effect.trigger().shouldTrigger(value, event.player.getRandom())) {
                        double factor = effect.trigger().factor(value);
                        effect.action().apply(event.player, value, factor);
                    }
                }
            }
            if (!forClient.isEmpty()) {

            }
        });
    }
}
