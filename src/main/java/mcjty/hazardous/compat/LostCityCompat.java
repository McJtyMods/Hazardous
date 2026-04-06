package mcjty.hazardous.compat;

import mcjty.lib.varia.Logging;
import mcjty.lostcities.api.ILostCities;
import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCityInfo;
import mcjty.lostcities.api.ILostCityInformation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class LostCityCompat {

    private static boolean hasLostCities = false;

    public static void register() {
        hasLostCities = ModList.get().isLoaded(ILostCities.LOSTCITIES);
        if (hasLostCities()) {
            registerInternal();
        }
    }

    private static boolean registered = false;

    public static boolean hasLostCities() {
        return hasLostCities;
    }

    @Nullable
    public static CityArea getCityArea(Level level, BlockPos pos, Optional<String> style) {
        ChunkData chunkData = getChunkData(level, pos);
        if (chunkData == null || !chunkData.chunkInfo().isCity()) {
            return null;
        }
        if (style.isPresent()) {
            ILostCityInfo cityInfo = chunkData.chunkInfo().getCityInfo();
            if (cityInfo == null || !style.get().equals(cityInfo.getCityStyle())) {
                return null;
            }
        }
        return new CityArea(resolveCityRange(chunkData.chunkInfo(), 16.0, 16.0));
    }

    public static boolean isCity(Level level, BlockPos pos, Optional<String> style, List<String> buildings, List<String> multibuildings) {
        ChunkData chunkData = getChunkData(level, pos);
        return chunkData != null && new CityMatcher(style, buildings, multibuildings).matches(chunkData.chunkInfo());
    }

    public static List<CitySource> findCitySources(Level level, BlockPos pos, Optional<String> style, List<String> buildings, List<String> multibuildings, int searchRadiusBlocks) {
        if (searchRadiusBlocks <= 0) {
            return List.of();
        }
        ILostCityInformation info = LostCityInternal.lostCities.getLostInfo(level);
        if (info == null) {
            return List.of();
        }
        CityMatcher matcher = new CityMatcher(style, buildings, multibuildings);
        if (!matcher.hasExplicitSource()) {
            return List.of();
        }

        ChunkPos origin = new ChunkPos(pos);
        int chunkRadius = Math.max(1, (int) Math.ceil(searchRadiusBlocks / 16.0));
        Map<SourceKey, CitySource> sources = new LinkedHashMap<>();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int chunkX = origin.x + dx;
                int chunkZ = origin.z + dz;
                ILostChunkInfo chunkInfo = info.getChunkInfo(chunkX, chunkZ);
                if (!matcher.matches(chunkInfo)) {
                    continue;
                }
                InternalCitySource source = matcher.createSource(new ChunkPos(chunkX, chunkZ), chunkInfo);
                if (source == null) {
                    continue;
                }
                double centerChunkX = chunkX * 16.0 + 8.0;
                double centerChunkZ = chunkZ * 16.0 + 8.0;
                double ddx = pos.getX() + 0.5 - centerChunkX;
                double ddz = pos.getZ() + 0.5 - centerChunkZ;
                if ((ddx * ddx) + (ddz * ddz) > (searchRadiusBlocks * searchRadiusBlocks)) {
                    continue;
                }
                sources.putIfAbsent(source.key(), source.source());
            }
        }
        return List.copyOf(sources.values());
    }

    @Nullable
    private static ChunkData getChunkData(Level level, BlockPos pos) {
        ILostCityInformation info = LostCityInternal.lostCities.getLostInfo(level);
        if (info == null) {
            return null;
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        return new ChunkData(chunkPos, info.getChunkInfo(chunkPos.x, chunkPos.z));
    }

    private static int resolveCityRange(ILostChunkInfo chunkInfo, double widthBlocks, double heightBlocks) {
        ILostCityInfo cityInfo = chunkInfo.getCityInfo();
        if (cityInfo != null && cityInfo.getCityRadius() > 0) {
            return Math.max(1, (int) Math.ceil(cityInfo.getCityRadius() * 2.0));
        }
        return Math.max(1, (int) Math.ceil(Math.hypot(widthBlocks, heightBlocks)));
    }

    private record ChunkData(ChunkPos chunkPos, ILostChunkInfo chunkInfo) {
    }

    private record SourceKey(ResourceLocation buildingType, int originChunkX, int originChunkZ, int widthChunks, int heightChunks) {
    }

    private record InternalCitySource(SourceKey key, CitySource source) {
    }

    private record CityMatcher(Optional<String> style, List<String> buildings, List<String> multibuildings) {
        private boolean matches(ILostChunkInfo chunkInfo) {
            if (!chunkInfo.isCity()) {
                return false;
            }
            if (style.isPresent()) {
                ILostCityInfo cityInfo = chunkInfo.getCityInfo();
                if (cityInfo == null || !style.get().equals(cityInfo.getCityStyle())) {
                    return false;
                }
            }
            if (!buildings.isEmpty()) {
                if (chunkInfo.getBuildingId() == null || !buildings.contains(chunkInfo.getBuildingId().toString())) {
                    return false;
                }
            }
            if (!multibuildings.isEmpty()) {
                ILostChunkInfo.MultiBuildingInfo multiBuildingInfo = chunkInfo.getMultiBuildingInfo();
                if (multiBuildingInfo == null || !multibuildings.contains(multiBuildingInfo.buildingType().toString())) {
                    return false;
                }
            }
            return true;
        }

        private boolean hasExplicitSource() {
            return !buildings.isEmpty() || !multibuildings.isEmpty();
        }

        @Nullable
        private InternalCitySource createSource(ChunkPos chunkPos, ILostChunkInfo chunkInfo) {
            if (!multibuildings.isEmpty()) {
                ILostChunkInfo.MultiBuildingInfo multiBuildingInfo = chunkInfo.getMultiBuildingInfo();
                if (multiBuildingInfo == null) {
                    return null;
                }
                int originChunkX = chunkPos.x - multiBuildingInfo.offsetX();
                int originChunkZ = chunkPos.z - multiBuildingInfo.offsetZ();
                int widthChunks = Math.max(1, multiBuildingInfo.w());
                int heightChunks = Math.max(1, multiBuildingInfo.h());
                double widthBlocks = widthChunks * 16.0;
                double heightBlocks = heightChunks * 16.0;
                double centerX = originChunkX * 16.0 + (widthBlocks / 2.0);
                double centerZ = originChunkZ * 16.0 + (heightBlocks / 2.0);
                int falloffRange = resolveCityRange(chunkInfo, widthBlocks, heightBlocks);
                return new InternalCitySource(
                        new SourceKey(multiBuildingInfo.buildingType(), originChunkX, originChunkZ, widthChunks, heightChunks),
                        new CitySource(centerX, centerZ, falloffRange));
            }

            if (!buildings.isEmpty()) {
                double centerX = chunkPos.getMinBlockX() + 8.0;
                double centerZ = chunkPos.getMinBlockZ() + 8.0;
                int falloffRange = resolveCityRange(chunkInfo, 16.0, 16.0);
                ResourceLocation buildingType = chunkInfo.getBuildingId();
                if (buildingType == null) {
                    return null;
                }
                return new InternalCitySource(
                        new SourceKey(buildingType, chunkPos.x, chunkPos.z, 1, 1),
                        new CitySource(centerX, centerZ, falloffRange));
            }

            return null;
        }
    }

    private static void registerInternal() {
        if (registered) {
            return;
        }
        registered = true;
        Logging.log("Hazardous detected LostCities: enabling support");
        InterModComms.sendTo(ILostCities.LOSTCITIES, ILostCities.GET_LOST_CITIES, LostCityInternal.GetLostCity::new);
    }

    public record CityArea(int searchRadiusBlocks) {
    }

    public record CitySource(double centerX, double centerZ, int falloffRange) {
    }
}
