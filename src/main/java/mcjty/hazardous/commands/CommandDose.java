package mcjty.hazardous.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.data.PlayerDoseData;
import mcjty.hazardous.data.PlayerDoseDispatcher;
import mcjty.hazardous.data.objects.HazardType;
import mcjty.lib.varia.Tools;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class CommandDose implements Command<CommandSourceStack> {

    private static final CommandDose CMD = new CommandDose();

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher) {
        return Commands.literal("dose")
                .requires(cs -> cs.hasPermission(0))
                .executes(CMD);
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayerOrException();
        Level level = player.level();
        Registry<HazardType> types = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);

        source.sendSuccess(() -> Component.literal("Accumulated hazard dose for you:"), false);

        var opt = PlayerDoseDispatcher.getPlayerDose(player);
        if (opt.isPresent()) {
            opt.ifPresent(store -> {
                for (HazardType type : types) {
                    ResourceLocation id = types.getKey(type);
                    if (id != null) {
                        double value = store.getDose(id);
                        source.sendSuccess(() -> Component.literal("- " + id + ": " + String.format("%.4f", value)), false);
                    }
                }
            });
        } else {
            source.sendFailure(Component.literal("No dose data available"));
        }

        return 0;
    }
}
