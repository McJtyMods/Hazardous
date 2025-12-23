package mcjty.hazardous.data;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.objects.HazardSource;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class DefaultHazardSources {

    public static final Map<ResourceLocation, HazardSource> DEFAULT_HAZARD_SOURCES = Map.of(
            new ResourceLocation(Hazardous.MODID, "overworld_solar"),
            new HazardSource(
                    new ResourceLocation(Hazardous.MODID, "solar_burn"),
                    new HazardSource.Association.Level(new ResourceLocation("minecraft", "overworld"))
            ),
            new ResourceLocation(Hazardous.MODID, "radioactive_zombie"),
            new HazardSource(
                    new ResourceLocation(Hazardous.MODID, "radioactive_source"),
                    new HazardSource.Association.EntityType(new ResourceLocation("minecraft", "zombie"), 3.0)
            ),
            new ResourceLocation(Hazardous.MODID, "near_lava"),
            new HazardSource(
                    new ResourceLocation(Hazardous.MODID, "lava_heat"),
                    new HazardSource.Association.Block(new ResourceLocation("minecraft", "lava"), false, 4.0)
            )
    );
}
