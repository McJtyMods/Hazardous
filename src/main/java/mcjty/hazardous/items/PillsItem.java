package mcjty.hazardous.items;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.data.PlayerDoseData;
import mcjty.hazardous.data.PlayerDoseDispatcher;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.HazardAttributes;
import mcjty.hazardous.setup.Registration;
import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.items.BaseItem;
import mcjty.lib.varia.Tools;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PillsItem extends BaseItem {

    private static final TooltipBuilder TOOLTIP = new TooltipBuilder()
            .info(
                    TooltipBuilder.header(),
                    TooltipBuilder.general("desc"),
                    TooltipBuilder.parameter("attribute", stack -> TooltipNameHelper.getAttributeName(Config.getPillsAttribute().orElse(null))),
                    TooltipBuilder.parameter("heal", stack -> String.format(Locale.ROOT, "%.2f", Mth.clamp(Config.PILLS_DOSE_HEAL.get(), 0.0, 1_000_000.0))),
                    TooltipBuilder.general("usage")
            );

    public PillsItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        Optional<ResourceLocation> attributeId = Config.getPillsAttribute();
        double healAmount = Mth.clamp(Config.PILLS_DOSE_HEAL.get(), 0.0, 1_000_000.0);
        if (attributeId.isEmpty() || healAmount <= 0.0) {
            return InteractionResultHolder.pass(stack);
        }

        Registry<HazardType> hazardTypes = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);
        double curedDose = PlayerDoseDispatcher.getPlayerDose(player)
                .map(data -> removeDoseForAttribute(data, hazardTypes, attributeId.get(), healAmount))
                .orElse(0.0);
        if (curedDose <= 0.0) {
            return InteractionResultHolder.pass(stack);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), Registration.PILLS_USE.get(), SoundSource.PLAYERS, 0.6f, 1.1f);
        player.displayClientMessage(
                Component.translatable(
                        "message." + Hazardous.MODID + ".pills.cured",
                        String.format(Locale.ROOT, "%.2f", curedDose)
                ),
                false
        );
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(this);
        if (id != null && TOOLTIP.isActive()) {
            TOOLTIP.makeTooltip(id, stack, tooltip, flag);
        }
    }

    private static double removeDoseForAttribute(PlayerDoseData data, Registry<HazardType> hazardTypes, ResourceLocation attributeId, double healAmount) {
        double removed = 0.0;
        for (HazardType hazardType : hazardTypes) {
            ResourceLocation hazardTypeId = hazardTypes.getKey(hazardType);
            if (hazardTypeId == null) {
                continue;
            }
            ResourceLocation resistanceAttributeId = HazardAttributes.resolveResistanceAttributeId(hazardTypeId, hazardType);
            if (attributeId.equals(resistanceAttributeId)) {
                removed += data.removeDose(hazardTypeId, healAmount);
            }
        }
        return removed;
    }
}
