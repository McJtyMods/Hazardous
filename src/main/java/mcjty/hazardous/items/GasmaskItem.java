package mcjty.hazardous.items;

import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import mcjty.lib.builder.TooltipBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GasmaskItem extends ArmorItem {

    private static final TooltipBuilder TOOLTIP = new TooltipBuilder()
            .info(
                    TooltipBuilder.header(),
                    TooltipBuilder.general("desc"),
                    TooltipBuilder.parameter("source", stack -> Config.getGasmaskProtectedSource().map(ResourceLocation::toString).orElse("disabled")),
                    TooltipBuilder.parameter("protection", stack -> String.format(Locale.ROOT, "%.0f%%", Mth.clamp(Config.GASMASK_PROTECTION_LEVEL.get(), 0.0, 1.0) * 100.0)),
                    TooltipBuilder.general("durability")
            );

    public GasmaskItem() {
        super(GasmaskArmorMaterial.INSTANCE, Type.HELMET, new Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(this);
        if (id != null && TOOLTIP.isActive()) {
            TOOLTIP.makeTooltip(id, stack, tooltip, flag);
        }
    }

    public static double applyProtectionAndDamage(Player player, ResourceLocation hazardType, double input) {
        if (input <= 0) {
            return input;
        }

        Optional<ResourceLocation> protectedSource = Config.getGasmaskProtectedSource();
        if (protectedSource.isEmpty() || !protectedSource.get().equals(hazardType)) {
            return input;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(Registration.GASMASK.get()) || getRemainingDurability(helmet) <= 0) {
            return input;
        }

        double protectionLevel = Mth.clamp(Config.GASMASK_PROTECTION_LEVEL.get(), 0.0, 1.0);
        if (protectionLevel <= 0) {
            return input;
        }

        damageByOne(helmet);
        return input * (1.0 - protectionLevel);
    }

    public static int restoreDurability(ItemStack stack, int amount) {
        if (!(stack.getItem() instanceof GasmaskItem) || amount <= 0 || !stack.isDamageableItem()) {
            return 0;
        }
        int currentDamage = stack.getDamageValue();
        if (currentDamage <= 0) {
            return 0;
        }
        int newDamage = Math.max(0, currentDamage - amount);
        stack.setDamageValue(newDamage);
        return currentDamage - newDamage;
    }

    public static int getRemainingDurability(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 0;
        }
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    private static void damageByOne(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return;
        }
        int maxDamage = stack.getMaxDamage();
        int damage = stack.getDamageValue();
        if (damage >= maxDamage) {
            return;
        }
        stack.setDamageValue(Math.min(maxDamage, damage + 1));
    }
}
