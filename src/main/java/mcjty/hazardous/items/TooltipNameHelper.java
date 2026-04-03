package mcjty.hazardous.items;

import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.hazardous.setup.HazardAttributes;
import mcjty.lib.varia.Tools;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;

final class TooltipNameHelper {

    private TooltipNameHelper() {
    }

    public static String getAttributeName(@Nullable ResourceLocation attributeId) {
        if (attributeId == null) {
            return "disabled";
        }

        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute != null) {
            return translatedOrFallback(attribute.getDescriptionId(), humanize(attributeId));
        }

        return translatedOrFallback(getAttributeTranslationKey(attributeId), humanize(attributeId));
    }

    public static String getHazardTargetName(@Nullable Level level, @Nullable ResourceLocation hazardTypeId) {
        if (hazardTypeId == null) {
            return "disabled";
        }

        if (level != null) {
            Registry<HazardType> hazardTypes = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);
            HazardType hazardType = hazardTypes.get(hazardTypeId);
            if (hazardType != null) {
                ResourceLocation attributeId = HazardAttributes.resolveResistanceAttributeId(hazardTypeId, hazardType);
                if (attributeId != null) {
                    return getAttributeName(attributeId);
                }
            }
        }

        return translatedOrFallback(getHazardTypeTranslationKey(hazardTypeId), humanize(hazardTypeId));
    }

    private static String getAttributeTranslationKey(ResourceLocation attributeId) {
        return "attribute.name." + attributeId.getNamespace() + "." + attributeId.getPath();
    }

    private static String getHazardTypeTranslationKey(ResourceLocation hazardTypeId) {
        return "hazardtype." + hazardTypeId.getNamespace() + "." + hazardTypeId.getPath();
    }

    private static String translatedOrFallback(String translationKey, String fallback) {
        String translated = Component.translatable(translationKey).getString();
        return translationKey.equals(translated) ? fallback : translated;
    }

    private static String humanize(ResourceLocation id) {
        String namespace = humanizeToken(id.getNamespace());
        String path = humanizeToken(id.getPath());
        if ("minecraft".equals(id.getNamespace()) || "hazardous".equals(id.getNamespace())) {
            return path;
        }
        return namespace + " " + path;
    }

    private static String humanizeToken(String token) {
        return Arrays.stream(token.split("[_\\-/]+"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase(Locale.ROOT))
                .reduce((left, right) -> left + " " + right)
                .orElse(token);
    }
}
