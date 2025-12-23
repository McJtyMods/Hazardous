package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Set;

/**
 * Defines how a hazard (radiation / solar burn / etc) behaves.
 * Sources (block/item/biome/...) will reference a HazardType id.
 */
public record HazardType(
        // How exposure propagates through the world
        Transmission transmission,

        // How intensity diminishes with distance (if distance applies)
        Falloff falloff,

        // How blocks/armor/etc can reduce exposure
        Blocking blocking,

        // Exposure timing/intervals and input handling (merged)
        Exposure exposure,

        // List of effect entry ids for this hazard type
        List<ResourceLocation> effects
) {

    public static final Codec<HazardType> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Transmission.CODEC.fieldOf("transmission").forGetter(HazardType::transmission),
                    Falloff.CODEC.fieldOf("falloff").forGetter(HazardType::falloff),
                    Blocking.CODEC.fieldOf("blocking").forGetter(HazardType::blocking),
                    Exposure.CODEC.fieldOf("exposure").forGetter(HazardType::exposure),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(HazardType::effects)
            ).apply(instance, HazardType::new));

    public sealed interface Transmission permits Transmission.Sky, Transmission.Point, Transmission.Contact {

        Set<HazardSource.Association.AssociationKind> supportedAssociations();
        <R> R accept(HazardType type, Visitor<R> visitor);

        interface Visitor<R> {
            default R sky(HazardType type, Transmission.Sky a) {
                throw new RuntimeException("sky not supported");
            }
            default R point(HazardType type, Transmission.Point a) {
                throw new RuntimeException("point not supported");
            }
            default R contact(HazardType type, Transmission.Contact a) {
                throw new RuntimeException("contact not supported");
            }
        }

        Codec<Transmission> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.STRING.dispatch("type",
                HazardType::transmissionType,
                HazardType::getTransmissionCodec));

        /**
         * Exposure comes from open sky (e.g. solar burn).
         * Typically computed with "canSeeSky" + time/weather modifiers.
         */
        record Sky(
                double baseIntensity,          // baseline intensity per tick (or per second) before modifiers
                boolean requiresDirectSky,      // canSeeSky (or skylight > 0) required?
                double rainMultiplier,          // e.g. 0.2 when raining
                double thunderMultiplier,       // e.g. 0.1 when thunder
                double nightMultiplier,         // e.g. 0.0 at night for solar
                double indoorLeak              // 0..1: allow some “leak” even without direct sky if you want later
        ) implements Transmission {
            public static final Codec<Sky> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.DOUBLE.fieldOf("baseIntensity").forGetter(Sky::baseIntensity),
                            Codec.BOOL.fieldOf("requiresDirectSky").forGetter(Sky::requiresDirectSky),
                            Codec.DOUBLE.fieldOf("rainMultiplier").forGetter(Sky::rainMultiplier),
                            Codec.DOUBLE.fieldOf("thunderMultiplier").forGetter(Sky::thunderMultiplier),
                            Codec.DOUBLE.fieldOf("nightMultiplier").forGetter(Sky::nightMultiplier),
                            Codec.DOUBLE.fieldOf("indoorLeak").forGetter(Sky::indoorLeak)
                    ).apply(instance, Sky::new));

            @Override
            public Set<HazardSource.Association.AssociationKind> supportedAssociations() {
                return Set.of(
                        HazardSource.Association.AssociationKind.LEVEL,
                        HazardSource.Association.AssociationKind.BIOME,
                        HazardSource.Association.AssociationKind.CITY
                );
            }

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.sky(type, this);
            }
        }

        /**
         * Exposure comes from a point source and may use distance/LOS.
         * Sources like blocks/entities/items can use this.
         */
        record Point(
                double baseIntensity,
                int maxDistance,               // hard cutoff
                boolean requiresLineOfSight,   // if true: ray / voxel occlusion check
                double airAttenuationPerBlock  // extra attenuation even in air (fog-like), usually 0
        ) implements Transmission {
            public static final Codec<Point> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.DOUBLE.fieldOf("baseIntensity").forGetter(Point::baseIntensity),
                            Codec.INT.fieldOf("maxDistance").forGetter(Point::maxDistance),
                            Codec.BOOL.fieldOf("requiresLineOfSight").forGetter(Point::requiresLineOfSight),
                            Codec.DOUBLE.fieldOf("airAttenuationPerBlock").forGetter(Point::airAttenuationPerBlock)
                    ).apply(instance, Point::new));

            @Override
            public Set<HazardSource.Association.AssociationKind> supportedAssociations() {
                return Set.of(
                        HazardSource.Association.AssociationKind.LOCATIONS,
                        HazardSource.Association.AssociationKind.ENTITY_TYPE,
                        HazardSource.Association.AssociationKind.BLOCK
                );
            }

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.point(type, this);
            }
        }

        /**
         * Exposure comes from direct contact (held item, touched block, etc.).
         * Distance/falloff usually irrelevant.
         */
        record Contact(
                double baseIntensity
        ) implements Transmission {
            public static final Codec<Contact> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.DOUBLE.fieldOf("baseIntensity").forGetter(Contact::baseIntensity)
                    ).apply(instance, Contact::new));

            @Override
            public Set<HazardSource.Association.AssociationKind> supportedAssociations() {
                return Set.of(
                        HazardSource.Association.AssociationKind.ENTITY_TYPE,
                        HazardSource.Association.AssociationKind.BLOCK
                );
            }

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.contact(type, this);
            }
        }
    }

    public sealed interface Falloff permits Falloff.None, Falloff.InverseSquare, Falloff.Linear, Falloff.Exponential {
        Codec<Falloff> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.STRING.dispatch("type",
                HazardType::falloffType,
                HazardType::getFalloffCodec));

        /**
         * Apply this falloff to the given base intensity at distance d.
         * Implementations should not enforce maxDistance cut-off; the caller may apply a global cutoff.
         */
        default double apply(double base, double d, int maxDistance) {
            return base;
        }

        record None() implements Falloff {
            public static final None INSTANCE = new None();
            public static final Codec<None> CODEC = Codec.unit(INSTANCE);
        }

        /** intensity *= 1 / (d^2)  (with clamp to avoid infinity) */
        record InverseSquare(double minDistance) implements Falloff {
            public static final Codec<InverseSquare> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.DOUBLE.fieldOf("minDistance").forGetter(InverseSquare::minDistance)
                    ).apply(instance, InverseSquare::new));

            @Override
            public double apply(double base, double d, int maxDistance) {
                double dd = Math.max(minDistance(), Math.max(0.0001, d));
                return base * (1.0 / (dd * dd));
            }
        }

        /** intensity *= max(0, 1 - d/maxDistance) */
        record Linear() implements Falloff {
            public static final Linear INSTANCE = new Linear();
            public static final Codec<Linear> CODEC = Codec.unit(INSTANCE);

            @Override
            public double apply(double base, double d, int maxDistance) {
                if (maxDistance <= 0) return base; // no info; don't reduce
                double f = Math.max(0.0, 1.0 - (d / (double) maxDistance));
                return base * f;
            }
        }

        /** intensity *= exp(-k * d) */
        record Exponential(double k) implements Falloff {
            public static final Codec<Exponential> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.DOUBLE.fieldOf("k").forGetter(Exponential::k)
                    ).apply(instance, Exponential::new));

            @Override
            public double apply(double base, double d, int maxDistance) {
                return base * Math.exp(-k() * d);
            }
        }
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
         * Simple “blocks reduce by X per solid block” model.
         * Good starter that works for LOS and non-LOS (sample ray) later.
         */
        record SimpleOcclusion(
                double solidBlockMultiplier,   // multiply intensity per solid block crossed (e.g. 0.6)
                double fluidMultiplier,        // multiply per fluid block crossed (e.g. water 0.3)
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
                ResourceLocation absorptionRegistryHint, // future registry id for absorbers/materials
                double defaultAbsorption
        ) implements Blocking {
            public static final Codec<Absorption> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.fieldOf("absorptionRegistryHint").forGetter(Absorption::absorptionRegistryHint),
                            Codec.DOUBLE.fieldOf("defaultAbsorption").forGetter(Absorption::defaultAbsorption)
                    ).apply(instance, Absorption::new));
        }
    }

    public record Exposure(
            // How often you apply exposure calculations (ticks).
            // Start with 20 (once per second) to keep it cheap.
            int applyIntervalTicks,

            // If true radiation is accumulated per player (dose buffer)
            boolean accumulate,

            // If true accumulation (up to the maximum) slows down as you approach max
            // (diminishing returns near maximum)
            boolean exponential,

            // The maximum amount of radiation that can be accumulated per player.
            // Use <= 0 to mean "no cap".
            double maximum,

            // How much accumulated radiation decays per tick (applied every calculate() call).
            // Typical: decayPerSecond / 20.0. Use 0 for no decay.
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
                        factor = net.minecraft.util.Mth.clamp(factor, 0.0, 1.0);
                        next = cur + (input * factor);
                    } else {
                        next = cur + input;
                    }
                } else {
                    next = cur + input;
                }
            }

            if (capped) {
                next = net.minecraft.util.Mth.clamp(next, 0.0, max);
            } else {
                next = Math.max(0.0, next);
            }

            return next;
        }
    }

    private static Codec<? extends Transmission> getTransmissionCodec(String type) {
        return switch (type) {
            case "sky" -> Transmission.Sky.CODEC;
            case "point" -> Transmission.Point.CODEC;
            case "contact" -> Transmission.Contact.CODEC;
            default -> throw new IllegalStateException("Unknown transmission type '" + type + "'");
        };
    }

    private static String transmissionType(Transmission transmission) {
        if (transmission instanceof Transmission.Sky) {
            return "sky";
        } else if (transmission instanceof Transmission.Point) {
            return "point";
        } else if (transmission instanceof Transmission.Contact) {
            return "contact";
        }
        throw new IllegalStateException("Unknown transmission: " + transmission);
    }

    private static Codec<? extends Falloff> getFalloffCodec(String type) {
        return switch (type) {
            case "none" -> Falloff.None.CODEC;
            case "inverse_square" -> Falloff.InverseSquare.CODEC;
            case "linear" -> Falloff.Linear.CODEC;
            case "exponential" -> Falloff.Exponential.CODEC;
            default -> throw new IllegalStateException("Unknown falloff type '" + type + "'");
        };
    }

    private static String falloffType(Falloff falloff) {
        if (falloff instanceof Falloff.None) {
            return "none";
        } else if (falloff instanceof Falloff.InverseSquare) {
            return "inverse_square";
        } else if (falloff instanceof Falloff.Linear) {
            return "linear";
        } else if (falloff instanceof Falloff.Exponential) {
            return "exponential";
        }
        throw new IllegalStateException("Unknown falloff: " + falloff);
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
