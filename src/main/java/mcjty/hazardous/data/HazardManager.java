package mcjty.hazardous.data;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import mcjty.hazardous.compat.LostCityCompat;
import mcjty.hazardous.data.objects.HazardSource;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.hazardous.setup.Config;
import mcjty.lib.varia.Tools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HazardManager {

    private static final double MIN_EFFECTIVE_RADIATION = 1.0e-6;
    private static final Map<Pair<ResourceKey<Level>, ResourceLocation>, Double> lastCachedValue = new HashMap<>();
    private static final Map<HazardType.Blocking.Absorption, AbsorptionModel> ABSORPTION_MODELS = new HashMap<>();

    public static double getHazardValue(HazardType type, Level level, Player player) {
        Registry<HazardSource> sources = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_SOURCE_REGISTRY_KEY);
        Registry<HazardType> types = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);

        ResourceLocation typeId = types.getKey(type);

        var visitor = new PlayerTickVisitor(player);
        double value = 0.0;
        // Always compute the current exposure value here. Any tick-based throttling is handled by callers.
        for (HazardSource source : sources) {
            ResourceLocation sourceId = sources.getKey(source);
            if (sourceId == null || !Config.isHazardSourceEnabled(sourceId)) {
                continue;
            }
            ResourceLocation hazardId = source.hazardType();
            if (hazardId.equals(typeId)) {
                Double v = source.association().accept(type, visitor.withTransmission(source.transmission()));
                if (v != null) {
                    value += v;
                }
            }
        }
        lastCachedValue.put(Pair.of(level.dimension(), typeId), value);
        return value;
    }

    public static double getLastCachedValue(ResourceLocation typeId, Level level) {
        return lastCachedValue.getOrDefault(Pair.of(level.dimension(), typeId), 0.0);
    }

    private static AbsorptionModel getAbsorptionModel(HazardType.Blocking.Absorption absorption) {
        return ABSORPTION_MODELS.computeIfAbsent(absorption, AbsorptionModel::new);
    }

    private static double clampAbsorption(double value) {
        return Mth.clamp(value, 0.0, 1.0);
    }

    private static class PlayerTickVisitor implements HazardSource.Association.Visitor<Double> {
        private final Player player;
        private final double targetX;
        private final double targetZ;
        private final double targetBodyY;
        private final double targetHeadY;
        private HazardSource.Transmission transmission;

        public PlayerTickVisitor(Player player) {
            this.player = player;
            this.targetX = player.getX();
            this.targetZ = player.getZ();
            this.targetBodyY = player.getY() + 0.6;
            this.targetHeadY = player.getEyeY();
        }

        public PlayerTickVisitor withTransmission(HazardSource.Transmission transmission) {
            this.transmission = transmission;
            return this;
        }

        private static double applyFalloff(double base, double d, int maxDistance, HazardType.Falloff falloff) {
            if (maxDistance > 0 && d > maxDistance) {
                return 0.0;
            }
            return falloff.apply(base, d, maxDistance);
        }

        private double computePointRaw(HazardType type, HazardSource.Transmission.Point t, double d) {
            double atten = t.airAttenuationPerBlock() > 0 ? Math.exp(-t.airAttenuationPerBlock() * d) : 1.0;
            double withFalloff = applyFalloff(t.baseIntensity(), d, t.maxDistance(), type.falloff());
            double raw = Math.max(0.0, withFalloff * atten);
            if (raw <= MIN_EFFECTIVE_RADIATION) {
                return 0.0;
            }
            return raw;
        }

        private double applyPointBlocking(HazardType type, Level level, double sourceX, double sourceY, double sourceZ, double rawIntensity) {
            if (rawIntensity <= MIN_EFFECTIVE_RADIATION) {
                return 0.0;
            }
            if (!(type.blocking() instanceof HazardType.Blocking.Absorption absorption)) {
                return rawIntensity;
            }
            AbsorptionModel model = getAbsorptionModel(absorption);
            double cutoffFactor = MIN_EFFECTIVE_RADIATION / rawIntensity;
            double bodyFactor = model.lineFactor(level, sourceX, sourceY, sourceZ, targetX, targetBodyY, targetZ, cutoffFactor);
            if (bodyFactor <= 0.0) {
                double headFactor = model.lineFactor(level, sourceX, sourceY, sourceZ, targetX, targetHeadY, targetZ, cutoffFactor);
                if (headFactor <= 0.0) {
                    return 0.0;
                }
                return rawIntensity * headFactor;
            }
            double headFactor = model.lineFactor(level, sourceX, sourceY, sourceZ, targetX, targetHeadY, targetZ, cutoffFactor);
            return rawIntensity * Math.max(bodyFactor, headFactor);
        }

        private double applySkyBlocking(HazardType type, Level level, double intensity) {
            if (intensity <= MIN_EFFECTIVE_RADIATION) {
                return 0.0;
            }
            if (!(type.blocking() instanceof HazardType.Blocking.Absorption absorption)) {
                return intensity;
            }
            AbsorptionModel model = getAbsorptionModel(absorption);
            int x = Mth.floor(targetX);
            int z = Mth.floor(targetZ);
            int startY = level.getMaxBuildHeight() - 1;
            if (startY <= Mth.floor(targetHeadY)) {
                return intensity;
            }
            double cutoffFactor = MIN_EFFECTIVE_RADIATION / intensity;
            double bodyFactor = model.verticalFactor(level, x, z, startY, Mth.floor(targetBodyY), cutoffFactor);
            if (bodyFactor <= 0.0) {
                double headFactor = model.verticalFactor(level, x, z, startY, Mth.floor(targetHeadY), cutoffFactor);
                if (headFactor <= 0.0) {
                    return 0.0;
                }
                return intensity * headFactor;
            }
            double headFactor = model.verticalFactor(level, x, z, startY, Mth.floor(targetHeadY), cutoffFactor);
            return intensity * Math.max(bodyFactor, headFactor);
        }

        private double computeSkyIntensity(HazardType type, HazardSource.Transmission.Sky t, Level level, BlockPos playerPos) {
            boolean canSeeSky = level.canSeeSky(playerPos);
            double intensity = t.baseIntensity();
            if (level.isNight()) {
                intensity *= t.nightMultiplier();
            }
            if (level.isThundering()) {
                intensity *= t.thunderMultiplier();
            } else if (level.isRaining()) {
                intensity *= t.rainMultiplier();
            }
            if (t.requiresDirectSky() && !canSeeSky) {
                intensity *= t.indoorLeak();
            }
            intensity = Math.max(0.0, intensity);
            return applySkyBlocking(type, level, intensity);
        }

        @Override
        public Double level(HazardType type, HazardSource.Association.Level a) {
            Level level = player.level();
            ResourceLocation currentLevel = level.dimension().location();
            if (!currentLevel.equals(a.level())) {
                return 0.0;
            }
            return transmission.accept(type, new HazardSource.Transmission.Visitor<>() {
                @Override
                public Double sky(HazardType type, HazardSource.Transmission.Sky t) {
                    return computeSkyIntensity(type, t, level, player.blockPosition());
                }
            });
        }

        @Override
        public Double entityType(HazardType type, HazardSource.Association.EntityType a) {
            double maxDistance = a.maxDistance();
            if (maxDistance <= 0) {
                return 0.0;
            }
            var entityType = BuiltInRegistries.ENTITY_TYPE.get(a.entityType());
            if (entityType == null) {
                return 0.0;
            }
            Level level = player.level();
            AABB bounds = player.getBoundingBox().inflate(maxDistance);
            var entities = level.getEntities(entityType, bounds, entity -> entity != player);
            if (entities.isEmpty()) {
                return 0.0;
            }
            return transmission.accept(type, new HazardSource.Transmission.Visitor<>() {
                @Override
                public Double point(HazardType type, HazardSource.Transmission.Point t) {
                    double sum = 0.0;
                    for (Entity entity : entities) {
                        double d = player.distanceTo(entity);
                        if (d > maxDistance) {
                            continue;
                        }
                        double raw = computePointRaw(type, t, d);
                        if (raw <= 0.0) {
                            continue;
                        }
                        double contributed = applyPointBlocking(type, level, entity.getX(), entity.getY(0.5), entity.getZ(), raw);
                        if (contributed > MIN_EFFECTIVE_RADIATION) {
                            sum += contributed;
                        }
                    }
                    return sum;
                }

                @Override
                public Double contact(HazardType type, HazardSource.Transmission.Contact t) {
                    double sum = 0.0;
                    for (Entity entity : entities) {
                        if (player.getBoundingBox().intersects(entity.getBoundingBox())) {
                            sum += t.baseIntensity();
                        }
                    }
                    return Math.max(0.0, sum);
                }
            });
        }

        @Override
        public Double locations(HazardType type, HazardSource.Association.Locations a) {
            Level level = player.level();
            ResourceLocation currentLevel = level.dimension().location();
            if (!currentLevel.equals(a.level())) {
                return 0.0;
            }
            return transmission.accept(type, new HazardSource.Transmission.Visitor<>() {
                @Override
                public Double point(HazardType type, HazardSource.Transmission.Point t) {
                    double sum = 0.0;
                    for (BlockPos p : a.positions()) {
                        double x = p.getX() + 0.5;
                        double y = p.getY() + 0.5;
                        double z = p.getZ() + 0.5;
                        double dx = targetX - x;
                        double dy = targetBodyY - y;
                        double dz = targetZ - z;
                        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        double raw = computePointRaw(type, t, d);
                        if (raw <= 0.0) {
                            continue;
                        }
                        double contributed = applyPointBlocking(type, level, x, y, z, raw);
                        if (contributed > MIN_EFFECTIVE_RADIATION) {
                            sum += contributed;
                        }
                    }
                    return sum;
                }

                @Override
                public Double contact(HazardType type, HazardSource.Transmission.Contact t) {
                    return 0.0;
                }
            });
        }

        @Override
        public Double biome(HazardType type, HazardSource.Association.Biome a) {
            Level level = player.level();
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, a.biome());
            if (!level.getBiome(player.blockPosition()).is(biomeKey)) {
                return 0.0;
            }
            return transmission.accept(type, new HazardSource.Transmission.Visitor<>() {
                @Override
                public Double sky(HazardType type, HazardSource.Transmission.Sky t) {
                    return computeSkyIntensity(type, t, level, player.blockPosition());
                }
            });
        }

        @Override
        public Double city(HazardType type, HazardSource.Association.City a) {
            Level level = player.level();
            if (!LostCityCompat.hasLostCities()) {
                return 0.0;
            }
            BlockPos pos = player.blockPosition();
            if (!LostCityCompat.isCity(level, pos)) {
                return 0.0;
            }
            return transmission.accept(type, new HazardSource.Transmission.Visitor<>() {
                @Override
                public Double sky(HazardType type, HazardSource.Transmission.Sky t) {
                    return computeSkyIntensity(type, t, level, pos);
                }
            });
        }

        @Override
        public Double block(HazardType type, HazardSource.Association.Block a) {
            double maxDistance = a.maxDistance();
            if (maxDistance <= 0) {
                return 0.0;
            }
            Level level = player.level();
            int radius = (int) Math.ceil(maxDistance);
            double maxDistanceSq = maxDistance * maxDistance;
            BlockPos center = player.blockPosition();
            boolean isTag = a.isTag();
            TagKey<Block> tag = isTag ? TagKey.create(Registries.BLOCK, a.blockOrTag()) : null;
            Block block = null;
            if (!isTag) {
                if (!BuiltInRegistries.BLOCK.containsKey(a.blockOrTag())) {
                    return 0.0;
                }
                block = BuiltInRegistries.BLOCK.get(a.blockOrTag());
            }
            Block finalBlock = block;
            TagKey<Block> finalTag = tag;

            return transmission.accept(type, new HazardSource.Transmission.Visitor<>() {
                private boolean matches(BlockState state) {
                    return isTag ? state.is(finalTag) : state.is(finalBlock);
                }

                @Override
                public Double point(HazardType type, HazardSource.Transmission.Point t) {
                    double sum = 0.0;
                    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                double distSq = dx * dx + dy * dy + dz * dz;
                                if (distSq > maxDistanceSq) {
                                    continue;
                                }
                                mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                                BlockState state = level.getBlockState(mutable);
                                if (!matches(state)) {
                                    continue;
                                }
                                double x = mutable.getX() + 0.5;
                                double y = mutable.getY() + 0.5;
                                double z = mutable.getZ() + 0.5;
                                double ddx = targetX - x;
                                double ddy = targetBodyY - y;
                                double ddz = targetZ - z;
                                double d = Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
                                double raw = computePointRaw(type, t, d);
                                if (raw <= 0.0) {
                                    continue;
                                }
                                double contributed = applyPointBlocking(type, level, x, y, z, raw);
                                if (contributed > MIN_EFFECTIVE_RADIATION) {
                                    sum += contributed;
                                }
                            }
                        }
                    }
                    return sum;
                }

                @Override
                public Double contact(HazardType type, HazardSource.Transmission.Contact t) {
                    AABB bounds = player.getBoundingBox();
                    int minX = (int) Math.floor(bounds.minX);
                    int minY = (int) Math.floor(bounds.minY);
                    int minZ = (int) Math.floor(bounds.minZ);
                    int maxX = (int) Math.floor(bounds.maxX);
                    int maxY = (int) Math.floor(bounds.maxY);
                    int maxZ = (int) Math.floor(bounds.maxZ);
                    for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
                        if (matches(level.getBlockState(pos))) {
                            return Math.max(0.0, t.baseIntensity());
                        }
                    }
                    return 0.0;
                }
            });
        }
    }

    private static class AbsorptionModel {
        private final double defaultAbsorption;
        private final Map<Block, Double> blockAbsorptions = new HashMap<>();
        private final List<TagRule> tagRules = new ArrayList<>();
        private final Int2DoubleOpenHashMap blockIdCache = new Int2DoubleOpenHashMap();

        private AbsorptionModel(HazardType.Blocking.Absorption absorption) {
            this.defaultAbsorption = clampAbsorption(absorption.defaultAbsorption());
            for (HazardType.Blocking.Absorption.BlockEntry entry : absorption.blocks()) {
                if (!BuiltInRegistries.BLOCK.containsKey(entry.block())) {
                    continue;
                }
                Block block = BuiltInRegistries.BLOCK.get(entry.block());
                blockAbsorptions.put(block, clampAbsorption(entry.absorption()));
            }
            for (HazardType.Blocking.Absorption.TagEntry entry : absorption.tags()) {
                tagRules.add(new TagRule(TagKey.create(Registries.BLOCK, entry.tag()), clampAbsorption(entry.absorption())));
            }
            blockIdCache.defaultReturnValue(Double.NaN);
        }

        private double getAbsorption(BlockState state) {
            if (state.isAir()) {
                return 0.0;
            }
            Block block = state.getBlock();
            int id = BuiltInRegistries.BLOCK.getId(block);
            double cached = blockIdCache.get(id);
            if (!Double.isNaN(cached)) {
                return cached;
            }

            Double configured = blockAbsorptions.get(block);
            double value = configured == null ? defaultAbsorption : configured;
            if (configured == null && !tagRules.isEmpty()) {
                for (TagRule rule : tagRules) {
                    if (state.is(rule.tag())) {
                        value = Math.max(value, rule.absorption());
                        if (value >= 1.0) {
                            value = 1.0;
                            break;
                        }
                    }
                }
            }
            value = clampAbsorption(value);
            blockIdCache.put(id, value);
            return value;
        }

        private double verticalFactor(Level level, int x, int z, int startY, int endY, double cutoffFactor) {
            if (startY <= endY) {
                return 1.0;
            }
            if (cutoffFactor >= 1.0) {
                return 0.0;
            }
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            double factor = 1.0;
            for (int y = startY; y > endY; y--) {
                mutable.set(x, y, z);
                double absorption = getAbsorption(level.getBlockState(mutable));
                if (absorption <= 0.0) {
                    continue;
                }
                factor *= (1.0 - absorption);
                if (factor <= cutoffFactor) {
                    return 0.0;
                }
            }
            return factor;
        }

        private double lineFactor(Level level, double sx, double sy, double sz, double ex, double ey, double ez, double cutoffFactor) {
            if (cutoffFactor >= 1.0) {
                return 0.0;
            }
            int x = Mth.floor(sx);
            int y = Mth.floor(sy);
            int z = Mth.floor(sz);
            int endX = Mth.floor(ex);
            int endY = Mth.floor(ey);
            int endZ = Mth.floor(ez);

            if (x == endX && y == endY && z == endZ) {
                return 1.0;
            }

            double dx = ex - sx;
            double dy = ey - sy;
            double dz = ez - sz;

            int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
            int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
            int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

            double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dx);
            double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dy);
            double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dz);

            double tMaxX = stepX == 0 ? Double.POSITIVE_INFINITY
                    : (stepX > 0 ? (x + 1.0 - sx) : (sx - x)) * tDeltaX;
            double tMaxY = stepY == 0 ? Double.POSITIVE_INFINITY
                    : (stepY > 0 ? (y + 1.0 - sy) : (sy - y)) * tDeltaY;
            double tMaxZ = stepZ == 0 ? Double.POSITIVE_INFINITY
                    : (stepZ > 0 ? (z + 1.0 - sz) : (sz - z)) * tDeltaZ;

            double factor = 1.0;
            int maxSteps = 1 + Math.abs(endX - x) + Math.abs(endY - y) + Math.abs(endZ - z);
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            boolean processCurrent = false;

            for (int i = 0; i <= maxSteps; i++) {
                if (processCurrent && !(x == endX && y == endY && z == endZ)) {
                    mutable.set(x, y, z);
                    double absorption = getAbsorption(level.getBlockState(mutable));
                    if (absorption > 0.0) {
                        factor *= (1.0 - absorption);
                        if (factor <= cutoffFactor) {
                            return 0.0;
                        }
                    }
                }
                if (x == endX && y == endY && z == endZ) {
                    break;
                }
                processCurrent = true;
                if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                    x += stepX;
                    tMaxX += tDeltaX;
                } else if (tMaxY <= tMaxZ) {
                    y += stepY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }

            return factor;
        }
    }

    private record TagRule(TagKey<Block> tag, double absorption) {
    }
}
