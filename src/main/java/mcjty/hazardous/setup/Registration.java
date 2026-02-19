package mcjty.hazardous.setup;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.items.FilterItem;
import mcjty.hazardous.items.GasmaskItem;
import mcjty.hazardous.items.GeigerCounterItem;
import mcjty.hazardous.items.PillsItem;
import mcjty.hazardous.recipes.GasmaskFilterRecipe;
import mcjty.lib.setup.DeferredItem;
import mcjty.lib.setup.DeferredItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.function.Supplier;

public class Registration {

    public static final DeferredItems ITEMS = DeferredItems.create(Hazardous.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Hazardous.MODID);

    public static final DeferredItem<GeigerCounterItem> GEIGER_COUNTER = ITEMS.register("geiger_counter", tab(GeigerCounterItem::new));
    public static final DeferredItem<GasmaskItem> GASMASK = ITEMS.register("gasmask", tab(GasmaskItem::new));
    public static final DeferredItem<FilterItem> FILTER = ITEMS.register("filter", tab(FilterItem::new));
    public static final DeferredItem<PillsItem> PILLS = ITEMS.register("pills", tab(PillsItem::new));

    public static final RegistryObject<SimpleCraftingRecipeSerializer<GasmaskFilterRecipe>> GASMASK_FILTER_RECIPE = RECIPE_SERIALIZERS.register("gasmask_filter_refill",
            () -> new SimpleCraftingRecipeSerializer<>(GasmaskFilterRecipe::new));

    public static void register(IEventBus bus) {
        CustomRegistries.init(bus);
        ITEMS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }

    public static <T extends Item> Supplier<T> tab(Supplier<T> supplier) {
        return Hazardous.instance.setup.tab(supplier);
    }
}
