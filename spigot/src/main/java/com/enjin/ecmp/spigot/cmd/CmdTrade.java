package com.enjin.ecmp.spigot.cmd;

import com.enjin.ecmp.spigot.Messages;
import com.enjin.ecmp.spigot.SpigotBootstrap;
import com.enjin.ecmp.spigot.cmd.arg.PlayerArgument;
import com.enjin.ecmp.spigot.enums.Permission;
import com.enjin.ecmp.spigot.player.EnjPlayer;
import com.enjin.ecmp.spigot.player.PlayerManager;
import com.enjin.ecmp.spigot.trade.TradeManager;
import com.enjin.ecmp.spigot.util.MessageUtils;
import com.enjin.enjincoin.sdk.graphql.GraphQLResponse;
import com.enjin.enjincoin.sdk.model.service.identities.GetIdentities;
import com.enjin.enjincoin.sdk.model.service.identities.Identity;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public class CmdTrade extends EnjCommand {

    public CmdTrade(SpigotBootstrap bootstrap) {
        super(bootstrap);
        this.aliases.add("trade");
        this.requirements = CommandRequirements.builder()
                .withAllowedSenderTypes(SenderType.PLAYER)
                .withPermission(Permission.CMD_TRADE)
                .build();
        this.addSubCommand(new CmdInvite());
        this.addSubCommand(new CmdAccept());
        this.addSubCommand(new CmdDecline());
    }

    @Override
    public void execute(CommandContext context) {
        // Show Usage
    }

    public class CmdInvite extends EnjCommand {

        public CmdInvite() {
            super(CmdTrade.this.bootstrap);
            this.aliases.add("invite");
            this.requirements.arguments.add(PlayerArgument.REQUIRED);
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() == 0) return;

            EnjPlayer senderEnjPlayer = context.enjPlayer;

            if (!senderEnjPlayer.isLinked()) {
                Messages.identityNotLinked(context.sender);
                return;
            }

            Player sender = context.player;
            Optional<Player> target = PlayerArgument.REQUIRED.parse(context, context.args);

            if (!target.isPresent() || !target.get().isOnline()) {
                MessageUtils.sendString(sender, String.format("&6%s &cis not online.", context.args.get(0)));
                return;
            }

            if (sender == target.get()) {
                MessageUtils.sendString(sender, "&cYou must specify a player other than yourself.");
                return;
            }

            EnjPlayer targetEnjPlayer = bootstrap.getPlayerManager().getPlayer(target.get()).orElse(null);

            if (!targetEnjPlayer.isLinked()) {
                MessageUtils.sendString(sender, String.format("&6%s &chas not linked a wallet and cannot trade.", targetEnjPlayer.getBukkitPlayer().getName()));
                MessageUtils.sendString(targetEnjPlayer.getBukkitPlayer(), String.format("&6%s &awants to trade with you.", sender.getName()));
                Messages.linkInstructions(targetEnjPlayer.getBukkitPlayer());
                return;
            }

            if (BigInteger.ZERO.equals(senderEnjPlayer.getEnjAllowance())) {
                Messages.allowanceNotSet(sender);
                return;
            }

            invite(senderEnjPlayer, targetEnjPlayer);
        }

        private void invite(EnjPlayer sender, EnjPlayer target) {
            boolean result = bootstrap.getTradeManager().addInvite(sender, target);

            if (!result) {
                MessageUtils.sendString(sender.getBukkitPlayer(),
                        String.format("You have already invited &6%s &cto trade.", target.getBukkitPlayer().getName()));
                return;
            }

            MessageUtils.sendString(sender.getBukkitPlayer(),
                    String.format("&aTrade invite sent to &6%s!", target.getBukkitPlayer().getName()));

            TextComponent.Builder inviteMessageBuilder = TextComponent.builder("")
                    .color(TextColor.GRAY)
                    .append(TextComponent.builder(sender.getBukkitPlayer().getName())
                            .color(TextColor.GOLD)
                            .build())
                    .append(TextComponent.of(" has invited you to trade. "))
                    .append(TextComponent.builder("Accept")
                            .color(TextColor.GREEN)
                            .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/enj trade accept %s", sender.getBukkitPlayer().getName())))
                            .build())
                    .append(TextComponent.of(" | "))
                    .append(TextComponent.builder("Decline")
                            .color(TextColor.RED)
                            .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/enj trade decline %s", sender.getBukkitPlayer().getName())))
                            .build());
            MessageUtils.sendComponent(target.getBukkitPlayer(), inviteMessageBuilder.build());
        }

    }

    public class CmdAccept extends EnjCommand {

        public CmdAccept() {
            super(CmdTrade.this.bootstrap);
            this.aliases.add("accept");
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() == 0) return;

            Player sender = context.player;
            Player target = Bukkit.getPlayer(context.args.get(0));

            if (target == null) {
                MessageUtils.sendString(sender, String.format("&6%s &cis not online.", context.args.get(0)));
                return;
            }

            if (target == sender) {
                MessageUtils.sendString(sender, "&cYou must specify a player other than yourself.");
                return;
            }

            EnjPlayer senderEnjPlayer = bootstrap.getPlayerManager().getPlayer(target).orElse(null);
            EnjPlayer targetEnjPlayer = context.enjPlayer;

            boolean result = bootstrap.getTradeManager().acceptInvite(senderEnjPlayer, targetEnjPlayer);
            if (!result) {
                MessageUtils.sendString(sender, String.format("&cNo open trade invites from &6%s.", target.getName()));
            }
        }

    }

    public class CmdDecline extends EnjCommand {

        public CmdDecline() {
            super(CmdTrade.this.bootstrap);
            this.aliases.add("decline");
        }

        @Override
        public void execute(CommandContext context) {
            if (context.args.size() == 0) return;

            Player sender = context.player;
            Player target = Bukkit.getPlayer(context.args.get(0));

            if (target == null) {
                MessageUtils.sendString(sender, String.format("&6%s &cis not online.", context.args.get(0)));
                return;
            }

            if (target == sender) {
                MessageUtils.sendString(sender, "&cYou must specify a player other than yourself.");
                return;
            }

            EnjPlayer senderEnjPlayer = bootstrap.getPlayerManager().getPlayer(target).orElse(null);
            EnjPlayer targetEnjPlayer = context.enjPlayer;

            boolean result = bootstrap.getTradeManager().declineInvite(senderEnjPlayer, targetEnjPlayer);
            if (result) {
                MessageUtils.sendString(sender, String.format("&aYou have declined &6%s's &atrade invite.", target.getName()));
                MessageUtils.sendString(target, String.format("&6%s &chas declined your trade invite.", sender.getName()));
            } else {
                MessageUtils.sendString(sender, String.format("&cNo open trade invites from &6%s.", target.getName()));
            }
        }

    }

}