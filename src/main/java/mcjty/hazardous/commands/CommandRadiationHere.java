package mcjty.hazardous.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.data.HazardManager;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.lib.varia.Tools;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CommandRadiationHere implements Command<CommandSourceStack> {

    private static final CommandRadiationHere CMD = new CommandRadiationHere();

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("radiationhere")
                .requires(cs -> cs.hasPermission(0))
                .executes(CMD);
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayerOrException();
        Level level = player.level();
        Registry<HazardType> types = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);

        source.sendSuccess(() -> Component.literal("Hazard radiation at your position:"), false);

        for (HazardType type : types) {
            ResourceLocation id = types.getKey(type);
            if (id == null) continue;
            double value = HazardManager.getHazardValue(type, level, player);
            source.sendSuccess(() -> Component.literal("- " + id + ": " + String.format("%.4f", value)), false);
        }
        return 0;
    }
}
