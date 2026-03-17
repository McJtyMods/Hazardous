package mcjty.hazardous.client;

import mcjty.hazardous.data.HazardManager;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ClientItemEmissionData {

    private static final List<CachedEntry> CACHE = new ArrayList<>();
    private static final List<ItemStack> PENDING = new ArrayList<>();

    @Nullable
    public static List<HazardManager.TooltipEmission> getEmissions(ItemStack stack) {
        for (CachedEntry entry : CACHE) {
            if (ItemStack.matches(entry.stack(), stack)) {
                return entry.emissions();
            }
        }
        return null;
    }

    public static boolean isPending(ItemStack stack) {
        return findPending(stack) != -1;
    }

    public static boolean markPending(ItemStack stack) {
        if (findPending(stack) != -1) {
            return false;
        }
        PENDING.add(stack.copy());
        return true;
    }

    public static void store(ItemStack stack, List<HazardManager.TooltipEmission> emissions) {
        int pendingIndex = findPending(stack);
        if (pendingIndex != -1) {
            PENDING.remove(pendingIndex);
        }

        CachedEntry cachedEntry = new CachedEntry(stack.copy(), List.copyOf(emissions));
        for (int i = 0; i < CACHE.size(); i++) {
            if (ItemStack.matches(CACHE.get(i).stack(), stack)) {
                CACHE.set(i, cachedEntry);
                return;
            }
        }
        CACHE.add(cachedEntry);
    }

    private static int findPending(ItemStack stack) {
        for (int i = 0; i < PENDING.size(); i++) {
            if (ItemStack.matches(PENDING.get(i), stack)) {
                return i;
            }
        }
        return -1;
    }

    private record CachedEntry(ItemStack stack, List<HazardManager.TooltipEmission> emissions) {
    }
}
