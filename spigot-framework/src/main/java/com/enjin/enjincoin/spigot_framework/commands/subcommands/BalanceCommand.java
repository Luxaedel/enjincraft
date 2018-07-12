package com.enjin.enjincoin.spigot_framework.commands.subcommands;

import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.WalletInventory;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.enjin.enjincoin.spigot_framework.player.TokenData;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import com.google.gson.JsonObject;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

/**
 * <p>Balance command handler.</p>
 */
public class BalanceCommand {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>Balance command handler constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public BalanceCommand(BasePlugin main) {
        this.main = main;
    }

    /**
     * <p>Executes and performs operations defined for the command.</p>
     *
     * @param sender the command sender
     * @param args the command arguments
     *
     * @since 1.0
     */
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            MinecraftPlayer mcPlayer = this.main.getBootstrap().getPlayerManager().getPlayer(player.getUniqueId());
            // reload/refresh user info
            mcPlayer.reloadUser();

            Map<String, TokenData> balances = mcPlayer.getWallet().getTokenBalances();

            boolean showAll = false;
            Identity identity = mcPlayer.getIdentity();
            List<TokenData> tokens = mcPlayer.getWallet().getTokens();
            if (identity != null) {
                Double ethBalance = (mcPlayer.getIdentityData().getEthBalance() == null) ? 0 : mcPlayer.getIdentityData().getEthBalance();
                Double enjBalance = (mcPlayer.getIdentityData().getEnjBalance() == null) ? 0 : mcPlayer.getIdentityData().getEnjBalance();

                sendMsg(sender, "EthAdr: " + ChatColor.LIGHT_PURPLE + identity.getEthereumAddress());
                sendMsg(sender, "ID: " + identity.getId() + " -> " + ChatColor.GREEN + "[ " + enjBalance  + " ENJ ] [ " + ethBalance + " ETH ]");
                sendMsg(sender, "");
                sendMsg(sender,  ChatColor.BOLD + "" + ChatColor.GOLD + "Found " + identity.getTokens().size() + " items in Wallet: ");

                JsonObject tokensDisplayConfig = main.getBootstrap().getConfig().get("tokens").getAsJsonObject();
                for(int i = 0; i < identity.getTokens().size(); i++) {
                    JsonObject tokenDisplay = tokensDisplayConfig.has(String.valueOf(identity.getTokens().get(i).getTokenId()))
                            ? tokensDisplayConfig.get(String.valueOf(identity.getTokens().get(i).getTokenId())).getAsJsonObject()
                            : null;
                    String name = "";
                    if (tokenDisplay != null) {
                        if (tokenDisplay != null && tokenDisplay.has("displayName")) {
                            sendMsg(sender, ChatColor.GOLD + String.valueOf(i + 1) + ". " + ChatColor.DARK_PURPLE + tokenDisplay.get("displayName").getAsString() + ChatColor.GREEN + " (qty. " + identity.getTokens().get(i).getBalance() + ")");
                        }
                    }
                    if (showAll) {
                        sendMsg(sender, ChatColor.GRAY + String.valueOf(i + 1) + ". " + identity.getTokens().get(i).getName() + " (qty. " + identity.getTokens().get(i).getBalance() + ")");
                    }
                }
            } else {
                TextComponent text = TextComponent.of("You have not linked a wallet to your account.")
                        .color(TextColor.RED);
                MessageUtils.sendMessage(sender, text);
            }
        } else {
            TextComponent text = TextComponent.of("Only players can use this command.")
                .color(TextColor.RED);
            MessageUtils.sendMessage(sender, text);
        }
    }

    private void sendMsg(CommandSender sender, String msg) {
        TextComponent text = TextComponent.of(msg)
                .color(TextColor.GOLD);
        MessageUtils.sendMessage(sender, text);
    }

}