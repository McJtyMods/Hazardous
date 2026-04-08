package mcjty.hazardous.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.hazardous.Hazardous;
import mcjty.hazardous.setup.ResistancePillEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores accumulated dose per HazardType (identified by ResourceLocation)
 * for a given player.
 */
public class PlayerDoseData {

    private final Map<ResourceLocation, Double> doses = new HashMap<>();
    private final Map<ResourceLocation, List<ResistancePillEffect>> resistancePills = new HashMap<>();
    private final Map<ResourceLocation, List<TimedAttributeEffect>> timedAttributes = new HashMap<>();

    private static final Codec<Map<ResourceLocation, Double>> DOSE_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE);
    private static final Codec<Map<ResourceLocation, List<ResistancePillEffect>>> RESISTANCE_PILLS_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, ResistancePillEffect.CODEC.listOf());
    private static final Codec<Map<ResourceLocation, List<TimedAttributeEffect>>> TIMED_ATTRIBUTES_CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, TimedAttributeEffect.CODEC.listOf());

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

    public double removeDose(ResourceLocation hazardType, double amount) {
        if (amount <= 0.0) {
            return 0.0;
        }
        double currentValue = getDose(hazardType);
        if (currentValue <= 0.0) {
            return 0.0;
        }
        double newValue = Math.max(0.0, currentValue - amount);
        setDose(hazardType, newValue);
        return currentValue - newValue;
    }

    public void clear() {
        doses.clear();
        resistancePills.clear();
        timedAttributes.clear();
    }

    public void clearResistancePills() {
        resistancePills.clear();
    }

    public void clearTimedAttributes() {
        timedAttributes.clear();
    }

    public void copyFrom(PlayerDoseData oldStore) {
        this.doses.clear();
        this.doses.putAll(oldStore.doses);
        this.resistancePills.clear();
        oldStore.resistancePills.forEach((attributeId, effects) -> this.resistancePills.put(attributeId, new ArrayList<>(effects)));
        this.timedAttributes.clear();
        oldStore.timedAttributes.forEach((attributeId, effects) -> this.timedAttributes.put(attributeId, new ArrayList<>(effects)));
    }

    public boolean addResistancePillEffect(ResourceLocation attributeId, double amount, long expiresAt, int maxStacks, long gameTime) {
        if (amount <= 0.0) {
            return false;
        }
        List<ResistancePillEffect> effects = resistancePills.computeIfAbsent(attributeId, id -> new ArrayList<>());
        pruneExpiredEffects(effects, gameTime);
        if (maxStacks > 0 && effects.size() >= maxStacks) {
            if (effects.isEmpty()) {
                resistancePills.remove(attributeId);
            }
            return false;
        }
        effects.add(new ResistancePillEffect(amount, expiresAt));
        return true;
    }

    public Set<ResourceLocation> getResistancePillAttributeIds() {
        return Set.copyOf(resistancePills.keySet());
    }

    public boolean addTimedAttributeEffect(ResourceLocation attributeId, UUID uuid, String name, double amount, AttributeModifier.Operation operation, long expiresAt, long gameTime) {
        if (amount == 0.0 || expiresAt <= gameTime) {
            return false;
        }
        List<TimedAttributeEffect> effects = timedAttributes.computeIfAbsent(attributeId, id -> new ArrayList<>());
        pruneExpiredTimedAttributeEffects(effects, gameTime);
        effects.removeIf(effect -> effect.uuid().equals(uuid));
        effects.add(new TimedAttributeEffect(uuid, name, amount, operation, expiresAt));
        return true;
    }

    public Set<ResourceLocation> getTimedAttributeAttributeIds() {
        return Set.copyOf(timedAttributes.keySet());
    }

    public Set<TimedAttributeKey> getTimedAttributeKeys() {
        Set<TimedAttributeKey> keys = new java.util.HashSet<>();
        for (Map.Entry<ResourceLocation, List<TimedAttributeEffect>> entry : timedAttributes.entrySet()) {
            for (TimedAttributeEffect effect : entry.getValue()) {
                keys.add(new TimedAttributeKey(entry.getKey(), effect.uuid()));
            }
        }
        return keys;
    }

    public Map<TimedAttributeKey, TimedAttributeModifier> getActiveTimedAttributeEffects(long gameTime) {
        Map<TimedAttributeKey, TimedAttributeModifier> activeEffects = new HashMap<>();
        Iterator<Map.Entry<ResourceLocation, List<TimedAttributeEffect>>> entryIterator = timedAttributes.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<ResourceLocation, List<TimedAttributeEffect>> entry = entryIterator.next();
            pruneExpiredTimedAttributeEffects(entry.getValue(), gameTime);
            if (entry.getValue().isEmpty()) {
                entryIterator.remove();
                continue;
            }
            for (TimedAttributeEffect effect : entry.getValue()) {
                activeEffects.put(new TimedAttributeKey(entry.getKey(), effect.uuid()), TimedAttributeModifier.fromEffect(effect));
            }
        }
        return activeEffects;
    }

    public Map<ResourceLocation, Double> getActiveResistancePillBonuses(long gameTime) {
        Map<ResourceLocation, Double> activeBonuses = new HashMap<>();
        for (Map.Entry<ResourceLocation, ResistancePillStatus> entry : getActiveResistancePillStatuses(gameTime).entrySet()) {
            activeBonuses.put(entry.getKey(), entry.getValue().amount());
        }
        return activeBonuses;
    }

    public Map<ResourceLocation, ResistancePillStatus> getActiveResistancePillStatuses(long gameTime) {
        Map<ResourceLocation, ResistancePillStatus> activeBonuses = new HashMap<>();
        Iterator<Map.Entry<ResourceLocation, List<ResistancePillEffect>>> entryIterator = resistancePills.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<ResourceLocation, List<ResistancePillEffect>> entry = entryIterator.next();
            double total = 0.0;
            int stacks = 0;
            long latestExpiry = 0L;
            pruneExpiredEffects(entry.getValue(), gameTime);
            for (ResistancePillEffect effect : entry.getValue()) {
                total += effect.amount();
                stacks++;
                latestExpiry = Math.max(latestExpiry, effect.expiresAt());
            }
            if (entry.getValue().isEmpty()) {
                entryIterator.remove();
            } else if (total > 0.0 && stacks > 0) {
                activeBonuses.put(entry.getKey(), new ResistancePillStatus(total, stacks, latestExpiry));
            }
        }
        return activeBonuses;
    }

    private static void pruneExpiredEffects(List<ResistancePillEffect> effects, long gameTime) {
        Iterator<ResistancePillEffect> effectIterator = effects.iterator();
        while (effectIterator.hasNext()) {
            if (effectIterator.next().expiresAt() <= gameTime) {
                effectIterator.remove();
            }
        }
    }

    private static void pruneExpiredTimedAttributeEffects(List<TimedAttributeEffect> effects, long gameTime) {
        Iterator<TimedAttributeEffect> effectIterator = effects.iterator();
        while (effectIterator.hasNext()) {
            if (effectIterator.next().expiresAt() <= gameTime) {
                effectIterator.remove();
            }
        }
    }

    public Tag saveNBTData() {
        CompoundTag compound = new CompoundTag();
        encodeField("doses", DOSE_CODEC, doses, compound);
        encodeField("resistancePills", RESISTANCE_PILLS_CODEC, resistancePills, compound);
        encodeField("timedAttributes", TIMED_ATTRIBUTES_CODEC, timedAttributes, compound);
        return compound;
    }

    public void loadNBTData(Tag tag) {
        this.doses.clear();
        this.resistancePills.clear();
        this.timedAttributes.clear();
        if (tag == null) {
            return;
        }
        if (tag instanceof CompoundTag compound && (compound.contains("doses") || compound.contains("resistancePills") || compound.contains("timedAttributes"))) {
            decodeField("doses", compound.get("doses"), DOSE_CODEC).ifPresent(doses::putAll);
            decodeField("resistancePills", compound.get("resistancePills"), RESISTANCE_PILLS_CODEC)
                    .ifPresent(decoded -> decoded.forEach((attributeId, effects) -> resistancePills.put(attributeId, new ArrayList<>(effects))));
            decodeField("timedAttributes", compound.get("timedAttributes"), TIMED_ATTRIBUTES_CODEC)
                    .ifPresent(decoded -> decoded.forEach((attributeId, effects) -> timedAttributes.put(attributeId, new ArrayList<>(effects))));
            return;
        }
        decodeField("legacy doses", tag, DOSE_CODEC).ifPresent(doses::putAll);
    }

    private static <T> void encodeField(String fieldName, Codec<T> codec, T value, CompoundTag compound) {
        DataResult<Tag> result = codec.encodeStart(NbtOps.INSTANCE, value);
        result.resultOrPartial(error -> Hazardous.LOGGER.error("Failed to encode PlayerDoseData {}: {}", fieldName, error))
                .ifPresent(encoded -> compound.put(fieldName, encoded));
    }

    private static <T> java.util.Optional<T> decodeField(String fieldName, Tag tag, Codec<T> codec) {
        if (tag == null) {
            return java.util.Optional.empty();
        }
        return codec.decode(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> Hazardous.LOGGER.error("Failed to decode PlayerDoseData {}: {}", fieldName, error))
                .map(pair -> pair.getFirst());
    }

    private record ResistancePillEffect(double amount, long expiresAt) {
        private static final Codec<ResistancePillEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("amount").forGetter(ResistancePillEffect::amount),
                Codec.LONG.fieldOf("expiresAt").forGetter(ResistancePillEffect::expiresAt)
        ).apply(instance, ResistancePillEffect::new));
    }

    private record TimedAttributeEffect(UUID uuid, String name, double amount, AttributeModifier.Operation operation, long expiresAt) {
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        private static final Codec<TimedAttributeEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUID_CODEC.fieldOf("uuid").forGetter(TimedAttributeEffect::uuid),
                Codec.STRING.fieldOf("name").forGetter(TimedAttributeEffect::name),
                Codec.DOUBLE.fieldOf("amount").forGetter(TimedAttributeEffect::amount),
                ResistancePillEffects.ATTRIBUTE_MODIFIER_OPERATION_CODEC.fieldOf("operation").forGetter(TimedAttributeEffect::operation),
                Codec.LONG.fieldOf("expiresAt").forGetter(TimedAttributeEffect::expiresAt)
        ).apply(instance, TimedAttributeEffect::new));
    }

    public record ResistancePillStatus(double amount, int stacks, long expiresAt) {
    }

    public record TimedAttributeKey(ResourceLocation attributeId, UUID uuid) {
    }

    public record TimedAttributeModifier(UUID uuid, String name, double amount, AttributeModifier.Operation operation, long expiresAt) {
        public static TimedAttributeModifier fromEffect(TimedAttributeEffect effect) {
            return new TimedAttributeModifier(effect.uuid(), effect.name(), effect.amount(), effect.operation(), effect.expiresAt());
        }
    }
}
