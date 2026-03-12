package mcjty.hazardous.setup;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.DefaultHazardTypes;
import mcjty.hazardous.data.objects.HazardType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class HazardAttributes {

    private static final double MIN_RESISTANCE = 0.0;
    private static final double MAX_RESISTANCE = 1.0;

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Hazardous.MODID);
    private static final Map<ResourceLocation, RegistryObject<Attribute>> DEFAULT_RESISTANCE_ATTRIBUTES = new LinkedHashMap<>();

    static {
        for (ResourceLocation hazardTypeId : DefaultHazardTypes.DEFAULT_HAZARD_TYPES.keySet()) {
            ResourceLocation attributeId = getDefaultResistanceAttributeId(hazardTypeId);
            DEFAULT_RESISTANCE_ATTRIBUTES.put(hazardTypeId,
                    ATTRIBUTES.register(attributeId.getPath(), () -> createResistanceAttribute(attributeId)));
        }
    }

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
        bus.addListener(HazardAttributes::onEntityAttributeModification);
    }

    private static Attribute createResistanceAttribute(ResourceLocation attributeId) {
        return new RangedAttribute("attribute.name." + attributeId.getNamespace() + "." + attributeId.getPath(),
                0.0, MIN_RESISTANCE, MAX_RESISTANCE).setSyncable(true);
    }

    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        for (RegistryObject<Attribute> attribute : DEFAULT_RESISTANCE_ATTRIBUTES.values()) {
            event.add(EntityType.PLAYER, attribute.get());
        }
    }

    public static double applyResistance(Player player, ResourceLocation hazardTypeId, HazardType hazardType, double exposure) {
        if (exposure <= 0.0) {
            return 0.0;
        }
        Attribute attribute = resolveResistanceAttribute(hazardTypeId, hazardType);
        if (attribute == null) {
            return exposure;
        }
        double resistance = Mth.clamp(player.getAttributeValue(attribute), MIN_RESISTANCE, MAX_RESISTANCE);
        if (resistance <= 0.0) {
            return exposure;
        }
        return exposure * (1.0 - resistance);
    }

    @Nullable
    public static Attribute resolveResistanceAttribute(ResourceLocation hazardTypeId, HazardType hazardType) {
        ResourceLocation attributeId = resolveResistanceAttributeId(hazardTypeId, hazardType);
        if (attributeId == null) {
            return null;
        }
        return ForgeRegistries.ATTRIBUTES.getValue(attributeId);
    }

    @Nullable
    public static ResourceLocation resolveResistanceAttributeId(ResourceLocation hazardTypeId, HazardType hazardType) {
        ResourceLocation attributeId = hazardType.resistanceAttribute();
        if (attributeId == null) {
            RegistryObject<Attribute> attribute = DEFAULT_RESISTANCE_ATTRIBUTES.get(hazardTypeId);
            return attribute == null ? null : attribute.getId();
        }
        return attributeId;
    }

    public static ResourceLocation getDefaultResistanceAttributeId(ResourceLocation hazardTypeId) {
        return new ResourceLocation(Hazardous.MODID, hazardTypeId.getPath() + "_resistance");
    }

    public static Collection<ResourceLocation> getBuiltInHazardTypesWithResistanceAttributes() {
        return DEFAULT_RESISTANCE_ATTRIBUTES.keySet();
    }
}
