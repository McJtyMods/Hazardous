package mcjty.hazardous.items;

import mcjty.hazardous.data.PlayerDoseDispatcher;
import mcjty.hazardous.setup.Config;
import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.items.BaseItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class PillsItem extends BaseItem {

    private static final TooltipBuilder TOOLTIP = new TooltipBuilder()
            .info(
                    TooltipBuilder.header(),
                    TooltipBuilder.general("desc"),
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

        double healAmount = Mth.clamp(Config.PILLS_DOSE_HEAL.get(), 0.0, 1_000_000.0);
        if (healAmount <= 0.0) {
            return InteractionResultHolder.pass(stack);
        }

        AtomicBoolean changed = new AtomicBoolean(false);
        PlayerDoseDispatcher.getPlayerDose(player).ifPresent(data -> changed.set(data.removeDoseFromAll(healAmount)));
        if (!changed.get()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.playSound(SoundEvents.GENERIC_DRINK, 0.6f, 1.1f);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(this);
        if (id != null && TOOLTIP.isActive()) {
            TOOLTIP.makeTooltip(id, stack, tooltip, flag);
        }
    }
}
