package mcjty.hazardous.items;

import mcjty.hazardous.data.PlayerDoseDispatcher;
import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import mcjty.hazardous.setup.TimedAttributeEffects;
import mcjty.lib.builder.TooltipBuilder;
import mcjty.lib.items.BaseItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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

public class ResistancePillsItem extends BaseItem {

    private static final TooltipBuilder TOOLTIP = new TooltipBuilder()
            .info(
                    TooltipBuilder.header(),
                    TooltipBuilder.general("desc"),
                    TooltipBuilder.parameter("attribute", stack -> TooltipNameHelper.getAttributeName(Config.getResistancePillsAttribute().orElse(null))),
                    TooltipBuilder.parameter("amount", stack -> String.format(Locale.ROOT, "%.2f", Mth.clamp(Config.RESISTANCE_PILLS_AMOUNT.get(), 0.0, 1.0))),
                    TooltipBuilder.parameter("duration", stack -> formatDuration(Config.RESISTANCE_PILLS_DURATION_TICKS.get())),
                    TooltipBuilder.general("usage")
            );

    public ResistancePillsItem() {
        super(new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        Optional<ResourceLocation> attributeId = Config.getResistancePillsAttribute();
        double amount = Mth.clamp(Config.RESISTANCE_PILLS_AMOUNT.get(), 0.0, 1.0);
        int durationTicks = Math.max(Config.RESISTANCE_PILLS_DURATION_TICKS.get(), 0);
        int maxStacks = Math.max(Config.RESISTANCE_PILLS_MAX_STACKS.get(), 0);
        if (attributeId.isEmpty() || amount <= 0.0 || durationTicks <= 0) {
            return InteractionResultHolder.pass(stack);
        }

        ResourceLocation resolvedAttributeId = attributeId.get();
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(resolvedAttributeId);
        if (attribute == null) {
            return InteractionResultHolder.pass(stack);
        }

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return InteractionResultHolder.pass(stack);
        }

        long gameTime = level.getGameTime();
        boolean applied = PlayerDoseDispatcher.getPlayerDose(player).map(store -> {
            if (!store.addResistancePillEffect(resolvedAttributeId, amount, gameTime + durationTicks, maxStacks, gameTime)) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(Component.translatable("message.hazardous.resistance_pills.max_stacks"), true);
                }
                return false;
            }
            TimedAttributeEffects.syncPlayer(player, store, gameTime);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), Registration.PILLS_USE.get(), SoundSource.PLAYERS, 0.6f, 1.1f);
            return true;
        }).orElse(false);

        if (!applied) {
            return InteractionResultHolder.pass(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(this);
        if (id != null && TOOLTIP.isActive()) {
            TOOLTIP.makeTooltip(id, stack, tooltip, flag);
        }
    }

    private static String formatDuration(int ticks) {
        if (ticks <= 0) {
            return "disabled";
        }
        if (ticks % 20 != 0) {
            return String.format(Locale.ROOT, "%.1f s", ticks / 20.0);
        }
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0 && seconds > 0) {
            return minutes + " min " + seconds + " s";
        }
        if (minutes > 0) {
            return minutes + " min";
        }
        return totalSeconds + " s";
    }
}
