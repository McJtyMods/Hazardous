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

    private static final List<String> DEFAULT_ENABLED_HAZARD_TYPES = List.of(
            Hazardous.MODID + ":solar_burn",
            Hazardous.MODID + ":radioactive_source",
            Hazardous.MODID + ":lava_heat"
    );

    private static Set<ResourceLocation> enabledHazardTypes = null;

    public static void register() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push(CATEGORY_GENERAL);

        ENABLED_HAZARD_TYPES = builder
                .comment("List of hazard type ids that are enabled. Only these will be used in the game")
                .defineList("enabledHazardTypes", DEFAULT_ENABLED_HAZARD_TYPES, s -> s instanceof String);

        builder.pop();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, builder.build());
    }

    public static boolean isHazardTypeEnabled(ResourceLocation id) {
        return getEnabledHazardTypes().contains(id);
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
}
