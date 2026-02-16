package mcjty.hazardous.setup;

import mcjty.hazardous.Hazardous;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class Config {

    private static final String CATEGORY_GENERAL = "general";
    private static final String CATEGORY_CLIENT = "client";

    public static ForgeConfigSpec.ConfigValue<List<? extends String>> ENABLED_HAZARD_TYPES;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> ENABLED_HAZARD_SOURCES;
    public static ForgeConfigSpec.ConfigValue<String> GASMASK_PROTECTED_SOURCE;
    public static ForgeConfigSpec.DoubleValue GASMASK_PROTECTION_LEVEL;
    public static ForgeConfigSpec.IntValue GASMASK_FILTER_RESTORE;
    public static ForgeConfigSpec.DoubleValue PILLS_DOSE_HEAL;
    public static ForgeConfigSpec.ConfigValue<String> GEIGER_DISPLAY_RESOURCE;
    public static ForgeConfigSpec.DoubleValue GEIGER_MAX_RADIATION;
    public static ForgeConfigSpec.ConfigValue<String> GEIGER_HUD_ANCHOR;
    public static ForgeConfigSpec.IntValue GEIGER_HUD_OFFSET_X;
    public static ForgeConfigSpec.IntValue GEIGER_HUD_OFFSET_Y;

    private static final List<String> DEFAULT_ENABLED_HAZARD_TYPES = List.of();
    private static final List<String> DEFAULT_ENABLED_HAZARD_SOURCES = List.of();

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
        GASMASK_PROTECTED_SOURCE = builder
                .comment("Hazard type id the gasmask protects against. Leave empty to disable protection")
                .define("gasmaskProtectedSource", Hazardous.MODID + ":radioactive_source");
        GASMASK_PROTECTION_LEVEL = builder
                .comment("Fraction of input exposure blocked by a usable gasmask (0.0 - 1.0)")
                .defineInRange("gasmaskProtectionLevel", 0.75, 0.0, 1.0);
        GASMASK_FILTER_RESTORE = builder
                .comment("Durability restored when using a filter on a gasmask (right-click or crafting)")
                .defineInRange("gasmaskFilterRestore", 250, 1, 1_000_000);
        PILLS_DOSE_HEAL = builder
                .comment("Dose removed from all accumulated personal hazard dose entries when using pills")
                .defineInRange("pillsDoseHeal", 20.0, 0.0, 1_000_000.0);

        builder.pop();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, builder.build());

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder.comment("Client settings").push(CATEGORY_CLIENT);

        GEIGER_DISPLAY_RESOURCE = clientBuilder
                .comment("Resource location from client radiation data to display on the geiger dial (example: hazardous:radioactive_source). Leave empty to disable")
                .define("geigerDisplayResource", Hazardous.MODID + ":radioactive_source");
        GEIGER_MAX_RADIATION = clientBuilder
                .comment("Value treated as 100% dial fill")
                .defineInRange("geigerMaxRadiation", 100.0, 0.0001, 1_000_000.0);
        GEIGER_HUD_ANCHOR = clientBuilder
                .comment("HUD anchor: top_left, top_right, bottom_left, bottom_right")
                .define("geigerHudAnchor", "top_right");
        GEIGER_HUD_OFFSET_X = clientBuilder
                .comment("Horizontal HUD offset from anchor in pixels")
                .defineInRange("geigerHudOffsetX", 8, -5000, 5000);
        GEIGER_HUD_OFFSET_Y = clientBuilder
                .comment("Vertical HUD offset from anchor in pixels")
                .defineInRange("geigerHudOffsetY", 8, -5000, 5000);

        clientBuilder.pop();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientBuilder.build());
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

    public static Optional<ResourceLocation> getGeigerDisplayResource() {
        String value = GEIGER_DISPLAY_RESOURCE.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(value));
    }

    public static Optional<ResourceLocation> getGasmaskProtectedSource() {
        String value = GASMASK_PROTECTED_SOURCE.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(value));
    }

    public static GeigerHudAnchor getGeigerHudAnchor() {
        return GeigerHudAnchor.fromName(GEIGER_HUD_ANCHOR.get());
    }

    public enum GeigerHudAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        public static GeigerHudAnchor fromName(String value) {
            if (value == null) {
                return TOP_RIGHT;
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "top_left" -> TOP_LEFT;
                case "bottom_left" -> BOTTOM_LEFT;
                case "bottom_right" -> BOTTOM_RIGHT;
                default -> TOP_RIGHT;
            };
        }
    }
}
