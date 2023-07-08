package me.g2213swo.cheque.command;

import me.g2213swo.cheque.Cheque;
import me.g2213swo.cheque.storage.ChequeStorage;
import me.xanium.gemseconomy.api.Account;
import me.xanium.gemseconomy.api.Currency;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ChequeCommand implements TabExecutor {

    private final Cheque cheque = Cheque.instance;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof Player) {
            switch (strings[0]) {
                case "write":
                    handleWriteCommand(commandSender, strings);
                    break;
                case "redeem":
                    handleRedeemCommand(commandSender, strings);
                    break;
                default:
                    commandSender.sendMessage(Cheque.PREFIX.append(Component.text(" Usage: /cheque <write> <currency> <amount>", NamedTextColor.WHITE)));
                    commandSender.sendMessage(Cheque.PREFIX.append(Component.text(" Usage: /cheque <redeem>", NamedTextColor.WHITE)));
                    break;
            }
        }
        return true;
    }

    private void handleWriteCommand(CommandSender commandSender, String[] strings) {
        if (strings.length == 3) {
            Player player = (Player) commandSender;
            Account user = Cheque.gemsEconomyAPI.getAccount(player.getUniqueId());
            Currency currency = Cheque.gemsEconomyAPI.getCurrency(strings[1]);
            double amount = Double.parseDouble(strings[2]);
            if (currency != null && user.hasEnough(currency, amount)) {
                makeCheque(player, user, amount, currency);
            } else {
                player.sendMessage("You do not have enough money to write this cheque!");
            }
        } else {
            commandSender.sendMessage("Usage: /cheque write <currency> <amount>");
        }
    }

    private void handleRedeemCommand(CommandSender commandSender, String[] strings) {
        if (strings.length == 1) {
            Player player = (Player) commandSender;
            Account user = Cheque.gemsEconomyAPI.getAccount(player.getUniqueId());
            ItemStack item = player.getInventory().getItemInMainHand();
            if (cheque.chequeManager.isValid(item)) {
                cheque.chequeManager.getCurrency(item).ifPresent(currency -> {
                    ChequeStorage storage = cheque.chequeManager.getChequeStorage(item).orElse(null);
                    if (storage != null && storage.getCurrency().equals(currency.getName())) {
                        cheque.chequeManager.redeemCheque(player, user, item, storage);
                    }
                });
            } else {
                player.sendMessage(Cheque.PREFIX.append(Component.text(" This is not a valid cheque!", NamedTextColor.WHITE)));
            }
        } else {
            commandSender.sendMessage("Usage: /cheque redeem");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        switch (strings.length) {
            case 1:
                return List.of("write", "redeem");
            case 2:
                if (strings[0].equals("write")) {
                    return Cheque.gemsEconomyAPI.getLoadedCurrencies().stream().map(Currency::getName).toList();
                }
            case 3:
                if (strings[0].equals("write")) {
                    return List.of("10", "100", "1000");
                }
        }
        return null;
    }

    private void makeCheque(Player player, Account user, double amount, Currency currency) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        if (user.hasEnough(currency, amount)) {
            cheque.chequeManager.write(player.getName(), currency, amount).ifPresent(cheque -> {
                user.withdraw(currency, amount);
                player.getInventory().addItem(cheque);
                player.sendMessage(Cheque.PREFIX.append(Component.text("msg_cheque_written", NamedTextColor.GREEN)));
            });
        } else {
            player.sendMessage(Cheque.PREFIX.append(Component.text(" err_insufficient_funds", NamedTextColor.RED)));
        }
    }
}
