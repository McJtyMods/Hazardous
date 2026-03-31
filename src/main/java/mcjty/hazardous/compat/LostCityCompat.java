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

    public static boolean isCity(Level level, BlockPos pos, Optional<String> style) {
        ILostCityInformation info = LostCityInternal.lostCities.getLostInfo(level);
        if (info != null) {
            ChunkPos cp =  new ChunkPos(pos);
            ILostChunkInfo chunkInfo = info.getChunkInfo(cp.x, cp.z);
            if (!chunkInfo.isCity()) {
                return false;
            }
            if (style.isEmpty()) {
                return true;
            }
            ILostCityInfo cityInfo = chunkInfo.getCityInfo();
            return cityInfo != null && style.get().equals(cityInfo.getCityStyle());
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
