package mcjty.hazardous.data;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.objects.Action;
import mcjty.hazardous.data.objects.EffectEntry;
import mcjty.hazardous.data.objects.Scaling;
import mcjty.hazardous.data.objects.Trigger;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Some example effect entries used for datagen. These are generic and meant to
 * pair with the example hazard types and sources defined in this module.
 */
public class DefaultEffectEntries {

    public static final Map<ResourceLocation, EffectEntry> DEFAULT_EFFECT_ENTRIES = Map.of(
            // Mild weakness when solar burn is noticeable
            new ResourceLocation(Hazardous.MODID, "solar_weakness"),
            new EffectEntry(
                    new Trigger.Threshold(0.05, 0.01),
                    new Action.Potion(
                            new ResourceLocation("minecraft", "weakness"),
                            200,
                            0,
                            false,
                            true,
                            true,
                            new Scaling.Constant(1.0)
                    )
            ),

            // Set player on fire a bit when solar burn is strong
            new ResourceLocation(Hazardous.MODID, "solar_ignite"),
            new EffectEntry(
                    new Trigger.Threshold(0.2, 0.02),
                    new Action.Fire(
                            2,
                            new Scaling.Linear01(0.2, 1.0)
                    )
            ),

            // Radiation causes magic damage scaling with intensity
            new ResourceLocation(Hazardous.MODID, "radiation_damage"),
            new EffectEntry(
                    new Trigger.Range(0.10, 1.0),
                    new Action.Damage(
                            new ResourceLocation("minecraft", "magic"),
                            1.0,
                            new Scaling.Linear01(0.10, 1.0)
                    )
            ),

            // Client geiger click effect with probability increasing with intensity
            new ResourceLocation(Hazardous.MODID, "radiation_geiger"),
            new EffectEntry(
                    new Trigger.Probability(new Scaling.Linear01(0.05, 1.0)),
                    new Action.ClientFx(
                            "geiger",
                            new Scaling.Constant(1.0),
                            20
                    )
            )
    );
}
