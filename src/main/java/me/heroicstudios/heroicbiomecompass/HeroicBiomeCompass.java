package me.heroicstudios.heroicbiomecompass;

import me.heroicstudios.heroicbiomecompass.commands.BiomeCompassCommand;
import me.heroicstudios.heroicbiomecompass.listeners.CompassListener;
import me.heroicstudios.heroicbiomecompass.managers.CompassManager;
import me.heroicstudios.heroicbiomecompass.managers.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeroicBiomeCompass extends JavaPlugin {
    private static HeroicBiomeCompass instance;
    private Economy economy;
    private ConfigManager configManager;
    private CompassManager compassManager;


    // Main


    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.compassManager = new CompassManager(this);

        if (!setupEconomy()) {
            getLogger().severe("Vault dependency not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(new CompassListener(this), this);
        if (getCommand("biomecompass") != null) {
            getCommand("biomecompass").setExecutor(new BiomeCompassCommand(this));
        } else {
            getLogger().severe("Command 'biomecompass' not found in plugin.yml!");
        }
        compassManager.registerCraftingRecipe();
        getLogger().info("HeroicBiomeCompass has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("HeroicBiomeCompass has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
         }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static HeroicBiomeCompass getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CompassManager getCompassManager() {
        return compassManager;
    }
}
