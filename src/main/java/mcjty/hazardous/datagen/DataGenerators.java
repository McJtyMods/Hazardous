package mcjty.hazardous.datagen;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.DefaultEffectEntries;
import mcjty.hazardous.data.DefaultHazardSources;
import mcjty.hazardous.data.DefaultHazardTypes;
import mcjty.hazardous.data.objects.EffectEntry;
import mcjty.hazardous.data.objects.HazardSource;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.lib.datagen.DataGen;
import mcjty.lib.datagen.Dob;
import org.apache.commons.lang3.tuple.Pair;

import java.util.stream.Collectors;

public class DataGenerators {

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
                                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)))
        );
    }
}
