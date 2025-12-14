package mcjty.hazardous.data;

import mcjty.hazardous.Hazardous;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class DefaultHazardTypes {

    public static final Map<ResourceLocation, HazardType> DEFAULT_HAZARD_TYPES = Map.of(
            new ResourceLocation(Hazardous.MODID, "solar_burn"),
            new HazardType(
                    new HazardType.Transmission.Sky(
                            0.12,
                            true,
                            0.25,
                            0.1,
                            0.0,
                            0.05),
                    HazardType.Falloff.None.INSTANCE,
                    new HazardType.Blocking.SimpleOcclusion(
                            0.6,
                            0.75,
                            true),
                    new HazardType.Exposure(
                            20,
                            true,
                            0.02,
                            120.0)),
            new ResourceLocation(Hazardous.MODID, "radioactive_source"),
            new HazardType(
                    new HazardType.Transmission.Point(
                            1.0,
                            12,
                            true,
                            0.05),
                    new HazardType.Falloff.Exponential(0.18),
                    new HazardType.Blocking.Absorption(
                            new ResourceLocation(Hazardous.MODID, "absorption_hints"),
                            0.2),
                    new HazardType.Exposure(
                            10,
                            true,
                            0.04,
                            200.0)),
            new ResourceLocation(Hazardous.MODID, "contact_burn"),
            new HazardType(
                    new HazardType.Transmission.Contact(
                            2.5),
                    HazardType.Falloff.None.INSTANCE,
                    HazardType.Blocking.None.INSTANCE,
                    new HazardType.Exposure(
                            1,
                            false,
                            0.0,
                            10.0))
    );
}
