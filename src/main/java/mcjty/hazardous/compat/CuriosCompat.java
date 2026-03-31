package mcjty.hazardous.compat;

import mcjty.hazardous.setup.Registration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;

public class CuriosCompat {
    private static final String HEAD_SLOT = "head";

    public static boolean hasActiveGeigerCounter(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, Registration.GEIGER_COUNTER.get()).isPresent();
    }

    public static boolean hasActiveDosimeter(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, Registration.DOSIMETER.get()).isPresent();
    }

    public static Optional<ItemStack> findFirstHeadCurio(Player player, Item item) {
        return CuriosApi.getCuriosHelper().findCurios(player, HEAD_SLOT).stream()
                .map(slotResult -> slotResult.stack())
                .filter(stack -> stack.is(item))
                .findFirst();
    }
}
