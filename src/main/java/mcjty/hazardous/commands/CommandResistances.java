package mcjty.hazardous.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.hazardous.setup.HazardAttributes;
import mcjty.lib.varia.Tools;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CommandResistances implements Command<CommandSourceStack> {

    private static final CommandResistances CMD = new CommandResistances();

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("resistances")
                .requires(cs -> cs.hasPermission(0))
                .executes(CMD);
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayerOrException();
        Level level = player.level();
        Registry<HazardType> types = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);

        source.sendSuccess(() -> Component.literal("Player resistance attributes:"), false);

        for (HazardType type : types) {
            ResourceLocation hazardTypeId = types.getKey(type);
            if (hazardTypeId == null) {
                continue;
            }

            ResourceLocation attributeId = HazardAttributes.resolveResistanceAttributeId(hazardTypeId, type);
            if (attributeId == null) {
                continue;
            }

            Attribute attribute = HazardAttributes.resolveResistanceAttribute(hazardTypeId, type);
            if (attribute == null) {
                source.sendSuccess(() -> Component.literal("- " + hazardTypeId + " -> " + attributeId + ": missing"), false);
                continue;
            }

            AttributeInstance instance = player.getAttribute(attribute);
            String valueText = instance == null
                    ? "not attached"
                    : String.format("%.4f (base %.4f)", instance.getValue(), instance.getBaseValue());
            source.sendSuccess(() -> Component.literal("- " + hazardTypeId + " -> " + attributeId + ": " + valueText), false);
        }

        return 0;
    }
}
