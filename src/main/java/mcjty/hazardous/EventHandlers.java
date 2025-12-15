package mcjty.hazardous;

import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.data.HazardManager;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.lib.varia.Tools;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;

public class EventHandlers {

    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        Level level = event.player.level();
        Registry<HazardType> types = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);

        for (HazardType type : types) {
            double value = HazardManager.getHazardValue(type, level, event.player);
        }
    }

}
