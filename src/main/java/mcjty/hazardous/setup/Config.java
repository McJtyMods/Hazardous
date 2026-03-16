package mcjty.hazardous.setup;

import mcjty.hazardous.Hazardous;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

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
    public static ForgeConfigSpec.ConfigValue<String> GASMASK_PROTECTED_TYPE;
    public static ForgeConfigSpec.DoubleValue GASMASK_PROTECTION_LEVEL;
    public static ForgeConfigSpec.IntValue GASMASK_FILTER_RESTORE;
    public static ForgeConfigSpec.DoubleValue PILLS_DOSE_HEAL;
    public static ForgeConfigSpec.ConfigValue<String> RESISTANCE_PILLS_ATTRIBUTE;
    public static ForgeConfigSpec.DoubleValue RESISTANCE_PILLS_AMOUNT;
    public static ForgeConfigSpec.ConfigValue<String> GEIGER_DISPLAY_HAZARD_TYPE;
    public static ForgeConfigSpec.DoubleValue GEIGER_MAX_RADIATION;
    public static ForgeConfigSpec.DoubleValue GEIGER_MEDIUM_THRESSHOLD;
    public static ForgeConfigSpec.DoubleValue GEIGER_HIGH_TRESSHOLD;
    public static ForgeConfigSpec.DoubleValue GEIGER_SOUND_MEDIUM_MIN_RADIATION;
    public static ForgeConfigSpec.DoubleValue GEIGER_SOUND_HIGH_MIN_RADIATION;
    public static ForgeConfigSpec.DoubleValue GEIGER_SOUND_VOLUME;
    public static ForgeConfigSpec.DoubleValue GEIGER_NEEDLE_JITTER_ANGLE;
    public static ForgeConfigSpec.DoubleValue GEIGER_NEEDLE_JITTER_SPEED;
    public static ForgeConfigSpec.ConfigValue<String> GEIGER_HUD_ANCHOR;
    public static ForgeConfigSpec.DoubleValue GEIGER_HUD_SCALE;
    public static ForgeConfigSpec.IntValue GEIGER_HUD_OFFSET_X;
    public static ForgeConfigSpec.IntValue GEIGER_HUD_OFFSET_Y;
    public static ForgeConfigSpec.ConfigValue<String> DOSIMETER_DISPLAY_RESOURCE;
    public static ForgeConfigSpec.DoubleValue DOSIMETER_MAX_DOSE;
    public static ForgeConfigSpec.DoubleValue DOSIMETER_MEDIUM_DOSE;
    public static ForgeConfigSpec.DoubleValue DOSIMETER_HIGH_DOSE;
    public static ForgeConfigSpec.ConfigValue<String> DOSIMETER_HUD_ANCHOR;
    public static ForgeConfigSpec.DoubleValue DOSIMETER_HUD_SCALE;
    public static ForgeConfigSpec.IntValue DOSIMETER_HUD_OFFSET_X;
    public static ForgeConfigSpec.IntValue DOSIMETER_HUD_OFFSET_Y;

    private static final List<String> DEFAULT_ENABLED_HAZARD_TYPES = List.of();
    private static final List<String> DEFAULT_ENABLED_HAZARD_SOURCES = List.of();

    private static volatile Set<ResourceLocation> enabledHazardTypes = null;
    private static volatile Set<ResourceLocation> enabledHazardSources = null;

    public static void register() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("General settings").push(CATEGORY_GENERAL);

        ENABLED_HAZARD_TYPES = builder
                .comment("List of hazard type ids that are enabled. Only these will be used in the game")
                .defineList("enabledHazardTypes", DEFAULT_ENABLED_HAZARD_TYPES, s -> s instanceof String);
        ENABLED_HAZARD_SOURCES = builder
                .comment("List of hazard source ids that are enabled. Only these will be used in the game")
                .defineList("enabledHazardSources", DEFAULT_ENABLED_HAZARD_SOURCES, s -> s instanceof String);
        GASMASK_PROTECTED_TYPE = builder
                .comment("Hazard type id the gasmask protects against. Leave empty to disable protection")
                .define("gasmaskProtectedType", Hazardous.MODID + ":radioactive_type");
        GASMASK_PROTECTION_LEVEL = builder
                .comment("Fraction of input exposure blocked by a usable gasmask (0.0 - 1.0)")
                .defineInRange("gasmaskProtectionLevel", 0.75, 0.0, 1.0);
        GASMASK_FILTER_RESTORE = builder
                .comment("Durability restored when using a filter on a gasmask (right-click or crafting)")
                .defineInRange("gasmaskFilterRestore", 250, 1, 1_000_000);
        PILLS_DOSE_HEAL = builder
                .comment("Dose removed from all accumulated personal hazard dose entries when using pills")
                .defineInRange("pillsDoseHeal", 20.0, 0.0, 1_000_000.0);
        RESISTANCE_PILLS_ATTRIBUTE = builder
                .comment("Attribute id granted by anti-rad pills. Leave empty to disable")
                .define("resistancePillsAttribute", Hazardous.MODID + ":radioactive_type_resistance");
        RESISTANCE_PILLS_AMOUNT = builder
                .comment("Amount added to the configured attribute each time anti-rad pills are eaten")
                .defineInRange("resistancePillsAmount", 0.1, 0.0, 1.0);

        builder.pop();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, builder.build());

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder.comment("Client settings").push(CATEGORY_CLIENT);

        GEIGER_DISPLAY_HAZARD_TYPE = clientBuilder
                .comment("Hazard type id from client radiation data to display on the geiger dial (example: hazardous:radioactive_type). Leave empty to disable")
                .define("geigerDisplayHazardType", Hazardous.MODID + ":radioactive_type");
        GEIGER_MAX_RADIATION = clientBuilder
                .comment("Value treated as 100% dial fill")
                .defineInRange("geigerMaxRadiation", 100.0, 0.0001, 1_000_000.0);
        GEIGER_MEDIUM_THRESSHOLD = clientBuilder
                .comment("Radiation threshold where geiger pointer enters the yellow segment")
                .defineInRange("geigerMediumThresshold", 33.3, 0.0, 1_000_000.0);
        GEIGER_HIGH_TRESSHOLD = clientBuilder
                .comment("Radiation threshold where geiger pointer enters the red segment")
                .defineInRange("geigerHighTresshold", 66.6, 0.0, 1_000_000.0);
        GEIGER_SOUND_MEDIUM_MIN_RADIATION = clientBuilder
                .comment("Minimum displayed radiation required to play the medium geiger loop")
                .defineInRange("geigerSoundMediumMinRadiation", 1.0, 0.0, 1_000_000.0);
        GEIGER_SOUND_HIGH_MIN_RADIATION = clientBuilder
                .comment("Minimum displayed radiation required to switch from medium to high geiger loop")
                .defineInRange("geigerSoundHighMinRadiation", 25.0, 0.0, 1_000_000.0);
        GEIGER_SOUND_VOLUME = clientBuilder
                .comment("Volume multiplier for geiger loops")
                .defineInRange("geigerSoundVolume", 0.8, 0.0, 1.0);
        GEIGER_NEEDLE_JITTER_ANGLE = clientBuilder
                .comment("Maximum angle in degrees for geiger needle jitter when radiation is present")
                .defineInRange("geigerNeedleJitterAngle", 1.8, 0.0, 10.0);
        GEIGER_NEEDLE_JITTER_SPEED = clientBuilder
                .comment("Speed multiplier for geiger needle jitter animation")
                .defineInRange("geigerNeedleJitterSpeed", 1.1, 0.1, 10.0);
        GEIGER_HUD_ANCHOR = clientBuilder
                .comment("HUD anchor: top_left, top_center, top_right, center_left, center_right, bottom_left, bottom_center, bottom_right")
                .define("geigerHudAnchor", "top_right");
        GEIGER_HUD_SCALE = clientBuilder
                .comment("Scale factor for the geiger HUD (1.0 = normal, 2.0 = twice as large, 0.5 = half size)")
                .defineInRange("geigerHudScale", 1.0, 0.1, 10.0);
        GEIGER_HUD_OFFSET_X = clientBuilder
                .comment("Horizontal HUD offset from anchor in pixels")
                .defineInRange("geigerHudOffsetX", 8, -5000, 5000);
        GEIGER_HUD_OFFSET_Y = clientBuilder
                .comment("Vertical HUD offset from anchor in pixels")
                .defineInRange("geigerHudOffsetY", 8, -5000, 5000);
        DOSIMETER_DISPLAY_RESOURCE = clientBuilder
                .comment("Resource location from player dose data to display on the dosimeter (example: hazardous:radioactive_type). Leave empty to display sum of all dose entries")
                .define("dosimeterDisplayResource", Hazardous.MODID + ":radioactive_type");
        DOSIMETER_MAX_DOSE = clientBuilder
                .comment("Dose value treated as 100% dosimeter bar fill")
                .defineInRange("dosimeterMaxDose", 20.0, 0.0001, 1_000_000.0);
        DOSIMETER_MEDIUM_DOSE = clientBuilder
                .comment("Dose threshold where dosimeter bar changes from green to yellow/orange")
                .defineInRange("dosimeterMediumDose", 3.0, 0.0, 1_000_000.0);
        DOSIMETER_HIGH_DOSE = clientBuilder
                .comment("Dose threshold where dosimeter bar changes from yellow/orange to red")
                .defineInRange("dosimeterHighDose", 6.0, 0.0, 1_000_000.0);
        DOSIMETER_HUD_ANCHOR = clientBuilder
                .comment("HUD anchor: top_left, top_center, top_right, center_left, center_right, bottom_left, bottom_center, bottom_right")
                .define("dosimeterHudAnchor", "top_right");
        DOSIMETER_HUD_SCALE = clientBuilder
                .comment("Scale factor for the dosimeter HUD (1.0 = normal, 2.0 = twice as large, 0.5 = half size)")
                .defineInRange("dosimeterHudScale", 1.0, 0.1, 10.0);
        DOSIMETER_HUD_OFFSET_X = clientBuilder
                .comment("Horizontal HUD offset from anchor in pixels")
                .defineInRange("dosimeterHudOffsetX", 8, -5000, 5000);
        DOSIMETER_HUD_OFFSET_Y = clientBuilder
                .comment("Vertical HUD offset from anchor in pixels")
                .defineInRange("dosimeterHudOffsetY", 84, -5000, 5000);

        clientBuilder.pop();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientBuilder.build());
    }

    public static void onConfigLoading(ModConfigEvent.Loading event) {
        invalidateCaches(event.getConfig());
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        invalidateCaches(event.getConfig());
    }

    public static boolean isHazardTypeEnabled(ResourceLocation id) {
        return getEnabledHazardTypes().contains(id);
    }

    public static boolean isHazardSourceEnabled(ResourceLocation id) {
        return getEnabledHazardSources().contains(id);
    }

    private static Set<ResourceLocation> getEnabledHazardTypes() {
        Set<ResourceLocation> cached = enabledHazardTypes;
        if (cached == null) {
            Set<ResourceLocation> parsed = new HashSet<>();
            for (String s : ENABLED_HAZARD_TYPES.get()) {
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl == null) {
                    Hazardous.LOGGER.warn("Invalid hazard type id '{}' in config 'enabledHazardTypes'", s);
                    continue;
                }
                parsed.add(rl);
            }
            enabledHazardTypes = parsed;
            return parsed;
        }
        return cached;
    }

    private static Set<ResourceLocation> getEnabledHazardSources() {
        Set<ResourceLocation> cached = enabledHazardSources;
        if (cached == null) {
            Set<ResourceLocation> parsed = new HashSet<>();
            for (String s : ENABLED_HAZARD_SOURCES.get()) {
                ResourceLocation rl = ResourceLocation.tryParse(s);
                if (rl == null) {
                    Hazardous.LOGGER.warn("Invalid hazard source id '{}' in config 'enabledHazardSources'", s);
                    continue;
                }
                parsed.add(rl);
            }
            enabledHazardSources = parsed;
            return parsed;
        }
        return cached;
    }

    private static void invalidateCaches(ModConfig config) {
        if (!Hazardous.MODID.equals(config.getModId())) {
            return;
        }
        enabledHazardTypes = null;
        enabledHazardSources = null;
    }

    public static Optional<ResourceLocation> getGeigerDisplayHazardType() {
        String value = GEIGER_DISPLAY_HAZARD_TYPE.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(value));
    }

    public static Optional<ResourceLocation> getGasmaskProtectedType() {
        String value = GASMASK_PROTECTED_TYPE.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(value));
    }

    public static Optional<ResourceLocation> getDosimeterDisplayResource() {
        String value = DOSIMETER_DISPLAY_RESOURCE.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(value));
    }

    public static Optional<ResourceLocation> getResistancePillsAttribute() {
        String value = RESISTANCE_PILLS_ATTRIBUTE.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(value));
    }

    public static GeigerHudAnchor getGeigerHudAnchor() {
        return GeigerHudAnchor.fromName(GEIGER_HUD_ANCHOR.get());
    }

    public static GeigerHudAnchor getDosimeterHudAnchor() {
        return GeigerHudAnchor.fromName(DOSIMETER_HUD_ANCHOR.get());
    }

    public enum GeigerHudAnchor {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        CENTER_LEFT,
        CENTER_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT;

        public static GeigerHudAnchor fromName(String value) {
            if (value == null) {
                return TOP_RIGHT;
            }
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "top_left" -> TOP_LEFT;
                case "top_center" -> TOP_CENTER;
                case "bottom_left" -> BOTTOM_LEFT;
                case "center_left" -> CENTER_LEFT;
                case "center_right" -> CENTER_RIGHT;
                case "bottom_center" -> BOTTOM_CENTER;
                case "bottom_right" -> BOTTOM_RIGHT;
                default -> TOP_RIGHT;
            };
        }
    }
}
