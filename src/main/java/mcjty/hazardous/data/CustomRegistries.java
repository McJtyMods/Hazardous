package mcjty.hazardous.data;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.objects.EffectEntry;
import mcjty.hazardous.data.objects.HazardSource;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.hazardous.setup.HazardAttributes;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DataPackRegistryEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.stream.Collectors;

public class CustomRegistries {

    public static final ResourceKey<Registry<HazardType>> HAZARD_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Hazardous.MODID, "hazardtypes"));
    public static final DeferredRegister<HazardType> HAZARD_TYPE_DEFERRED_REGISTER = DeferredRegister.create(HAZARD_TYPE_REGISTRY_KEY, Hazardous.MODID);
    public static final ResourceKey<Registry<HazardSource>> HAZARD_SOURCE_REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Hazardous.MODID, "hazardsources"));
    public static final DeferredRegister<HazardSource> HAZARD_SOURCE_DEFERRED_REGISTER = DeferredRegister.create(HAZARD_SOURCE_REGISTRY_KEY, Hazardous.MODID);
    public static final ResourceKey<Registry<EffectEntry>> EFFECT_ENTRY_REGISTRY_KEY = ResourceKey.createRegistryKey(new ResourceLocation(Hazardous.MODID, "effectentries"));
    public static final DeferredRegister<EffectEntry> EFFECT_ENTRY_DEFERRED_REGISTER = DeferredRegister.create(EFFECT_ENTRY_REGISTRY_KEY, Hazardous.MODID);

    public static void init(IEventBus bus) {
        HAZARD_TYPE_DEFERRED_REGISTER.register(bus);
        HAZARD_SOURCE_DEFERRED_REGISTER.register(bus);
        EFFECT_ENTRY_DEFERRED_REGISTER.register(bus);
        bus.addListener((DataPackRegistryEvent.NewRegistry event) -> {
            event.dataPackRegistry(HAZARD_TYPE_REGISTRY_KEY, HazardType.CODEC);
            event.dataPackRegistry(HAZARD_SOURCE_REGISTRY_KEY, HazardSource.CODEC);
            event.dataPackRegistry(EFFECT_ENTRY_REGISTRY_KEY, EffectEntry.CODEC);
        });
    }

    public static void validateSources(Map<ResourceLocation, HazardType> hazardTypes,
                                Map<ResourceLocation, HazardSource> sources) {
        for (Map.Entry<ResourceLocation, HazardSource> entry : sources.entrySet()) {
            ResourceLocation sourceId = entry.getKey();
            HazardSource s = entry.getValue();
            HazardType type = hazardTypes.get(s.hazardType());
            if (type == null) {
                Hazardous.LOGGER.error("HazardSource refers to missing HazardType '{}'", s.hazardType());
                throw new RuntimeException("HazardSource refers to missing HazardType '" + s.hazardType() + "'!");
            }
            var transmission = s.transmission();
            if (!transmission.supportedAssociations().contains(s.association().kind())) {
                Hazardous.LOGGER.error("Incompatible hazard source: type='{}' transmission='{}' does not support association='{}' ({})",
                        s.hazardType(),
                        transmission.getClass().getSimpleName(),
                        s.association().kind(),
                        s);
                throw new RuntimeException("Incompatible hazard source '" + s.association().kind() + "' for type '" + s.hazardType() + "'!");
            }
            if (s.association() instanceof HazardSource.Association.City city
                    && transmission instanceof HazardSource.Transmission.Point
                    && city.buildings().isEmpty()
                    && city.multibuildings().isEmpty()) {
                Hazardous.LOGGER.error("HazardSource '{}' uses city + point without buildings or multibuildings; Hazardous needs explicit building centers for point transmission", sourceId);
                throw new RuntimeException("HazardSource '" + sourceId + "' uses city + point without buildings or multibuildings!");
            }
            if (s.association() instanceof HazardSource.Association.City city
                    && !(s.falloff() instanceof HazardSource.Falloff.None)
                    && transmission instanceof HazardSource.Transmission.Sky
                    && city.buildings().isEmpty()
                    && city.multibuildings().isEmpty()) {
                Hazardous.LOGGER.warn("HazardSource '{}' uses city + sky falloff without buildings or multibuildings; Hazardous cannot derive a building center and will fall back to city-wide behavior", sourceId);
            }
        }
    }

    public static void validateHazardTypes(Map<ResourceLocation, HazardType> hazardTypes) {
        for (Map.Entry<ResourceLocation, HazardType> entry : hazardTypes.entrySet()) {
            ResourceLocation hazardTypeId = entry.getKey();
            HazardType hazardType = entry.getValue();
            ResourceLocation attributeId = hazardType.resistanceAttribute();
            if (attributeId == null && HazardAttributes.getBuiltInHazardTypesWithResistanceAttributes().contains(hazardTypeId)) {
                attributeId = HazardAttributes.getDefaultResistanceAttributeId(hazardTypeId);
            }
            if (attributeId != null && !ForgeRegistries.ATTRIBUTES.containsKey(attributeId)) {
                Hazardous.LOGGER.error("HazardType '{}' refers to missing resistance attribute '{}'", hazardTypeId, attributeId);
                throw new RuntimeException("HazardType '" + hazardTypeId + "' refers to missing resistance attribute '" + attributeId + "'!");
            }
        }
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return; // Only validate after full registry reloads, not on per-player syncs
        }

        RegistryAccess access = event.getPlayerList().getServer().registryAccess();
        Registry<HazardType> hazardTypes = access.registryOrThrow(HAZARD_TYPE_REGISTRY_KEY);
        Registry<HazardSource> hazardSources = access.registryOrThrow(HAZARD_SOURCE_REGISTRY_KEY);

        Map<ResourceLocation, HazardType> hazardTypeMap = hazardTypes.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey().location(), Map.Entry::getValue));
        Map<ResourceLocation, HazardSource> sources = hazardSources.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey().location(), Map.Entry::getValue));

        validateHazardTypes(hazardTypeMap);
        validateSources(hazardTypeMap, sources);
    }

}
