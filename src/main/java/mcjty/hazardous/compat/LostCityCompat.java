package mcjty.hazardous.compat;

import mcjty.lib.varia.Logging;
import mcjty.lostcities.api.ILostCities;
import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCityInfo;
import mcjty.lostcities.api.ILostCityInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    public static CityMatch getCityMatch(Level level, BlockPos pos, Optional<String> style, List<String> buildings, List<String> multibuildings) {
        ILostCityInformation info = LostCityInternal.lostCities.getLostInfo(level);
        if (info == null) {
            return null;
        }

        ChunkPos cp = new ChunkPos(pos);
        ILostChunkInfo chunkInfo = info.getChunkInfo(cp.x, cp.z);
        if (!chunkInfo.isCity()) {
            return null;
        }
        if (style.isPresent()) {
            ILostCityInfo cityInfo = chunkInfo.getCityInfo();
            if (cityInfo == null || !style.get().equals(cityInfo.getCityStyle())) {
                return null;
            }
        }

        boolean buildingMatch = buildings.isEmpty();
        if (!buildings.isEmpty()) {
            buildingMatch = chunkInfo.getBuildingId() != null && buildings.contains(chunkInfo.getBuildingId().toString());
            if (!buildingMatch) {
                return null;
            }
        }

        ILostChunkInfo.MultiBuildingInfo multiBuildingInfo = chunkInfo.getMultiBuildingInfo();
        boolean multiBuildingMatch = multibuildings.isEmpty();
        if (!multibuildings.isEmpty()) {
            multiBuildingMatch = multiBuildingInfo != null && multibuildings.contains(multiBuildingInfo.buildingType().toString());
            if (!multiBuildingMatch) {
                return null;
            }
        }

        if (!multibuildings.isEmpty() && multiBuildingInfo != null) {
            int originChunkX = cp.x - multiBuildingInfo.offsetX();
            int originChunkZ = cp.z - multiBuildingInfo.offsetZ();
            int widthChunks = Math.max(1, multiBuildingInfo.w());
            int heightChunks = Math.max(1, multiBuildingInfo.h());
            double widthBlocks = widthChunks * 16.0;
            double heightBlocks = heightChunks * 16.0;
            double centerX = originChunkX * 16.0 + (widthBlocks / 2.0);
            double centerZ = originChunkZ * 16.0 + (heightBlocks / 2.0);
            int effectiveRange = Math.max(1, (int) Math.ceil(Math.hypot(widthBlocks, heightBlocks)));
            return new CityMatch(centerX, centerZ, effectiveRange, true);
        }

        if (!buildings.isEmpty() && buildingMatch) {
            double centerX = cp.getMinBlockX() + 8.0;
            double centerZ = cp.getMinBlockZ() + 8.0;
            int effectiveRange = (int) Math.ceil(Math.hypot(16.0, 16.0));
            return new CityMatch(centerX, centerZ, effectiveRange, true);
        }

        return new CityMatch(cp.getMinBlockX() + 8.0, cp.getMinBlockZ() + 8.0, 0, false);
    }

    public static boolean isCity(Level level, BlockPos pos, Optional<String> style, List<String> buildings, List<String> multibuildings) {
        return getCityMatch(level, pos, style, buildings, multibuildings) != null;
    }

    private static void registerInternal() {
        if (registered) {
            return;
        }
        registered = true;
        Logging.log("Hazardous detected LostCities: enabling support");
        InterModComms.sendTo(ILostCities.LOSTCITIES, ILostCities.GET_LOST_CITIES, LostCityInternal.GetLostCity::new);
    }

    public record CityMatch(double centerX, double centerZ, int effectiveRange, boolean hasExplicitSource) {
    }
}
