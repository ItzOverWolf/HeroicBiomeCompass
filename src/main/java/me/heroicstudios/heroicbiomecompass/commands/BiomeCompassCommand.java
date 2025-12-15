package me.heroicstudios.heroicbiomecompass.commands;

import me.heroicstudios.heroicbiomecompass.HeroicBiomeCompass;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BiomeCompassCommand implements CommandExecutor {
    private final HeroicBiomeCompass plugin;

    public BiomeCompassCommand(HeroicBiomeCompass plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("biomecompass.reload")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            try {
                plugin.reloadConfig();
                plugin.getConfigManager().reload();

                sender.sendMessage(
                        plugin.getConfigManager().getMessage("reload-success")
                );
            } catch (Exception e) {
                sender.sendMessage(
                        plugin.getConfigManager().getMessage("reload-failed")
                );
                e.printStackTrace();
            }

            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("biomecompass.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        player.getInventory().addItem(plugin.getCompassManager().createBiomeCompass());
        player.sendMessage(plugin.getConfigManager().getMessage("compass-given"));

        return true;
    }
}
