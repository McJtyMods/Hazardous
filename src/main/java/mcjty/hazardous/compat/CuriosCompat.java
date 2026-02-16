package mcjty.hazardous.compat;

import mcjty.hazardous.setup.Registration;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

public class CuriosCompat {

    public static boolean hasActiveGeigerCounter(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, Registration.GEIGER_COUNTER.get()).isPresent();
    }
}
