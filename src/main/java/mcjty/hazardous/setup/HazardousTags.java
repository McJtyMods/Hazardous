package mcjty.hazardous.setup;

import mcjty.hazardous.Hazardous;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class HazardousTags {

    public static final TagKey<Item> PROTECTIVE_ARMOR = TagKey.create(Registries.ITEM, new ResourceLocation(Hazardous.MODID, "protective_armor"));

    private HazardousTags() {
    }
}
