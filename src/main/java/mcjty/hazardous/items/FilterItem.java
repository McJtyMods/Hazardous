package mcjty.hazardous.items;

import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.items.BaseItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FilterItem extends BaseItem {

    private static final TooltipBuilder TOOLTIP = new TooltipBuilder()
            .info(
                    TooltipBuilder.header(),
                    TooltipBuilder.general("desc"),
                    TooltipBuilder.parameter("restore", stack -> Integer.toString(Config.GASMASK_FILTER_RESTORE.get())),
                    TooltipBuilder.general("rightclick"),
                    TooltipBuilder.general("crafting")
            );

    public FilterItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack filterStack = player.getItemInHand(hand);
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(Registration.GASMASK.get())) {
            return InteractionResultHolder.pass(filterStack);
        }

        if (helmet.getDamageValue() <= 0) {
            return InteractionResultHolder.pass(filterStack);
        }

        if (!level.isClientSide()) {
            int restore = Mth.clamp(Config.GASMASK_FILTER_RESTORE.get(), 1, Integer.MAX_VALUE);
            int repaired = GasmaskItem.restoreDurability(helmet, restore);
            if (repaired <= 0) {
                return InteractionResultHolder.pass(filterStack);
            }
            if (!player.getAbilities().instabuild) {
                filterStack.shrink(1);
            }
            player.playSound(SoundEvents.ANVIL_USE, 0.5f, 1.7f);
        }
        return InteractionResultHolder.sidedSuccess(filterStack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(this);
        if (id != null && TOOLTIP.isActive()) {
            TOOLTIP.makeTooltip(id, stack, tooltip, flag);
        }
    }
}
