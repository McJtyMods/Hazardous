package mcjty.hazardous.items;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;

public final class TooltipNameHelper {

    private TooltipNameHelper() {
    }

    public static String getAttributeName(@Nullable ResourceLocation attributeId) {
        if (attributeId == null) {
            return "disabled";
        }

        return translatedOrFallback(getAttributeTranslationKey(attributeId), humanize(attributeId));
    }

    public static String getHazardTypeName(@Nullable ResourceLocation hazardTypeId) {
        if (hazardTypeId == null) {
            return "disabled";
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
