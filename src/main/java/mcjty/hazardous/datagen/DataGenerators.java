package mcjty.hazardous.datagen;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.DefaultEffectEntries;
import mcjty.hazardous.data.DefaultHazardSources;
import mcjty.hazardous.data.DefaultHazardTypes;
import mcjty.hazardous.data.objects.EffectEntry;
import mcjty.hazardous.data.objects.HazardSource;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.hazardous.setup.Registration;
import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.stream.Collectors;

public class DataGenerators {

    private static final TagKey<Item> CURIO_TAG = TagKey.create(Registries.ITEM, new ResourceLocation("curios", "curio"));

    public static void datagen(DataGen dataGen) {
        dataGen.addCodecProvider("hazardtypes", Hazardous.MODID + "/hazardtypes", HazardType.CODEC);
        dataGen.addCodecProvider("hazardsources", Hazardous.MODID + "/hazardsources", HazardSource.CODEC);
        dataGen.addCodecProvider("effectentries", Hazardous.MODID + "/effectentries", EffectEntry.CODEC);
        dataGen.add(
                Dob.builder()
                        .codecObjectSupplier("hazardtypes", () -> DefaultHazardTypes.DEFAULT_HAZARD_TYPES.entrySet().stream()
                                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight))),
                Dob.builder()
                        .codecObjectSupplier("hazardsources", () -> DefaultHazardSources.DEFAULT_HAZARD_SOURCES.entrySet().stream()
                                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight))),
                Dob.builder()
                        .codecObjectSupplier("effectentries", () -> DefaultEffectEntries.DEFAULT_EFFECT_ENTRIES.entrySet().stream()
                                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight))),
                Dob.builder()
                        .message("attribute.name.hazardous.solar_burn_resistance", "Solar Resistance"),
                Dob.builder()
                        .message("attribute.name.hazardous.radioactive_type_resistance", "Radiation Resistance"),
                Dob.builder()
                        .message("attribute.name.hazardous.lostcity_radiation_resistance", "Lost City Radiation Resistance"),
                Dob.builder()
                        .message("attribute.name.hazardous.lava_heat_resistance", "Heat Resistance"),
                Dob.builder()
                        .message("hazardtype.hazardous.lava_heat", "Heat"),
                Dob.builder()
                        .message("hazardtype.hazardous.lostcity_radiation", "Lost City Radiation"),
                Dob.builder()
                        .message("hazardtype.hazardous.radioactive_type", "Radiation"),
                Dob.builder()
                        .message("hazardtype.hazardous.solar_burn", "Solar Burn"),
                Dob.builder()
                        .message("itemGroup." + Hazardous.MODID, "Hazardous"),
                Dob.builder()
                        .message("hazardous.datapack.hazardtype.resistance_attribute", "Resistance Attribute"),
                Dob.itemBuilder(Registration.GEIGER_COUNTER)
                        .name("Geiger Counter")
                        .generatedItem("item/geigercounter")
                        .recipeConsumer(() -> consumer -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.GEIGER_COUNTER.get())
                                .define('q', Items.QUARTZ)
                                .define('t', Items.REDSTONE_TORCH)
                                .define('d', Items.DIAMOND)
                                .define('c', Items.COMPARATOR)
                                .define('i', Items.IRON_INGOT)
                                .pattern("qtq")
                                .pattern("dcd")
                                .pattern("qiq")
                                .unlockedBy("has_comparator", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COMPARATOR))
                                .save(consumer))
                        .itemTags(List.of(CURIO_TAG)),
                Dob.itemBuilder(Registration.DOSIMETER)
                        .name("Dosimeter")
                        .generatedItem("item/dosimeter")
                        .recipeConsumer(() -> consumer -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.DOSIMETER.get())
                                .define('q', Items.QUARTZ)
                                .define('t', Items.REDSTONE_TORCH)
                                .define('i', Items.IRON_INGOT)
                                .define('c', Items.CLOCK)
                                .define('g', Items.GOLD_INGOT)
                                .pattern("qtq")
                                .pattern("ici")
                                .pattern("qgq")
                                .unlockedBy("has_clock", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CLOCK))
                                .save(consumer))
                        .itemTags(List.of(CURIO_TAG)),
                Dob.itemBuilder(Registration.FILTER)
                        .name("Filter")
                        .generatedItem("item/filter")
                        .keyedMessage("header", "Gas Mask Filter")
                        .keyedMessage("desc", "Refills a worn gas mask")
                        .keyedMessage("restore", "Durability restored: ")
                        .keyedMessage("rightclick", "Right-click while wearing a gas mask to refill it")
                        .keyedMessage("crafting", "Can also be combined with a damaged gas mask in a crafting grid")
                        .recipeConsumer(() -> consumer -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.FILTER.get())
                                .define('g', Items.GRAVEL)
                                .define('c', Items.CHARCOAL)
                                .define('s', Items.SAND)
                                .pattern("gcg")
                                .pattern("csc")
                                .pattern("gcg")
                                .unlockedBy("has_charcoal", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CHARCOAL))
                                .save(consumer)),
                Dob.itemBuilder(Registration.GASMASK)
                        .name("Gas Mask")
                        .generatedItem("item/gasmask")
                        .keyedMessage("header", "Gas Mask")
                        .keyedMessage("desc", "Protects against radiation")
                        .keyedMessage("source", "Protected source: ")
                        .keyedMessage("protection", "Protection level: ")
                        .keyedMessage("durability", "At zero durability it stays equipped but provides no protection")
                        .recipeConsumer(() -> consumer -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.GASMASK.get())
                                .define('i', Items.IRON_INGOT)
                                .define('g', Items.GLASS)
                                .define('f', Registration.FILTER.get())
                                .pattern("iii")
                                .pattern("gfg")
                                .pattern("iii")
                                .unlockedBy("has_filter", InventoryChangeTrigger.TriggerInstance.hasItems(Registration.FILTER.get()))
                                .save(consumer))
                        .itemTags(List.of(CURIO_TAG)),
                Dob.itemBuilder(Registration.PILLS)
                        .name("Pills")
                        .generatedItem("item/pills")
                        .keyedMessage("header", "Anti-Rad Pills")
                        .keyedMessage("desc", "Reduces accumulated personal dose for one configured resistance attribute")
                        .keyedMessage("attribute", "Target attribute: ")
                        .keyedMessage("heal", "Dose restored per use: ")
                        .keyedMessage("cured", "Dose cured: %s")
                        .keyedMessage("usage", "Right-click to consume and reduce matching dose")
                        .recipeConsumer(() -> consumer -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.PILLS.get())
                                .define('c', Items.CHARCOAL)
                                .define('g', Items.GOLDEN_APPLE)
                                .pattern("ccc")
                                .pattern("cgc")
                                .pattern("ccc")
                                .unlockedBy("has_golden_apple", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GOLDEN_APPLE))
                                .save(consumer)),
                Dob.itemBuilder(Registration.RESISTANCE_PILLS)
                        .name("Resistance Pills")
                        .generatedItem("item/resistance_pills")
                        .keyedMessage("header", "Resistance Pills")
                        .keyedMessage("desc", "Add temporary resistance when eaten")
                        .keyedMessage("attribute", "Granted attribute: ")
                        .keyedMessage("amount", "Amount per use: ")
                        .keyedMessage("duration", "Duration per use: ")
                        .keyedMessage("max_stacks", "Active stack limit reached")
                        .keyedMessage("hud_title", "Resistance Active")
                        .keyedMessage("usage", "Eat to temporarily raise resistance")
                        .recipeConsumer(() -> consumer -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Registration.RESISTANCE_PILLS.get())
                                .define('p', Registration.PILLS.get())
                                .define('q', Items.QUARTZ)
                                .define('g', Items.GOLD_INGOT)
                                .pattern(" q ")
                                .pattern("gpg")
                                .pattern(" g ")
                                .unlockedBy("has_pills", InventoryChangeTrigger.TriggerInstance.hasItems(Registration.PILLS.get()))
                                .save(consumer))
        );
    }
}
