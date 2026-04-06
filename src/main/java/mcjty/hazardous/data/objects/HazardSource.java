package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.hazardous.util.BiomeMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Describes what produces a hazard, how it transmits, and where it is attached.
 * Points to a HazardType for shared blocking/exposure/effects while keeping source-specific falloff.
 */
public record HazardSource(
        ResourceLocation hazardType,
        Falloff falloff,
        Transmission transmission,
        Association association
) {
    public static final Codec<HazardSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("hazardType").forGetter(HazardSource::hazardType),
                    Falloff.CODEC.fieldOf("falloff").forGetter(HazardSource::falloff),
                    Transmission.CODEC.fieldOf("transmission").forGetter(HazardSource::transmission),
                    Association.CODEC.fieldOf("association").forGetter(HazardSource::association)
            ).apply(instance, HazardSource::new));

    public sealed interface Falloff permits Falloff.None, Falloff.InverseSquare, Falloff.Linear, Falloff.Exponential {
        Codec<Falloff> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.STRING.dispatch("type",
                HazardSource::falloffType,
                HazardSource::getFalloffCodec));

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

        /** intensity *= 1 / (d^2) (with clamp to avoid infinity) */
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
                if (maxDistance <= 0) {
                    return base;
                }
                double factor = Math.max(0.0, 1.0 - (d / (double) maxDistance));
                return base * factor;
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
         * Exposure comes from a point source and may use distance/LOS plus this source's falloff.
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
                        Association.AssociationKind.BLOCK,
                        Association.AssociationKind.ITEM
                );
            }

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.point(type, this);
            }
        }

        /**
         * Exposure comes from direct contact (held item, touched block, etc.).
         * Distance-based falloff is usually irrelevant here.
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
                        Association.AssociationKind.BLOCK,
                        Association.AssociationKind.ITEM
                );
            }

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.contact(type, this);
            }
        }
    }

    public sealed interface Association permits Association.Level, Association.EntityType, Association.Locations, Association.Biome, Association.City, Association.Block, Association.Item {

        enum AssociationKind {
            LEVEL, ENTITY_TYPE, LOCATIONS, BIOME, CITY, BLOCK, ITEM
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
            R item(HazardType type, Association.Item a);
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
         * Hazard is attached to all entities of the given types.
         * For item entities, optional stack predicates can further filter the dropped item.
         * Point search distance comes from transmission.maxDistance.
         */
        record EntityType(List<ResourceLocation> entityTypes, List<Item.ItemStackPredicate> stacks) implements Association {
            public static final Codec<EntityType> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.listOf().fieldOf("entityTypes").forGetter(EntityType::entityTypes),
                            Item.ItemStackPredicate.CODEC.listOf().optionalFieldOf("stacks", List.of()).forGetter(EntityType::stacks),
                            Codec.DOUBLE.optionalFieldOf("maxDistance").forGetter(a -> Optional.empty())
                    ).apply(instance, (entityTypes, stacks, ignoredMaxDistance) -> new EntityType(entityTypes, stacks)));

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
        record Biome(BiomeMatcher biome) implements Association {
            public static final Codec<Biome> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            BiomeMatcher.CODEC.fieldOf("biome").forGetter(Biome::biome)
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
        record City(Optional<String> style, List<String> buildings, List<String> multibuildings) implements Association {
            public static final City INSTANCE = new City(Optional.empty(), List.of(), List.of());
            public static final Codec<City> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.STRING.optionalFieldOf("style").forGetter(City::style),
                            Codec.STRING.listOf().optionalFieldOf("buildings", List.of()).forGetter(City::buildings),
                            Codec.STRING.listOf().optionalFieldOf("multibuildings", List.of()).forGetter(City::multibuildings)
                    ).apply(instance, City::new));

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
         * Point search distance comes from transmission.maxDistance.
         */
        record Block(ResourceLocation blockOrTag, boolean isTag) implements Association {
            public static final Codec<Block> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ResourceLocation.CODEC.optionalFieldOf("block").forGetter(a -> a.isTag() ? Optional.empty() : Optional.of(a.blockOrTag())),
                            ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(a -> a.isTag() ? Optional.of(a.blockOrTag()) : Optional.empty()),
                            Codec.DOUBLE.optionalFieldOf("maxDistance").forGetter(a -> Optional.empty())
                    ).apply(instance, (block, tag, ignoredMaxDistance) -> {
                        if (block.isPresent() == tag.isPresent()) {
                            throw new IllegalStateException("Block association must have either 'block' or 'tag'");
                        }
                        return new Block(block.orElseGet(tag::get), tag.isPresent());
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

        /**
         * Hazard is attached to players carrying matching item stacks.
         * Point search distance comes from transmission.maxDistance.
         */
        record Item(List<ItemStackPredicate> stacks) implements Association {
            public static final Codec<Item> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            ItemStackPredicate.CODEC.listOf().fieldOf("stacks").forGetter(Item::stacks),
                            Codec.DOUBLE.optionalFieldOf("maxDistance").forGetter(a -> Optional.empty())
                    ).apply(instance, (stacks, ignoredMaxDistance) -> new Item(stacks)));

            @Override
            public <R> R accept(HazardType type, Visitor<R> visitor) {
                return visitor.item(type, this);
            }

            @Override
            public AssociationKind kind() {
                return AssociationKind.ITEM;
            }

            /**
             * Matches a carried stack by item id or item tag, with optional minimum count and NBT.
             */
            public record ItemStackPredicate(ResourceLocation itemOrTag, boolean isTag, int count, Optional<CompoundTag> nbt) {
                private static final Codec<CompoundTag> SNBT_CODEC = Codec.STRING.comapFlatMap(
                        s -> {
                            try {
                                return DataResult.success(TagParser.parseTag(s));
                            } catch (Exception e) {
                                return DataResult.error(() -> "Invalid SNBT in item association: " + e.getMessage());
                            }
                        },
                        CompoundTag::toString
                );

                public static final Codec<ItemStackPredicate> CODEC = RecordCodecBuilder.create(instance ->
                        instance.group(
                                ResourceLocation.CODEC.optionalFieldOf("item").forGetter(a -> a.isTag() ? Optional.empty() : Optional.of(a.itemOrTag())),
                                ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(a -> a.isTag() ? Optional.of(a.itemOrTag()) : Optional.empty()),
                                Codec.INT.optionalFieldOf("count", 1).forGetter(ItemStackPredicate::count),
                                SNBT_CODEC.optionalFieldOf("nbt").forGetter(ItemStackPredicate::nbt)
                        ).apply(instance, (item, tag, count, nbt) -> {
                            if (item.isPresent() == tag.isPresent()) {
                                throw new IllegalStateException("Item association predicate must have either 'item' or 'tag'");
                            }
                            if (count <= 0) {
                                throw new IllegalStateException("Item association predicate count must be > 0");
                            }
                            return new ItemStackPredicate(item.orElseGet(tag::get), tag.isPresent(), count, nbt);
                        }));

                public boolean matches(ItemStack stack) {
                    if (stack.isEmpty() || stack.getCount() < count) {
                        return false;
                    }
                    if (isTag) {
                        if (!stack.is(TagKey.create(Registries.ITEM, itemOrTag))) {
                            return false;
                        }
                    } else if (!itemOrTag.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                        return false;
                    }
                    return nbt.isEmpty() || stack.getTag() != null && net.minecraft.nbt.NbtUtils.compareNbt(nbt.get(), stack.getTag(), true);
                }
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

    private static Codec<? extends Association> getAssociationCodec(String type) {
        return switch (type) {
            case "level" -> Association.Level.CODEC;
            case "entity_type" -> Association.EntityType.CODEC;
            case "locations" -> Association.Locations.CODEC;
            case "biome" -> Association.Biome.CODEC;
            case "city" -> Association.City.CODEC;
            case "block" -> Association.Block.CODEC;
            case "item" -> Association.Item.CODEC;
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
        } else if (association instanceof Association.Item) {
            return "item";
        }
        throw new IllegalStateException("Unknown association: " + association);
    }
}
