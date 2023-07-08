package me.g2213swo.cheque;

import me.g2213swo.cheque.command.ChequeCommand;
import me.g2213swo.cheque.manager.ChequeManager;
import me.xanium.gemseconomy.api.GemsEconomy;
import me.xanium.gemseconomy.api.GemsEconomyProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class Cheque extends JavaPlugin {

    public static Cheque instance;

    public static final Component PREFIX = Component.text("[", NamedTextColor.GRAY)
            .append(Component.text("Cheque", NamedTextColor.YELLOW))
            .append(Component.text("]", NamedTextColor.GRAY));

    public ChequeManager chequeManager;
    public static GemsEconomy gemsEconomyAPI;

    @Override
    public void onLoad() {
        instance = this;
        gemsEconomyAPI = GemsEconomyProvider.get();
        chequeManager = new ChequeManager(instance);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        getCommand("cheque").setExecutor(new ChequeCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    public ChequeManager getChequeManager() {
        return chequeManager;
    }
}
