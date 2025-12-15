package me.heroicstudios.heroicbiomecompass.managers;

import me.heroicstudios.heroicbiomecompass.HeroicBiomeCompass;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class ConfigManager {
    private final HeroicBiomeCompass plugin;
    private FileConfiguration config;
    private Map<String, BiomeMenuData> biomeMenuData;
    private Map<Integer, List<String>> pageContents;

    public ConfigManager(HeroicBiomeCompass plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.pageContents = new HashMap<>();
        loadBiomeMenuData();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        loadBiomeMenuData();
    }

    private void loadBiomeMenuData() {
        biomeMenuData = new HashMap<>();
        pageContents = new HashMap<>();
        ConfigurationSection biomesSection = config.getConfigurationSection("biomes");
        if (biomesSection == null) return;

        for (String biomeName : biomesSection.getKeys(false)) {
            ConfigurationSection biomeSection = biomesSection.getConfigurationSection(biomeName);
            if (biomeSection == null) continue;

            double price = biomeSection.getDouble("price", 0.0);
            String material = biomeSection.getString("material", "COMPASS");
            String displayName = biomeSection.getString("name", biomeName);
            List<String> lore = biomeSection.getStringList("lore");
            int slot = biomeSection.getInt("slot", -1);
            int page = biomeSection.getInt("page", 1);
            String permission = biomeSection.getString("permission", null); // new field

            List<String> formattedLore = new ArrayList<>();
            for (String line : lore) {
                formattedLore.add(ChatColor.translateAlternateColorCodes('&',
                        line.replace("{price}", String.valueOf(price))));
            }

            Material mat = Material.matchMaterial(material);
            if (mat == null) {
                plugin.getLogger().warning("Invalid material for biome " + biomeName + ": " + material);
                mat = Material.COMPASS;
            }

            BiomeMenuData menuData = new BiomeMenuData(
                    price,
                    mat,
                    ChatColor.translateAlternateColorCodes('&', displayName),
                    formattedLore,
                    slot,
                    page,
                    permission
            );

            biomeMenuData.put(biomeName, menuData);
            pageContents.computeIfAbsent(page, k -> new ArrayList<>()).add(biomeName);
        }
    }

    public BiomeMenuData getBiomeMenuData(String biomeName) {
        return biomeMenuData.get(biomeName);
    }

    public List<String> getBiomesForPage(int page) {
        return pageContents.getOrDefault(page, new ArrayList<>());
    }

    public int getTotalPages() {
        return pageContents.isEmpty() ? 1 : Collections.max(pageContents.keySet());
    }

    public Set<String> getConfiguredBiomes() {
        return biomeMenuData.keySet();
    }

    public String getCompassName() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("compass.name", "&6Biome Compass"));
    }

    public List<String> getCompassLore() {
        return config.getStringList("compass.lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    public String getMessage(String path) {
        String prefix = ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.prefix", "&8[&6BiomeCompass&8] &7"));
        String message = config.getString("messages." + path, "Message not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    public String getMenuTitle() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.title", "&8Biome Finder"));
    }

    public int getSearchCooldown() {
        return config.getInt("search.cooldown", 60);
    }

    public int getMaxSearchRadius() {
        return config.getInt("search.max-radius", 5000);
    }

    public int getSearchStep() {
        return config.getInt("search.search-step", 16);
    }

    public boolean shouldPointCompass() {
        return config.getBoolean("search.point-compass", true);
    }

    public boolean useParticleEffects() {
        return config.getBoolean("search.effects.particles", true);
    }

    public boolean useSoundEffects() {
        return config.getBoolean("search.effects.sounds", true);
    }

    public int getParticleCount() {
        return config.getInt("search.effects.particle-count", 20);
    }

    public Sound getSearchSound() {
        try {
            return Sound.valueOf(config.getString("search.effects.sound-type", "BLOCK_NOTE_BLOCK_PLING"));
        } catch (IllegalArgumentException e) {
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }

    public double getBiomePrice(String biomeName) {
        BiomeMenuData data = getBiomeMenuData(biomeName);
        return data != null ? data.getPrice() : -1;
    }

    public ConfigurationSection getGuiConfig() {
        return config.getConfigurationSection("gui");
    }

    public static class BiomeMenuData {
        private final double price;
        private final Material material;
        private final String displayName;
        private final List<String> lore;
        private final int slot;
        private final int page;
        private final String permission; // new

        public BiomeMenuData(double price, Material material, String displayName, List<String> lore, int slot, int page, String permission) {
            this.price = price;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
            this.slot = slot;
            this.page = page;
            this.permission = permission;
        }

        public double getPrice() {
            return price;
        }

        public Material getMaterial() {
            return material;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getLore() {
            return lore;
        }

        public int getSlot() {
            return slot;
        }

        public int getPage() {
            return page;
        }

        public String getPermission() {
            return permission;
        }
    }
}
