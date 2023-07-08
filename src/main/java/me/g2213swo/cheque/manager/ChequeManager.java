package me.g2213swo.cheque.manager;

import me.g2213swo.cheque.Cheque;
import me.g2213swo.cheque.storage.ChequeStorage;
import me.g2213swo.cheque.storage.ChequeStorageType;
import me.g2213swo.cheque.utils.UtilString;
import me.xanium.gemseconomy.api.Account;
import me.xanium.gemseconomy.api.Currency;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class ChequeManager {
    private final ItemStack chequeBaseItem;

    public ChequeManager(Cheque plugin) {
        ItemStack item = new ItemStack(Material.valueOf(plugin.getConfig().getString("cheque.material")), 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(UtilString.colorize(plugin.getConfig().getString("cheque.name")));
        meta.setLore(UtilString.colorize(plugin.getConfig().getStringList("cheque.lore")));
        item.setItemMeta(meta);
        chequeBaseItem = item;
    }

    public Optional<ItemStack> write(String creatorName, Currency currency, double amount) {
        if (!currency.isPayable()) return Optional.empty();

        List<String> format = new ArrayList<>();
        for (String baseLore : requireNonNull(chequeBaseItem.getItemMeta().getLore())) {
            format.add(baseLore
                    .replace("{value}", currency.format(amount))
                    .replace("{account}", creatorName)
            );
        }
        ItemStack itemStack = chequeBaseItem.clone();
        ItemMeta meta = itemStack.getItemMeta();
        meta.setLore(format);
        ChequeStorage storage = new ChequeStorage(creatorName, currency.getName(), amount);
        meta.getPersistentDataContainer().set(ChequeStorage.key, ChequeStorageType.INSTANCE, storage);
        itemStack.setItemMeta(meta);
        return Optional.of(itemStack);
    }

    public boolean isValid(ItemStack itemStack) {
        return getChequeStorage(itemStack)
                .map(storage -> StringUtils.isNotBlank(storage.getCurrency()) && StringUtils.isNotBlank(storage.getIssuer()))
                .orElse(false);
    }

    public double getValue(ItemStack itemStack) {
        return getChequeStorage(itemStack)
                .map(ChequeStorage::getValue)
                .orElse(0.0);
    }

    public Optional<Currency> getCurrency(ItemStack itemStack) {
        return getChequeStorage(itemStack)
                .map(storage -> Cheque.gemsEconomyAPI.getCurrency(storage.getCurrency()))
                .or(() -> Optional.ofNullable(Cheque.gemsEconomyAPI.getDefaultCurrency()));
    }

    public void redeemCheque(Player player, Account user, ItemStack item, ChequeStorage storage) {
        if (storage == null || storage.getValue() <= 0 || storage.getCurrency() == null || storage.getIssuer() == null) {
            return;
        }

        Currency currency = Cheque.gemsEconomyAPI.getCurrency(storage.getCurrency());
        if (currency == null) {
            return;
        }

        user.deposit(currency, storage.getValue());
        Component message = Component.text(currency.simpleFormat(storage.getValue()), NamedTextColor.GOLD)
                .append(Component.text(" has been deposited to your account ", NamedTextColor.WHITE))
                .append(Component.text(storage.getIssuer(), NamedTextColor.GREEN));
        player.sendMessage(Cheque.PREFIX.append(message));
        item.setAmount(item.getAmount() - 1);
    }

    public Optional<ChequeStorage> getChequeStorage(ItemStack itemStack) {
        return Optional.ofNullable(ChequeStorage.read(itemStack));
    }
}
