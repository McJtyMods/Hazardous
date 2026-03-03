package mcjty.hazardous.client;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ClientDoseData {
    private static Map<ResourceLocation, Double> values = new HashMap<>();

    public static void setValues(Map<ResourceLocation, Double> values) {
        ClientDoseData.values = values;
    }

    public static Map<ResourceLocation, Double> getValues() {
        return values;
    }
}
