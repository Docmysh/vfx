package Vfx.vfx.command;

import Vfx.vfx.Vfx;
import Vfx.vfx.domain.DomainOfShadowsManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Vfx.MODID)
public class DarknessCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("darknessbeam")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("cooldown")
                                .executes(DarknessCommands::showCooldownReduction)
                                .then(Commands.argument("reduction_ticks", IntegerArgumentType.integer(0))
                                        .executes(context -> setCooldownReduction(
                                                context,
                                                IntegerArgumentType.getInteger(context, "reduction_ticks")
                                        ))))
        );
    }

    private static int showCooldownReduction(CommandContext<CommandSourceStack> context) {
        int currentReduction = DomainOfShadowsManager.getCooldownReductionTicks();
        context.getSource().sendSuccess(
                () -> Component.literal("Current darkness beam cooldown reduction: " + currentReduction + " ticks."),
                false
        );
        return currentReduction;
    }

    private static int setCooldownReduction(CommandContext<CommandSourceStack> context, int reduction) {
        DomainOfShadowsManager.setCooldownReductionTicks(reduction);
        int updated = DomainOfShadowsManager.getCooldownReductionTicks();
        context.getSource().sendSuccess(
                () -> Component.literal("Set darkness beam cooldown reduction to " + updated + " ticks."),
                true
        );
        return updated;
    }
}
