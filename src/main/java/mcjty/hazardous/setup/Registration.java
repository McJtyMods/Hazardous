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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class Registration {

    public static final DeferredItems ITEMS = DeferredItems.create(Hazardous.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Hazardous.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Hazardous.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Hazardous.MODID);

    public static final DeferredItem<GeigerCounterItem> GEIGER_COUNTER = ITEMS.register("geiger_counter", tab(GeigerCounterItem::new));
    public static final DeferredItem<GasmaskItem> GASMASK = ITEMS.register("gasmask", tab(GasmaskItem::new));
    public static final DeferredItem<FilterItem> FILTER = ITEMS.register("filter", tab(FilterItem::new));
    public static final DeferredItem<PillsItem> PILLS = ITEMS.register("pills", tab(PillsItem::new));

    public static final RegistryObject<SoundEvent> GEIGER_MEDIUMDOSE = SOUND_EVENTS.register("geiger.mediumdose",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Hazardous.MODID, "geiger.mediumdose")));
    public static final RegistryObject<SoundEvent> GEIGER_HIGHDOSE = SOUND_EVENTS.register("geiger.highdose",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Hazardous.MODID, "geiger.highdose")));

    public static final RegistryObject<SimpleCraftingRecipeSerializer<GasmaskFilterRecipe>> GASMASK_FILTER_RECIPE = RECIPE_SERIALIZERS.register("gasmask_filter_refill",
            () -> new SimpleCraftingRecipeSerializer<>(GasmaskFilterRecipe::new));

    public static void register(IEventBus bus) {
        CustomRegistries.init(bus);
        ITEMS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        SOUND_EVENTS.register(bus);
        TABS.register(bus);
    }

    public static <T extends Item> Supplier<T> tab(Supplier<T> supplier) {
        return Hazardous.instance.setup.tab(supplier);
    }

    public static RegistryObject<CreativeModeTab> TAB = TABS.register("fancytrinkets", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Hazardous.MODID))
            .icon(() -> new ItemStack(GEIGER_COUNTER.get()))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .displayItems((featureFlags, output) -> {
                Hazardous.setup.populateTab(output);
            })
            .build());

}
