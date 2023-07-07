package me.g2213swo.cheque.command;

import me.g2213swo.cheque.Cheque;
import me.g2213swo.cheque.manager.ChequeManager;
import me.g2213swo.cheque.storage.ChequeStorage;
import me.g2213swo.cheque.storage.ChequeStorageType;
import me.xanium.gemseconomy.api.Account;
import me.xanium.gemseconomy.api.Currency;
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
                    if (strings.length == 3) {
                        Player player = (Player) commandSender;
                        Account user = Cheque.gemsEconomyAPI.getAccount(player.getUniqueId());
                        Currency currency = Cheque.gemsEconomyAPI.getCurrency(strings[1]);
                        double amount = Double.parseDouble(strings[2]);
                        if (currency != null) {
                            if (user.hasEnough(currency, amount)) {
                                makeCheque(player, user, amount, currency);
                            } else {
                                player.sendMessage("You do not have enough money to write this cheque!");
                            }
                        }
                    } else {
                        commandSender.sendMessage("Usage: /cheque write <currency> <amount>");
                    }
                    break;
                case "redeem":
                    if (strings.length == 1) {
                        Player player = (Player) commandSender;
                        Account user = Cheque.gemsEconomyAPI.getAccount(player.getUniqueId());
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (cheque.chequeManager.isValid(item)) {
                            Currency currency = cheque.chequeManager.getCurrency(item);
                            ChequeStorage storage = ChequeStorage.read(item);
                            if (storage != null) {
                                if (storage.getCurrency().equals(currency.getName())) {
                                    cheque.chequeManager.redeemCheque(player, user, item, storage);
                                } else {
                                    player.sendMessage("The amount on the cheque does not match the amount you entered!");
                                }
                            } else {
                                player.sendMessage("The currency on the cheque does not match the currency you entered!");
                            }
                        } else {
                            player.sendMessage("You must be holding a valid cheque!");
                        }
                    } else {
                        commandSender.sendMessage("Usage: /cheque redeem");
                    }

                    break;
                default:
                    commandSender.sendMessage("Usage: /cheque <write> <currency> <amount>");
                    commandSender.sendMessage("Usage: /cheque <redeem>");
                    break;
            }
        }
        return true;
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
        // Check for null parameters
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        if (user.hasEnough(currency, amount)) {
            ItemStack cheque = Cheque.instance.getChequeManager().write(player.getName(), currency, amount);
            if (cheque != null) {
                user.withdraw(currency, amount);
                player.getInventory().addItem(cheque);
                player.sendMessage("msg_cheque_written");
            } else {
                player.sendMessage("err_cheque_written");
            }
        } else {
            player.sendMessage(("err_insufficient_funds"));
        }
    }
}