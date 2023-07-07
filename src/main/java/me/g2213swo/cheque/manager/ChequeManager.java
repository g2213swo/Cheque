package me.g2213swo.cheque.manager;

import me.g2213swo.cheque.Cheque;
import me.g2213swo.cheque.storage.ChequeStorage;
import me.g2213swo.cheque.storage.ChequeStorageType;
import me.g2213swo.cheque.utils.UtilString;
import me.xanium.gemseconomy.api.Account;
import me.xanium.gemseconomy.api.Currency;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    public @Nullable ItemStack write(String creatorName, Currency currency, double amount) {
        if (!currency.isPayable()) return null;

        List<String> format = new ArrayList<>();
        for (String baseLore : requireNonNull(chequeBaseItem.getItemMeta().getLore())) {
            format.add(baseLore
                    .replace("{value}", currency.format(amount))
                    .replace("{account}", creatorName)
            );
        }
        ItemStack ret = chequeBaseItem.clone();
        ItemMeta meta = ret.getItemMeta();
        meta.setLore(format);
        ChequeStorage storage = new ChequeStorage(creatorName, currency.getSymbolOrEmpty(), amount);
        meta.getPersistentDataContainer().set(ChequeStorage.key, ChequeStorageType.INSTANCE, storage);
        ret.setItemMeta(meta);
        return ret;
    }

    public boolean isValid(ItemStack itemstack) {
        ChequeStorage storage = ChequeStorage.read(itemstack);
        return storage != null && StringUtils.isNotBlank(storage.getCurrency()) && StringUtils.isNotBlank(storage.getIssuer());
    }

    public double getValue(ItemStack itemstack) {
        ChequeStorage storage = ChequeStorage.read(itemstack);
        return storage != null ? storage.getValue() : 0;
    }

    /**
     * @param itemStack - The cheque item
     *
     * @return Currency it represents
     */
    public @Nullable Currency getCurrency(ItemStack itemStack) {
        ChequeStorage storage = ChequeStorage.read(itemStack);
        return storage != null
                ? Cheque.gemsEconomyAPI.getCurrency(storage.getCurrency()) // Might be null if the currency is deleted from database
                : Cheque.gemsEconomyAPI.getDefaultCurrency(); // Should not be null as it was checked during plugin startup
    }

    public void redeemCheque(Player player, Account user, ItemStack item, ChequeStorage storage) {
        if (storage == null) return;
        if (storage.getValue() <= 0) return;
        if (storage.getCurrency() == null) return;
        if (storage.getIssuer() == null) return;

        Currency currency = Cheque.gemsEconomyAPI.getCurrency(storage.getCurrency());
        if (currency == null) return;

        user.deposit(currency, storage.getValue());
        player.sendMessage(UtilString.colorize(Cheque.instance.getConfig().getString("redeem-message")
                .replace("{value}", currency.format(storage.getValue()))
                .replace("{account}", storage.getIssuer())
        ));
        item.setAmount(item.getAmount() - 1);
    }
}