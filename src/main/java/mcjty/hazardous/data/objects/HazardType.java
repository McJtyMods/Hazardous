package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Defines how a hazard (radiation / solar burn / etc) behaves.
 * Sources (block/item/biome/...) reference a HazardType id and provide source-specific falloff plus transmission details.
 */
public record HazardType(
        // How blocks/armor/etc can reduce exposure
        Blocking blocking,

        // Exposure timing/intervals and input handling (merged)
        Exposure exposure,

        // List of effect entry ids for this hazard type
        List<ResourceLocation> effects,

        // Optional player attribute used as a 0..1 resistance multiplier for this hazard type
        ResourceLocation resistanceAttribute
) {

    public static final Codec<HazardType> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Blocking.CODEC.fieldOf("blocking").forGetter(HazardType::blocking),
                    Exposure.CODEC.fieldOf("exposure").forGetter(HazardType::exposure),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(HazardType::effects),
                    ResourceLocation.CODEC.optionalFieldOf("resistanceAttribute")
                            .forGetter(type -> java.util.Optional.ofNullable(type.resistanceAttribute()))
            ).apply(instance, (blocking, exposure, effects, resistanceAttribute) ->
                    new HazardType(blocking, exposure, effects, resistanceAttribute.orElse(null))));

    public HazardType(Blocking blocking, Exposure exposure, List<ResourceLocation> effects) {
        this(blocking, exposure, effects, null);
    }

    public sealed interface Blocking permits Blocking.None, Blocking.SimpleOcclusion, Blocking.Absorption {
        Codec<Blocking> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.STRING.dispatch("type",
                HazardType::blockingType,
                HazardType::getBlockingCodec));

        record None() implements Blocking {
            public static final None INSTANCE = new None();
            public static final Codec<None> CODEC = Codec.unit(INSTANCE);
        }

        /**
         * Simple "blocks reduce by X per solid block" model.
         * Good starter that works for LOS and non-LOS (sample ray) later.
         */
        record SimpleOcclusion(
                double solidBlockMultiplier,
                double fluidMultiplier,
                boolean treatLeavesAsSolid
        ) implements Blocking {
            public static final Codec<SimpleOcclusion> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.DOUBLE.fieldOf("solidBlockMultiplier").forGetter(SimpleOcclusion::solidBlockMultiplier),
                            Codec.DOUBLE.fieldOf("fluidMultiplier").forGetter(SimpleOcclusion::fluidMultiplier),
                            Codec.BOOL.fieldOf("treatLeavesAsSolid").forGetter(SimpleOcclusion::treatLeavesAsSolid)
                    ).apply(instance, SimpleOcclusion::new));
        }

        /**
         * More advanced: materials/tags provide absorption coefficients.
         * Keep it here for later; you can start with SimpleOcclusion.
         */
        record Absorption(
                ResourceLocation absorptionRegistryHint,
                double defaultAbsorption,
                List<BlockEntry> blocks,
                List<TagEntry> tags
        ) implements Blocking {
            public static final Codec<Absorption> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.fieldOf("absorptionRegistryHint").forGetter(Absorption::absorptionRegistryHint),
                            Codec.DOUBLE.fieldOf("defaultAbsorption").forGetter(Absorption::defaultAbsorption),
                            BlockEntry.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(Absorption::blocks),
                            TagEntry.CODEC.listOf().optionalFieldOf("tags", List.of()).forGetter(Absorption::tags)
                    ).apply(instance, Absorption::new));

            public record BlockEntry(ResourceLocation block, double absorption) {
                public static final Codec<BlockEntry> CODEC = RecordCodecBuilder.create(instance ->
                        instance.group(
                                ResourceLocation.CODEC.fieldOf("block").forGetter(BlockEntry::block),
                                Codec.DOUBLE.fieldOf("absorption").forGetter(BlockEntry::absorption)
                        ).apply(instance, BlockEntry::new));
            }

            public record TagEntry(ResourceLocation tag, double absorption) {
                public static final Codec<TagEntry> CODEC = RecordCodecBuilder.create(instance ->
                        instance.group(
                                ResourceLocation.CODEC.fieldOf("tag").forGetter(TagEntry::tag),
                                Codec.DOUBLE.fieldOf("absorption").forGetter(TagEntry::absorption)
                        ).apply(instance, TagEntry::new));
            }
        }
    }

    public record Exposure(
            int applyIntervalTicks,
            boolean accumulate,
            boolean exponential,
            double maximum,
            double decayPerTick
    ) {
        public static final Codec<Exposure> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("applyIntervalTicks").forGetter(Exposure::applyIntervalTicks),
                        Codec.BOOL.fieldOf("accumulate").forGetter(Exposure::accumulate),
                        Codec.BOOL.fieldOf("exponential").forGetter(Exposure::exponential),
                        Codec.DOUBLE.fieldOf("maximum").forGetter(Exposure::maximum),
                        Codec.DOUBLE.fieldOf("decayPerTick").forGetter(Exposure::decayPerTick)
                ).apply(instance, Exposure::new));

        public double calculate(double input, double current) {
            final boolean capped = maximum > 0;
            final double max = capped ? maximum : Double.POSITIVE_INFINITY;

            double cur = accumulate ? current : 0.0;
            if (accumulate && decayPerTick > 0) {
                cur = Math.max(0.0, cur - decayPerTick);
            }

            double next;
            if (!accumulate) {
                next = input;
            } else {
                if (input >= 0) {
                    if (exponential && capped) {
                        double factor = 1.0 - (cur / max);
                        factor = Mth.clamp(factor, 0.0, 1.0);
                        next = cur + (input * factor);
                    } else {
                        next = cur + input;
                    }
                } else {
                    next = cur + input;
                }
            }

            if (capped) {
                next = Mth.clamp(next, 0.0, max);
            } else {
                next = Math.max(0.0, next);
            }

            return next;
        }
    }

    private static Codec<? extends Blocking> getBlockingCodec(String type) {
        return switch (type) {
            case "none" -> Blocking.None.CODEC;
            case "simple" -> Blocking.SimpleOcclusion.CODEC;
            case "absorption" -> Blocking.Absorption.CODEC;
            default -> throw new IllegalStateException("Unknown blocking type '" + type + "'");
        };
    }

    private static String blockingType(Blocking blocking) {
        if (blocking instanceof Blocking.None) {
            return "none";
        } else if (blocking instanceof Blocking.SimpleOcclusion) {
            return "simple";
        } else if (blocking instanceof Blocking.Absorption) {
            return "absorption";
        }
        throw new IllegalStateException("Unknown blocking: " + blocking);
    }
}
