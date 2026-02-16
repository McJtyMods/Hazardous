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
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

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
                Dob.itemBuilder(Registration.GEIGER_COUNTER)
                        .name("Geiger Counter")
                        .generatedItem("item/geigercounter")
                        .itemTags(List.of(CURIO_TAG))
        );
    }
}
