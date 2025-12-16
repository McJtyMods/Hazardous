package mcjty.hazardous.client;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ClientRadiationData {
    private static Map<ResourceLocation, Double> values = new HashMap<ResourceLocation, Double>();

    public static void setValues(Map<ResourceLocation, Double> values) {
        ClientRadiationData.values = values;
    }

    public static Map<ResourceLocation, Double> getValues() {
        return values;
    }
}
