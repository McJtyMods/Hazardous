package mcjty.hazardous.items;

import mcjty.hazardous.setup.Config;
import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.items.BaseItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ResistancePillsItem extends BaseItem {

    private static final TooltipBuilder TOOLTIP = new TooltipBuilder()
            .info(
                    TooltipBuilder.header(),
                    TooltipBuilder.general("desc"),
                    TooltipBuilder.parameter("attribute", stack -> Config.getResistancePillsAttribute().map(ResourceLocation::toString).orElse("disabled")),
                    TooltipBuilder.parameter("amount", stack -> String.format(Locale.ROOT, "%.2f", Mth.clamp(Config.RESISTANCE_PILLS_AMOUNT.get(), 0.0, 1.0))),
                    TooltipBuilder.general("usage")
            );

    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationMod(0.1f)
            .alwaysEat()
            .build();

    public ResistancePillsItem() {
        super(new Properties().food(FOOD));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return result;
        }

        Optional<ResourceLocation> attributeId = Config.getResistancePillsAttribute();
        double amount = Mth.clamp(Config.RESISTANCE_PILLS_AMOUNT.get(), 0.0, 1.0);
        if (attributeId.isEmpty() || amount <= 0.0) {
            return result;
        }

        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId.get());
        if (attribute == null) {
            return result;
        }

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return result;
        }

        double newValue = instance.getBaseValue() + amount;
        if (attribute instanceof RangedAttribute rangedAttribute) {
            newValue = Mth.clamp(newValue, rangedAttribute.getMinValue(), rangedAttribute.getMaxValue());
        }
        instance.setBaseValue(newValue);
        player.playSound(SoundEvents.GENERIC_DRINK, 0.6f, 1.1f);
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(this);
        if (id != null && TOOLTIP.isActive()) {
            TOOLTIP.makeTooltip(id, stack, tooltip, flag);
        }
    }
}
