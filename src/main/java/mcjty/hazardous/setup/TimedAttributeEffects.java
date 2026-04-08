package mcjty.hazardous.setup;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import mcjty.hazardous.data.PlayerHazardData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TimedAttributeEffects {

    private static final String MODIFIER_NAME = "hazardous.resistance_pills";
    public static final Codec<Attribute> ATTRIBUTE_CODEC = ResourceLocation.CODEC.comapFlatMap(
            TimedAttributeEffects::decodeAttribute,
            TimedAttributeEffects::encodeAttribute
    );
    public static final Codec<AttributeModifier.Operation> ATTRIBUTE_MODIFIER_OPERATION_CODEC = Codec.STRING.comapFlatMap(
            TimedAttributeEffects::decodeOperation,
            TimedAttributeEffects::encodeOperation
    );

    public static void syncPlayer(Player player, PlayerHazardData store, long gameTime) {
        syncResistancePills(player, store, gameTime);
        syncTimedAttributes(player, store, gameTime);
    }

    private static void syncResistancePills(Player player, PlayerHazardData store, long gameTime) {
        Set<ResourceLocation> trackedAttributes = new HashSet<>(store.getResistancePillAttributeIds());
        Map<ResourceLocation, Double> activeBonuses = store.getActiveResistancePillBonuses(gameTime);
        trackedAttributes.addAll(activeBonuses.keySet());

        for (ResourceLocation attributeId : trackedAttributes) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
            if (attribute == null) {
                continue;
            }

            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            UUID modifierId = getModifierId(attributeId);
            AttributeModifier existing = instance.getModifier(modifierId);
            double amount = activeBonuses.getOrDefault(attributeId, 0.0);
            if (amount <= 0.0) {
                if (existing != null) {
                    instance.removeModifier(modifierId);
                }
                continue;
            }

            if (existing != null
                    && existing.getOperation() == AttributeModifier.Operation.ADDITION
                    && Double.compare(existing.getAmount(), amount) == 0) {
                continue;
            }

            if (existing != null) {
                instance.removeModifier(modifierId);
            }
            instance.addTransientModifier(new AttributeModifier(modifierId, MODIFIER_NAME, amount, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void syncTimedAttributes(Player player, PlayerHazardData store, long gameTime) {
        Set<PlayerHazardData.TimedAttributeKey> trackedEffects = new HashSet<>(store.getTimedAttributeKeys());
        Map<PlayerHazardData.TimedAttributeKey, PlayerHazardData.TimedAttributeModifier> activeEffects = getActiveTimedAttributeModifiers(store, gameTime);
        trackedEffects.addAll(activeEffects.keySet());

        for (PlayerHazardData.TimedAttributeKey effectKey : trackedEffects) {
            AttributeInstance instance = player.getAttribute(effectKey.attribute());
            if (instance == null) {
                continue;
            }

            AttributeModifier existing = instance.getModifier(effectKey.uuid());
            PlayerHazardData.TimedAttributeModifier activeEffect = activeEffects.get(effectKey);
            if (activeEffect == null) {
                if (existing != null) {
                    instance.removeModifier(effectKey.uuid());
                }
                continue;
            }

            if (existing != null
                    && existing.getOperation() == activeEffect.operation()
                    && Double.compare(existing.getAmount(), activeEffect.amount()) == 0
                    && existing.getName().equals(activeEffect.name())) {
                continue;
            }

            if (existing != null) {
                instance.removeModifier(effectKey.uuid());
            }
            instance.addTransientModifier(new AttributeModifier(effectKey.uuid(), activeEffect.name(), activeEffect.amount(), activeEffect.operation()));
        }
    }

    public static void clearModifier(Player player, ResourceLocation attributeId) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            return;
        }

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        instance.removeModifier(getModifierId(attributeId));
    }

    public static void clearAllModifiers(Player player, PlayerHazardData store) {
        for (ResourceLocation attributeId : store.getResistancePillAttributeIds()) {
            clearModifier(player, attributeId);
        }
        for (PlayerHazardData.TimedAttributeKey effectKey : store.getTimedAttributeKeys()) {
            clearTimedAttributeModifier(player, effectKey);
        }
    }

    private static DataResult<AttributeModifier.Operation> decodeOperation(String operation) {
        return switch (operation) {
            case "add", "addition" -> DataResult.success(AttributeModifier.Operation.ADDITION);
            case "multiply_base" -> DataResult.success(AttributeModifier.Operation.MULTIPLY_BASE);
            case "multiply_total" -> DataResult.success(AttributeModifier.Operation.MULTIPLY_TOTAL);
            default -> DataResult.error(() -> "Unknown attribute modifier operation '" + operation + "'");
        };
    }

    private static DataResult<Attribute> decodeAttribute(ResourceLocation attributeId) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            return DataResult.error(() -> "Unknown attribute '" + attributeId + "'");
        }
        return DataResult.success(attribute);
    }

    private static ResourceLocation encodeAttribute(Attribute attribute) {
        ResourceLocation attributeId = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (attributeId == null) {
            throw new IllegalStateException("Cannot encode unregistered attribute " + attribute);
        }
        return attributeId;
    }

    private static String encodeOperation(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADDITION -> "add";
            case MULTIPLY_BASE -> "multiply_base";
            case MULTIPLY_TOTAL -> "multiply_total";
        };
    }

    private static Map<PlayerHazardData.TimedAttributeKey, PlayerHazardData.TimedAttributeModifier> getActiveTimedAttributeModifiers(PlayerHazardData store, long gameTime) {
        Map<PlayerHazardData.TimedAttributeKey, PlayerHazardData.TimedAttributeModifier> activeEffects = new java.util.HashMap<>();
        activeEffects.putAll(store.getActiveTimedAttributeEffects(gameTime));
        return activeEffects;
    }

    private static void clearTimedAttributeModifier(Player player, PlayerHazardData.TimedAttributeKey effectKey) {
        AttributeInstance instance = player.getAttribute(effectKey.attribute());
        if (instance == null) {
            return;
        }

        instance.removeModifier(effectKey.uuid());
    }

    private static UUID getModifierId(ResourceLocation attributeId) {
        return UUID.nameUUIDFromBytes(("hazardous:resistance_pills:" + attributeId).getBytes(StandardCharsets.UTF_8));
    }
}
