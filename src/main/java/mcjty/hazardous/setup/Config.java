package mcjty.hazardous.setup;

import mcjty.hazardous.Hazardous;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {

    private static final String CATEGORY_GENERAL = "general";

    public static ForgeConfigSpec.ConfigValue<List<? extends String>> ENABLED_HAZARD_TYPES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> ENABLED_HAZARD_SOURCES;

    private static final List<String> DEFAULT_ENABLED_HAZARD_TYPES = List.of(
            Hazardous.MODID + ":solar_burn",
            Hazardous.MODID + ":radioactive_source",
            Hazardous.MODID + ":lostcity_radiation",
            Hazardous.MODID + ":lava_heat"
    );
    private static final List<String> DEFAULT_ENABLED_HAZARD_SOURCES = List.of(
            Hazardous.MODID + ":overworld_solar",
            Hazardous.MODID + ":radioactive_zombie",
            Hazardous.MODID + ":lostcity_buildings",
            Hazardous.MODID + ":near_lava"
    );

    private static Set<ResourceLocation> enabledHazardTypes = null;
    private static Set<ResourceLocation> enabledHazardSources = null;

    public static void register() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push(CATEGORY_GENERAL);

        ENABLED_HAZARD_TYPES = builder
                .comment("List of hazard type ids that are enabled. Only these will be used in the game")
                .defineList("enabledHazardTypes", DEFAULT_ENABLED_HAZARD_TYPES, s -> s instanceof String);
        ENABLED_HAZARD_SOURCES = builder
                .comment("List of hazard source ids that are enabled. Only these will be used in the game")
                .defineList("enabledHazardSources", DEFAULT_ENABLED_HAZARD_SOURCES, s -> s instanceof String);

        builder.pop();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, builder.build());
    }

    public static boolean isHazardTypeEnabled(ResourceLocation id) {
        return getEnabledHazardTypes().contains(id);
    }

    public static boolean isHazardSourceEnabled(ResourceLocation id) {
        return getEnabledHazardSources().contains(id);
    }

    private static Set<ResourceLocation> getEnabledHazardTypes() {
        if (enabledHazardTypes == null) {
            enabledHazardTypes = new HashSet<>();
            for (String s : ENABLED_HAZARD_TYPES.get()) {
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl == null) {
                    Hazardous.LOGGER.warn("Invalid hazard type id '{}' in config 'enabledHazardTypes'", s);
                    continue;
                }
                enabledHazardTypes.add(rl);
            }
        }
        return enabledHazardTypes;
    }

    private static Set<ResourceLocation> getEnabledHazardSources() {
        if (enabledHazardSources == null) {
            enabledHazardSources = new HashSet<>();
            for (String s : ENABLED_HAZARD_SOURCES.get()) {
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl == null) {
                    Hazardous.LOGGER.warn("Invalid hazard source id '{}' in config 'enabledHazardSources'", s);
                    continue;
                }
                enabledHazardSources.add(rl);
            }
        }
        return enabledHazardSources;
    }
}
