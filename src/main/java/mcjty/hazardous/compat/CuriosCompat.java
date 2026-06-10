package mcjty.hazardous.compat;

import mcjty.hazardous.setup.Registration;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CuriosCompat {
    private static final String HEAD_SLOT = "head";
    private static final String FACE_SLOT = "faceslot";
    private static final List<String> HEAD_OR_FACE_SLOTS = List.of(HEAD_SLOT, FACE_SLOT);

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

    public static Optional<ItemStack> findFirstHeadOrFaceCurio(Player player, Item item) {
        return findFirstHeadOrFaceCurio((LivingEntity) player, stack -> stack.is(item))
                .map(SlotResult::stack);
    }

    public static Optional<SlotResult> findFirstHeadOrFaceCurio(LivingEntity entity, Predicate<ItemStack> predicate) {
        for (String slot : HEAD_OR_FACE_SLOTS) {
            Optional<SlotResult> result = CuriosApi.getCuriosHelper().findCurios(entity, slot).stream()
                    .filter(slotResult -> predicate.test(slotResult.stack()))
                    .findFirst();
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
