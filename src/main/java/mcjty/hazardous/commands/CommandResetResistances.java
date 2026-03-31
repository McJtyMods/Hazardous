package mcjty.hazardous.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.data.PlayerDoseData;
import mcjty.hazardous.data.PlayerDoseDispatcher;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.hazardous.setup.HazardAttributes;
import mcjty.hazardous.setup.ResistancePillEffects;
import mcjty.lib.varia.Tools;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Set;

public class CommandResetResistances {

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("resetresistances")
                .requires(cs -> cs.hasPermission(1))
                .executes(CommandResetResistances::runSelf)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(CommandResetResistances::runOther));
    }

    private static int runSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        resetResistances(context.getSource(), player);
        return 0;
    }

    private static int runOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        resetResistances(context.getSource(), player);
        return 0;
    }

    private static void resetResistances(CommandSourceStack source, ServerPlayer player) {
        Set<ResourceLocation> attributeIds = collectResistanceAttributeIds(player);

        PlayerDoseDispatcher.getPlayerDose(player).ifPresent(store -> {
            attributeIds.addAll(store.getResistancePillAttributeIds());
            resetResistanceState(player, store, attributeIds);
        });

        for (ResourceLocation attributeId : attributeIds) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
            if (attribute == null) {
                continue;
            }

            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }

            instance.setBaseValue(0.0);
            ResistancePillEffects.clearModifier(player, attributeId);
        }

        if (source.getEntity() == player) {
            source.sendSuccess(() -> Component.literal("Hazard resistances reset."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Hazard resistances reset for " + player.getGameProfile().getName() + "."), false);
        }
    }

    private static Set<ResourceLocation> collectResistanceAttributeIds(ServerPlayer player) {
        Registry<HazardType> types = Tools.getRegistryAccess(player.level()).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);
        Set<ResourceLocation> attributeIds = new LinkedHashSet<>();
        for (HazardType type : types) {
            ResourceLocation hazardTypeId = types.getKey(type);
            if (hazardTypeId == null) {
                continue;
            }

            ResourceLocation attributeId = HazardAttributes.resolveResistanceAttributeId(hazardTypeId, type);
            if (attributeId != null) {
                attributeIds.add(attributeId);
            }
        }
        return attributeIds;
    }

    private static void resetResistanceState(ServerPlayer player, PlayerDoseData store, Set<ResourceLocation> attributeIds) {
        for (ResourceLocation attributeId : attributeIds) {
            ResistancePillEffects.clearModifier(player, attributeId);
        }
        store.clearResistancePills();
    }
}
