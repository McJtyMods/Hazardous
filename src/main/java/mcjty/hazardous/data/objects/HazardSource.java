package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Optional;

/**
 * Describes what produces a hazard and where it is attached.
 * Points to a HazardType for the actual behavior numbers.
 */
public record HazardSource(
        ResourceLocation hazardType,
        Association association
) {
    public static final Codec<HazardSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("hazardType").forGetter(HazardSource::hazardType),
                    Association.CODEC.fieldOf("association").forGetter(HazardSource::association)
            ).apply(instance, HazardSource::new));

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
