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

            // Solar burn can temporarily darken vision
            new ResourceLocation(Hazardous.MODID, "solar_darken"),
            new EffectEntry(
                    new Trigger.Threshold(0.12, 0.01),
                    new Action.ClientFx(
                            "darken",
                            new Scaling.Linear01(0.12, 1.0),
                            35
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

            // Intense radiation can shake the camera
            new ResourceLocation(Hazardous.MODID, "radiation_shake"),
            new EffectEntry(
                    new Trigger.Threshold(0.30, 0.02),
                    new Action.ClientFx(
                            "shake",
                            new Scaling.Linear01(0.30, 1.0),
                            18
                    )
            ),

            // Intense radiation can warp player vision
            new ResourceLocation(Hazardous.MODID, "radiation_warp"),
            new EffectEntry(
                    new Trigger.Threshold(0.45, 0.02),
                    new Action.ClientFx(
                            "warp",
                            new Scaling.Linear01(0.45, 1.0),
                            24
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
            ),

            // Heat haze around lava can blur vision
            new ResourceLocation(Hazardous.MODID, "lava_blur"),
            new EffectEntry(
                    new Trigger.Threshold(0.25, 0.03),
                    new Action.ClientFx(
                            "blur",
                            new Scaling.Linear01(0.25, 1.0),
                            16
                    )
            ),

            // Fire damage near lava
            new ResourceLocation(Hazardous.MODID, "lava_fire_damage"),
            new EffectEntry(
                    new Trigger.Range(0.10, 1.0),
                    new Action.Damage(
                            new ResourceLocation("minecraft", "on_fire"),
                            1.0,
                            new Scaling.Linear01(0.05, 1.0)
                    )
            )
    );
}
