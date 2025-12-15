package me.heroicstudios.heroicbiomecompass.listeners;

import me.heroicstudios.heroicbiomecompass.HeroicBiomeCompass;
import me.heroicstudios.heroicbiomecompass.gui.BiomeFinderGUI;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CompassListener implements Listener {
    private final HeroicBiomeCompass plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final BiomeFinderGUI guiManager;

    public CompassListener(HeroicBiomeCompass plugin) {
        this.plugin = plugin;
        this.guiManager = new BiomeFinderGUI(plugin);
    }

    @EventHandler
    public void onCompassRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (plugin.getCompassManager().isBiomeCompass(item)) {
            event.setCancelled(true);
            guiManager.openInventory(player, 1); // Open first page
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains(plugin.getConfigManager().getMenuTitle())) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();

        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Check if this is a navigation item
        Integer newPage = container.get(guiManager.getPageKey(), PersistentDataType.INTEGER);
        if (newPage != null) {
            guiManager.openInventory(player, newPage);
            return;
        }

        // Handle biome selection
        String biomeName = container.get(guiManager.getBiomeKey(), PersistentDataType.STRING);
        if (biomeName == null) return;

        handleBiomeSelection(player, biomeName);
    }

    private void handleBiomeSelection(Player player, String biomeName) {
        // Check permission for specific biome
        if (!player.hasPermission("biomecompass.biome." + biomeName.toLowerCase())) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-biome-permission"));
            return;
        }

        // Check cooldown
        if (isOnCooldown(player)) {
            long timeLeft = (cooldowns.get(player.getUniqueId()) +
                    (plugin.getConfigManager().getSearchCooldown() * 1000) -
                    System.currentTimeMillis()) / 1000;
            player.sendMessage(plugin.getConfigManager().getMessage("search-cooldown")
                    .replace("{time}", String.valueOf(timeLeft)));
            return;
        }

        double price = plugin.getConfigManager().getBiomePrice(biomeName);

        if (plugin.getEconomy().getBalance(player) < price) {
            player.sendMessage(plugin.getConfigManager().getMessage("insufficient-funds")
                    .replace("{amount}", String.valueOf(price)));
            return;
        }

        try {
            Biome targetBiome = Biome.valueOf(biomeName);
            plugin.getEconomy().withdrawPlayer(player, price);
            setCooldown(player);
            locateBiome(player, targetBiome);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid biome: " + biomeName);
        }
    }

    private boolean isOnCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        return System.currentTimeMillis() - cooldowns.get(player.getUniqueId()) <
                (plugin.getConfigManager().getSearchCooldown() * 1000);
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void locateBiome(Player player, Biome targetBiome) {
        player.sendMessage(plugin.getConfigManager().getMessage("search-in-progress")
                .replace("{biome}", targetBiome.name()));

        Location playerLoc = player.getLocation();
        int maxRadius = plugin.getConfigManager().getMaxSearchRadius();
        int step = plugin.getConfigManager().getSearchStep();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Location found = searchBiome(playerLoc, targetBiome, maxRadius, step);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (found != null) {
                    handleBiomeFound(player, targetBiome, found);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("search-failed")
                            .replace("{biome}", targetBiome.name())
                            .replace("{radius}", String.valueOf(maxRadius)));
                }
            });
        });
    }

    private Location searchBiome(Location center, Biome targetBiome, int maxRadius, int step) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        for (int r = 0; r <= maxRadius; r += step) {
            for (int x = -r; x <= r; x += step) {
                for (int z = -r; z <= r; z += step) {
                    if (Math.abs(x) != r && Math.abs(z) != r) continue;

                    int checkX = centerX + x;
                    int checkZ = centerZ + z;

                    if (world.getBiome(checkX, 0, checkZ) == targetBiome) {
                        return new Location(world, checkX, 64, checkZ);
                    }
                }
            }
        }
        return null;
    }

    private void handleBiomeFound(Player player, Biome biome, Location location) {
        player.sendMessage(plugin.getConfigManager().getMessage("biome-found")
                .replace("{biome}", biome.name())
                .replace("{x}", String.valueOf(location.getBlockX()))
                .replace("{z}", String.valueOf(location.getBlockZ())));

        if (plugin.getConfigManager().shouldPointCompass()) {
            player.setCompassTarget(location);
            player.sendMessage(plugin.getConfigManager().getMessage("compass-pointing")
                    .replace("{biome}", biome.name()));
        }

        if (plugin.getConfigManager().useParticleEffects()) {
            player.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0),
                    plugin.getConfigManager().getParticleCount(), 0.5, 0.5, 0.5, 0.1);
        }

        if (plugin.getConfigManager().useSoundEffects()) {
            player.playSound(player.getLocation(),
                    plugin.getConfigManager().getSearchSound(), 1.0f, 1.0f);
        }
    }
}
