package net.blacksmithzstudios.wasteland.legendary;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.blacksmithzstudios.wasteland.WastelandMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

/**
 * Testing aids:
 *   /mutate &lt;targets&gt;                      - fire the mutation immediately
 *   /mutate legendary &lt;targets&gt; [prefix]   - promote mobs, random roll if no prefix given
 *   /mutate elite &lt;targets&gt; [rank]         - promote mobs to a higher-level elite
 *   /mutate clear &lt;targets&gt;                - strip every trace back off
 */
@EventBusSubscriber(modid = WastelandMod.MOD_ID)
public final class WastelandCommand {

    private WastelandCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mutate")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("legendary")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> makeLegendary(context, null))
                                .then(Commands.argument("prefix", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(LegendaryPrefix.values())
                                                        .map(prefix -> prefix.name().toLowerCase(Locale.ROOT)),
                                                builder))
                                        .executes(context -> makeLegendary(context,
                                                StringArgumentType.getString(context, "prefix"))))))
                .then(Commands.literal("elite")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(context -> makeElite(context, null))
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(EliteRank.values())
                                                        .map(rank -> rank.name().toLowerCase(Locale.ROOT)),
                                                builder))
                                        .executes(context -> makeElite(context,
                                                StringArgumentType.getString(context, "rank"))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(WastelandCommand::clear)))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(WastelandCommand::mutate)));
    }

    private static int makeLegendary(CommandContext<CommandSourceStack> context, String prefixName) {
        LegendaryPrefix requested = null;
        if (prefixName != null) {
            requested = LegendaryPrefix.byName(prefixName.toUpperCase(Locale.ROOT));
            if (requested == null) {
                context.getSource().sendFailure(Component.literal("Unknown prefix: " + prefixName));
                return 0;
            }
        }

        int affected = 0;
        for (LivingEntity entity : livingTargets(context)) {
            LegendaryPrefix prefix = requested != null ? requested : LegendaryPrefix.roll(entity.getRandom());
            LegendaryHandler.makeLegendary(entity, prefix);
            affected++;
        }
        return report(context, affected, "promoted");
    }

    private static int makeElite(CommandContext<CommandSourceStack> context, String rankName) {
        EliteRank requested = null;
        if (rankName != null) {
            requested = EliteRank.byName(rankName.toUpperCase(Locale.ROOT));
            if (requested == null) {
                context.getSource().sendFailure(Component.literal("Unknown rank: " + rankName));
                return 0;
            }
        }

        int affected = 0;
        for (LivingEntity entity : livingTargets(context)) {
            LegendaryHandler.makeElite(entity, requested != null ? requested : EliteRank.roll(entity.getRandom()));
            affected++;
        }
        return report(context, affected, "promoted");
    }

    private static int mutate(CommandContext<CommandSourceStack> context) {
        int affected = 0;
        for (LivingEntity entity : livingTargets(context)) {
            if (!LegendaryData.isLegendary(entity)) {
                LegendaryHandler.makeLegendary(entity, LegendaryPrefix.roll(entity.getRandom()));
            }
            LegendaryHandler.mutate(entity);
            affected++;
        }
        return report(context, affected, "mutated");
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        int affected = 0;
        for (LivingEntity entity : livingTargets(context)) {
            LegendaryData.clear(entity);
            affected++;
        }
        return report(context, affected, "cleared");
    }

    private static Collection<LivingEntity> livingTargets(CommandContext<CommandSourceStack> context) {
        try {
            return EntityArgument.getEntities(context, "targets").stream()
                    .filter(LivingEntity.class::isInstance)
                    .map(LivingEntity.class::cast)
                    // Never a player: promoting one would rename them and stack permanent
                    // attribute modifiers onto their profile.
                    .filter(entity -> !(entity instanceof net.minecraft.world.entity.player.Player))
                    .toList();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException noMatch) {
            return java.util.List.of();
        }
    }

    private static int report(CommandContext<CommandSourceStack> context, int affected, String verb) {
        context.getSource().sendSuccess(
                () -> Component.literal(verb + " " + affected + " entit" + (affected == 1 ? "y" : "ies")), true);
        return affected;
    }
}
