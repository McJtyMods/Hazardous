package mcjty.hazardous.data;

import mcjty.hazardous.compat.LostCityCompat;
import mcjty.hazardous.data.objects.HazardSource;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.lib.varia.LevelTools;
import mcjty.lib.varia.Tools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class HazardManager {

    private final static Map<Pair<ResourceKey<Level>, ResourceLocation>, Double> lastCachedValue = new HashMap<>();

    public static double getHazardValue(HazardType type, Level level, Player player) {
        Registry<HazardSource> sources = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_SOURCE_REGISTRY_KEY);
        Registry<HazardType> types = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);

        ResourceLocation typeId = types.getKey(type);

        var visitor = new PlayerTickVisitor(player);
        double value = 0.0;
        // Always compute the current exposure value here. Any tick-based throttling is handled by callers.
        // Optimize by using a map of type -> source?
        for (HazardSource source : sources) {
            ResourceLocation hazardId = source.hazardType();
            if (hazardId.equals(typeId)) {
                Double v = source.association().accept(type, visitor);
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

    private static class PlayerTickVisitor implements HazardSource.Association.Visitor<Double> {
        private final Player player;

        public PlayerTickVisitor(Player player) {
            this.player = player;
        }

        private static double applyFalloff(double base, double d, int maxDistance, HazardType.Falloff falloff) {
            if (maxDistance > 0 && d > maxDistance) {
                return 0.0;
            }
            return falloff.apply(base, d, maxDistance);
        }

        private static double distanceTo(Player player, double x, double y, double z) {
            double dx = player.getX() - x;
            double dy = player.getY() - y;
            double dz = player.getZ() - z;
            return Math.sqrt(dx*dx + dy*dy + dz*dz);
        }

        @Override
        public Double level(HazardType type, HazardSource.Association.Level a) {
            Level level = player.level();
            ResourceLocation currentLevel = level.dimension().location();
            if (!currentLevel.equals(a.level())) {
                return 0.0;
            }
            return type.transmission().accept(type, new HazardType.Transmission.Visitor<>() {
                @Override
                public Double sky(HazardType type, HazardType.Transmission.Sky t) {
                    BlockPos pos = player.blockPosition();
                    boolean canSeeSky = level.canSeeSky(pos);
                    double intensity = t.baseIntensity();
                    boolean isNight = level.isNight();
                    if (isNight) {
                        intensity *= t.nightMultiplier();
                    }
                    if (level.isThundering()) {
                        intensity *= t.thunderMultiplier();
                    } else if (level.isRaining()) {
                        intensity *= t.rainMultiplier();
                    }
                    if (t.requiresDirectSky()) {
                        if (!canSeeSky) {
                            intensity *= t.indoorLeak();
                        }
                    }
                    return Math.max(0.0, intensity);
                }

                // Other transmission types are not supported for level association
            });
        }

        @Override
        public Double entityType(HazardType type, HazardSource.Association.EntityType a) {
            // Compare against the player's own entity type for a minimal implementation
            ResourceLocation playerTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(player.getType());
            if (playerTypeId == null || !playerTypeId.equals(a.entityType())) {
                return 0.0;
            }
            return type.transmission().accept(type, new HazardType.Transmission.Visitor<>() {
                @Override
                public Double point(HazardType type, HazardType.Transmission.Point t) {
                    // Treat source as at the player's position (distance 0)
                    double base = t.baseIntensity();
                    // Air attenuation per block (distance in blocks)
                    double d = 0.0;
                    double atten = t.airAttenuationPerBlock() > 0 ? Math.exp(-t.airAttenuationPerBlock() * d) : 1.0;
                    double withFalloff = applyFalloff(base, d, t.maxDistance(), type.falloff());
                    return Math.max(0.0, withFalloff * atten);
                }

                @Override
                public Double contact(HazardType type, HazardType.Transmission.Contact t) {
                    return Math.max(0.0, t.baseIntensity());
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
            return type.transmission().accept(type, new HazardType.Transmission.Visitor<>() {
                @Override
                public Double point(HazardType type, HazardType.Transmission.Point t) {
                    double sum = 0.0;
                    for (BlockPos p : a.positions()) {
                        double x = p.getX() + 0.5;
                        double y = p.getY() + 0.5;
                        double z = p.getZ() + 0.5;
                        double d = distanceTo(player, x, y, z);
                        double base = t.baseIntensity();
                        double atten = t.airAttenuationPerBlock() > 0 ? Math.exp(-t.airAttenuationPerBlock() * d) : 1.0;
                        double withFalloff = applyFalloff(base, d, t.maxDistance(), type.falloff());
                        sum += Math.max(0.0, withFalloff * atten);
                    }
                    return sum;
                }

                @Override
                public Double contact(HazardType type, HazardType.Transmission.Contact t) {
                    // Contact is not supported for locations in current model
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
            return type.transmission().accept(type, new HazardType.Transmission.Visitor<>() {
                @Override
                public Double sky(HazardType type, HazardType.Transmission.Sky t) {
                    BlockPos pos = player.blockPosition();
                    boolean canSeeSky = level.canSeeSky(pos);
                    double intensity = t.baseIntensity();
                    boolean isNight = level.isNight();
                    if (isNight) {
                        intensity *= t.nightMultiplier();
                    }
                    if (level.isThundering()) {
                        intensity *= t.thunderMultiplier();
                    } else if (level.isRaining()) {
                        intensity *= t.rainMultiplier();
                    }
                    if (t.requiresDirectSky()) {
                        if (!canSeeSky) {
                            intensity *= t.indoorLeak();
                        }
                    }
                    return Math.max(0.0, intensity);
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
            return type.transmission().accept(type, new HazardType.Transmission.Visitor<>() {
                @Override
                public Double sky(HazardType type, HazardType.Transmission.Sky t) {
                    boolean canSeeSky = level.canSeeSky(pos);
                    double intensity = t.baseIntensity();
                    boolean isNight = level.isNight();
                    if (isNight) {
                        intensity *= t.nightMultiplier();
                    }
                    if (level.isThundering()) {
                        intensity *= t.thunderMultiplier();
                    } else if (level.isRaining()) {
                        intensity *= t.rainMultiplier();
                    }
                    if (t.requiresDirectSky()) {
                        if (!canSeeSky) {
                            intensity *= t.indoorLeak();
                        }
                    }
                    return Math.max(0.0, intensity);
                }
            });
        }
    }

}
