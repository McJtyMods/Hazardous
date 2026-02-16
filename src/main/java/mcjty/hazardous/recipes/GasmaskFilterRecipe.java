package mcjty.hazardous.recipes;

import mcjty.hazardous.items.GasmaskItem;
import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class GasmaskFilterRecipe extends CustomRecipe {

    public GasmaskFilterRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack gasmask = ItemStack.EMPTY;
        boolean hasFilter = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Registration.GASMASK.get())) {
                if (!gasmask.isEmpty()) {
                    return false;
                }
                gasmask = stack;
                continue;
            }
            if (stack.is(Registration.FILTER.get())) {
                if (hasFilter) {
                    return false;
                }
                hasFilter = true;
                continue;
            }
            return false;
        }

        return hasFilter && !gasmask.isEmpty() && gasmask.getDamageValue() > 0;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack gasmask = ItemStack.EMPTY;
        boolean hasFilter = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Registration.GASMASK.get())) {
                if (!gasmask.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                gasmask = stack;
                continue;
            }
            if (stack.is(Registration.FILTER.get())) {
                if (hasFilter) {
                    return ItemStack.EMPTY;
                }
                hasFilter = true;
                continue;
            }
            return ItemStack.EMPTY;
        }

        if (gasmask.isEmpty() || !hasFilter) {
            return ItemStack.EMPTY;
        }

        ItemStack result = gasmask.copyWithCount(1);
        int restore = Mth.clamp(Config.GASMASK_FILTER_RESTORE.get(), 1, Integer.MAX_VALUE);
        if (GasmaskItem.restoreDurability(result, restore) <= 0) {
            return ItemStack.EMPTY;
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.GASMASK_FILTER_RECIPE.get();
    }
}
