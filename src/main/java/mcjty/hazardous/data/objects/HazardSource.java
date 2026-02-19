package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Describes what produces a hazard, how it transmits, and where it is attached.
 * Points to a HazardType for shared behavior numbers (falloff/blocking/exposure/effects).
 */
public record HazardSource(
        ResourceLocation hazardType,
        Transmission transmission,
        Association association
) {
    public static final Codec<HazardSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("hazardType").forGetter(HazardSource::hazardType),
                    Transmission.CODEC.fieldOf("transmission").forGetter(HazardSource::transmission),
                    Association.CODEC.fieldOf("association").forGetter(HazardSource::association)
            ).apply(instance, HazardSource::new));

    public sealed interface Transmission permits Transmission.Sky, Transmission.Point, Transmission.Contact {

        Set<Association.AssociationKind> supportedAssociations();
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
                HazardSource::transmissionType,
                HazardSource::getTransmissionCodec));

        /**
         * Exposure comes from open sky (e.g. solar burn).
         * Typically computed with "canSeeSky" + time/weather modifiers.
         */
        record Sky(
                double baseIntensity,
                boolean requiresDirectSky,
                double rainMultiplier,
                double thunderMultiplier,
                double nightMultiplier,
                double indoorLeak
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
            public Set<Association.AssociationKind> supportedAssociations() {
                return Set.of(
                        Association.AssociationKind.LEVEL,
                        Association.AssociationKind.BIOME,
                        Association.AssociationKind.CITY
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
                int maxDistance,
                boolean requiresLineOfSight,
                double airAttenuationPerBlock
        ) implements Transmission {
            public static final Codec<Point> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.DOUBLE.fieldOf("baseIntensity").forGetter(Point::baseIntensity),
                            Codec.INT.fieldOf("maxDistance").forGetter(Point::maxDistance),
                            Codec.BOOL.fieldOf("requiresLineOfSight").forGetter(Point::requiresLineOfSight),
                            Codec.DOUBLE.fieldOf("airAttenuationPerBlock").forGetter(Point::airAttenuationPerBlock)
                    ).apply(instance, Point::new));

            @Override
            public Set<Association.AssociationKind> supportedAssociations() {
                return Set.of(
                        Association.AssociationKind.LOCATIONS,
                        Association.AssociationKind.ENTITY_TYPE,
                        Association.AssociationKind.BLOCK
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
            public Set<Association.AssociationKind> supportedAssociations() {
                return Set.of(
                        Association.AssociationKind.ENTITY_TYPE,
                        Association.AssociationKind.BLOCK
                );
            }

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.contact(type, this);
            }
        }
    }

    public sealed interface Association permits Association.Level, Association.EntityType, Association.Locations, Association.Biome, Association.City, Association.Block {

        enum AssociationKind {
            LEVEL, ENTITY_TYPE, LOCATIONS, BIOME, CITY, BLOCK
        }

        <R> R accept(HazardType type, Visitor<R> visitor);

        AssociationKind kind();

        interface Visitor<R> {
            R level(HazardType type, Association.Level a);

            R entityType(HazardType type, Association.EntityType a);

            R locations(HazardType type, Association.Locations a);

            R biome(HazardType type, Association.Biome a);

            R city(HazardType type, Association.City a);

            R block(HazardType type, Association.Block a);
        }

        Codec<Association> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.STRING.dispatch("type",
                HazardSource::associationType,
                HazardSource::getAssociationCodec));

        /**
         * Hazard applies to an entire level/dimension.
         */
        record Level(ResourceLocation level) implements Association {
            public static final Codec<Level> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.fieldOf("level").forGetter(Level::level)
                    ).apply(instance, Level::new));

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.level(type, this);
            }

            @Override
            public AssociationKind kind() {
                return AssociationKind.LEVEL;
            }
        }

        /**
         * Hazard is attached to all entities of the given type.
         */
        record EntityType(ResourceLocation entityType, double maxDistance) implements Association {
            public static final Codec<EntityType> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.fieldOf("entityType").forGetter(EntityType::entityType),
                            Codec.DOUBLE.fieldOf("maxDistance").forGetter(EntityType::maxDistance)
                    ).apply(instance, EntityType::new));

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.entityType(type, this);
            }

            @Override
            public AssociationKind kind() {
                return AssociationKind.ENTITY_TYPE;
            }
        }

        /**
         * Hazard is attached to explicit locations inside a level.
         */
        record Locations(ResourceLocation level, List<BlockPos> positions) implements Association {
            public static final Codec<Locations> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.fieldOf("level").forGetter(Locations::level),
                            BlockPos.CODEC.listOf().fieldOf("positions").forGetter(Locations::positions)
                    ).apply(instance, Locations::new));

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.locations(type, this);
            }

            @Override
            public AssociationKind kind() {
                return AssociationKind.LOCATIONS;
            }
        }

        /**
         * Hazard is attached to everything inside a biome.
         */
        record Biome(ResourceLocation biome) implements Association {
            public static final Codec<Biome> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.fieldOf("biome").forGetter(Biome::biome)
                    ).apply(instance, Biome::new));

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.biome(type, this);
            }

            @Override
            public AssociationKind kind() {
                return AssociationKind.BIOME;
            }
        }

        /**
         * Hazard applies when inside a Lost Cities city (if Lost Cities is present).
         */
        record City() implements Association {
            public static final City INSTANCE = new City();
            public static final Codec<City> CODEC = Codec.unit(INSTANCE);

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.city(type, this);
            }

            @Override
            public AssociationKind kind() {
                return AssociationKind.CITY;
            }
        }

        /**
         * Hazard is attached to specific blocks or block tags.
         */
        record Block(ResourceLocation blockOrTag, boolean isTag, double maxDistance) implements Association {
            public static final Codec<Block> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.optionalFieldOf("block").forGetter(a -> a.isTag() ? Optional.empty() : Optional.of(a.blockOrTag())),
                            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(a -> a.isTag() ? Optional.of(a.blockOrTag()) : Optional.empty()),
                            Codec.DOUBLE.fieldOf("maxDistance").forGetter(Block::maxDistance)
                    ).apply(instance, (block, tag, maxDistance) -> {
                        if (block.isPresent() == tag.isPresent()) {
                            throw new IllegalStateException("Block association must have either 'block' or 'tag'");
                        }
                        return new Block(block.orElseGet(tag::get), tag.isPresent(), maxDistance);
                    }));

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.block(type, this);
            }

            @Override
            public AssociationKind kind() {
                return AssociationKind.BLOCK;
            }
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

    private static Codec<? extends Association> getAssociationCodec(String type) {
        return switch (type) {
            case "level" -> Association.Level.CODEC;
            case "entity_type" -> Association.EntityType.CODEC;
            case "locations" -> Association.Locations.CODEC;
            case "biome" -> Association.Biome.CODEC;
            case "city" -> Association.City.CODEC;
            case "block" -> Association.Block.CODEC;
            default -> throw new IllegalStateException("Unknown association type '" + type + "'");
        };
    }

    private static String associationType(Association association) {
        if (association instanceof Association.Level) {
            return "level";
        } else if (association instanceof Association.EntityType) {
            return "entity_type";
        } else if (association instanceof Association.Locations) {
            return "locations";
        } else if (association instanceof Association.Biome) {
            return "biome";
        } else if (association instanceof Association.City) {
            return "city";
        } else if (association instanceof Association.Block) {
            return "block";
        }
        throw new IllegalStateException("Unknown association: " + association);
    }
}
