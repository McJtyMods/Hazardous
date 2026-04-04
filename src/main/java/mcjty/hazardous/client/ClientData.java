package mcjty.hazardous.client;

import mcjty.hazardous.data.PlayerDoseData;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ClientData {
    private static Map<ResourceLocation, Double> doseValues = new HashMap<>();
    static Map<ResourceLocation, Double> radiationValues = new HashMap<>();
    private static Map<ResourceLocation, PlayerDoseData.ResistancePillStatus> resistancePillStatuses = new HashMap<>();

    public static void setDoseValues(Map<ResourceLocation, Double> doseValues) {
        ClientData.doseValues = doseValues;
    }

    public static Map<ResourceLocation, Double> getDoseValues() {
        return doseValues;
    }

    public static void setRadiationValues(Map<ResourceLocation, Double> radiationValues) {
        ClientData.radiationValues = radiationValues;
    }

    public static Map<ResourceLocation, Double> getRadiationValues() {
        return radiationValues;
    }

    public static void setResistancePillStatuses(Map<ResourceLocation, PlayerDoseData.ResistancePillStatus> resistancePillStatuses) {
        ClientData.resistancePillStatuses = resistancePillStatuses;
    }

    public static Map<ResourceLocation, PlayerDoseData.ResistancePillStatus> getResistancePillStatuses() {
        return resistancePillStatuses;
    }
}
