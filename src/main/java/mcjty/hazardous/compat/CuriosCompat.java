package mcjty.hazardous.compat;

import mcjty.hazardous.setup.Registration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;
import java.util.function.Predicate;

public class CuriosCompat {
    private static final String HEAD_SLOT = "head";

    public static boolean hasActiveGeigerCounter(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, Registration.GEIGER_COUNTER.get()).isPresent();
    }

    public static boolean hasActiveDosimeter(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, Registration.DOSIMETER.get()).isPresent();
    }

    public static Optional<ItemStack> findFirstHeadCurio(Player player, Item item) {
        return findFirstHeadCurio((LivingEntity) player, stack -> stack.is(item))
                .map(SlotResult::stack);
    }

    public static Optional<SlotResult> findFirstHeadCurio(LivingEntity entity, Predicate<ItemStack> predicate) {
        return CuriosApi.getCuriosHelper().findCurios(entity, HEAD_SLOT).stream()
                .filter(slotResult -> predicate.test(slotResult.stack()))
                .findFirst();
    }

    public static Optional<ItemStack> findFirstHeadCurio(Player player, Predicate<ItemStack> predicate) {
        return findFirstHeadCurio((LivingEntity) player, predicate)
                .map(SlotResult::stack);
    }
}
