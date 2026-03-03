package mcjty.hazardous.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import mcjty.hazardous.Hazardous;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Stores accumulated dose per HazardType (identified by ResourceLocation)
 * for a given player.
 */
public class PlayerDoseData {

    private final Map<ResourceLocation, Double> doses = new HashMap<>();

    private static final Codec<Map<ResourceLocation, Double>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE);

    public double getDose(ResourceLocation hazardType) {
        return doses.getOrDefault(hazardType, 0.0);
    }

    public void setDose(ResourceLocation hazardType, double value) {
        if (value == 0.0) {
            doses.remove(hazardType);
        } else {
            doses.put(hazardType, value);
        }
    }

    public void addDose(ResourceLocation hazardType, double amount) {
        if (amount == 0.0) {
            return;
        }
        setDose(hazardType, getDose(hazardType) + amount);
    }

    public double removeDoseFromAll(double amount) {
        if (amount <= 0.0 || doses.isEmpty()) {
            return 0.0;
        }
        double removed = 0.0;
        Iterator<Map.Entry<ResourceLocation, Double>> iterator = doses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, Double> entry = iterator.next();
            double currentValue = entry.getValue();
            double newValue = Math.max(0.0, currentValue - amount);
            removed += currentValue - newValue;
            if (newValue <= 0.0) {
                iterator.remove();
            } else if (newValue != currentValue) {
                entry.setValue(newValue);
            }
        }
        return removed;
    }

    public void clear() {
        doses.clear();
    }

    public void copyFrom(PlayerDoseData oldStore) {
        this.doses.clear();
        this.doses.putAll(oldStore.doses);
    }

    public Tag saveNBTData() {
        DataResult<Tag> result = CODEC.encodeStart(NbtOps.INSTANCE, doses);
        return result.result().orElseGet(() -> {
            Hazardous.LOGGER.error("Failed to encode PlayerDoseData NBT");
            return Tag.TAG_END == 0 ? null : null;
        });
    }

    public void loadNBTData(Tag tag) {
        if (tag == null) {
            this.doses.clear();
            return;
        }
        CODEC.decode(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> Hazardous.LOGGER.error("Failed to decode PlayerDoseData: {}", error))
                .ifPresent(pair -> {
                    this.doses.clear();
                    this.doses.putAll(pair.getFirst());
                });
    }
}
