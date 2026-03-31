package mcjty.hazardous.setup;

import mcjty.hazardous.data.PlayerDoseData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ResistancePillEffects {

    private static final String MODIFIER_NAME = "hazardous.resistance_pills";

    public static void syncPlayer(Player player, PlayerDoseData store, long gameTime) {
        Set<ResourceLocation> trackedAttributes = new java.util.HashSet<>(store.getResistancePillAttributeIds());
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

    private static UUID getModifierId(ResourceLocation attributeId) {
        return UUID.nameUUIDFromBytes(("hazardous:resistance_pills:" + attributeId).getBytes(StandardCharsets.UTF_8));
    }
}
