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

    public static boolean isCity(Level level, BlockPos pos, Optional<String> style, List<String> buildings, List<String> multibuildings) {
        ILostCityInformation info = LostCityInternal.lostCities.getLostInfo(level);
        if (info != null) {
            ChunkPos cp =  new ChunkPos(pos);
            ILostChunkInfo chunkInfo = info.getChunkInfo(cp.x, cp.z);
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
        return false;
    }

    private static void registerInternal() {
        if (registered) {
            return;
        }
        registered = true;
        Logging.log("Hazardous detected LostCities: enabling support");
        InterModComms.sendTo(ILostCities.LOSTCITIES, ILostCities.GET_LOST_CITIES, LostCityInternal.GetLostCity::new);
    }

}
