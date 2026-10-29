package mcjty.hazardous.client;

import mcjty.hazardous.data.HazardManager;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ClientItemEmissionData {

    // Tooltip generation can overlap with emission replies. Guard both lists with the class lock.
    private static final List<CachedEntry> CACHE = new ArrayList<>();
    private static final List<ItemStack> PENDING = new ArrayList<>();

    @Nullable
    public static synchronized List<HazardManager.TooltipEmission> getEmissions(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        for (CachedEntry entry : CACHE) {
            if (ItemStack.matches(entry.stack(), stack)) {
                return entry.emissions();
            }
        }
        return null;
    }

    public static synchronized boolean isPending(@Nullable ItemStack stack) {
        return findPending(stack) != -1;
    }

    public static synchronized boolean markPending(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty() || findPending(stack) != -1) {
            return false;
        }
        PENDING.add(stack.copy());
        return true;
    }

    public static synchronized void store(@Nullable ItemStack stack, List<HazardManager.TooltipEmission> emissions) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
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

    private static int findPending(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < PENDING.size(); i++) {
            ItemStack pendingStack = PENDING.get(i);
            if (pendingStack != null && ItemStack.matches(pendingStack, stack)) {
                return i;
            }
        }
        return -1;
    }

    private record CachedEntry(ItemStack stack, List<HazardManager.TooltipEmission> emissions) {
    }
}
