package mcjty.hazardous.data;

import mcjty.hazardous.Hazardous;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
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
                    new HazardSource.Association.EntityType(new ResourceLocation("minecraft", "zombie"))
            ),
            new ResourceLocation(Hazardous.MODID, "nether_wastes_biome"),
            new HazardSource(
                    new ResourceLocation(Hazardous.MODID, "solar_burn"),
                    new HazardSource.Association.Biome(new ResourceLocation("minecraft", "nether_wastes"))
            ),
            new ResourceLocation(Hazardous.MODID, "spawn_contact_point"),
            new HazardSource(
                    new ResourceLocation(Hazardous.MODID, "contact_burn"),
                    new HazardSource.Association.Locations(
                            new ResourceLocation("minecraft", "overworld"),
                            List.of(new BlockPos(0, 64, 0))
                    )
            )
    );
}
