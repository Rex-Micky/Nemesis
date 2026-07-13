package dev.rexmicky.nemesis;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/** Owns all nemesis state: creation, config-driven traits, levelling, death and persistence. */
public class NemesisManager {

    private static final int INFINITE = Integer.MAX_VALUE;

    private final NemesisPlugin plugin;
    private final File file;
    private final NamespacedKey keyOwner;
    private final NamespacedKey keyLevel;
    private final NamespacedKey keyBase;
    private final NamespacedKey keyOrigHealth;
    private final NamespacedKey keyOrigSpeed;
    private final NamespacedKey keyOrigDamage;

    private final Map<UUID, NemesisData> byOwner = new LinkedHashMap<>();
    private Set<EntityType> eligible = new HashSet<>();

    public NemesisManager(NemesisPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "nemeses.yml");
        this.keyOwner = new NamespacedKey(plugin, "owner");
        this.keyLevel = new NamespacedKey(plugin, "level");
        this.keyBase = new NamespacedKey(plugin, "base");
        this.keyOrigHealth = new NamespacedKey(plugin, "orig_health");
        this.keyOrigSpeed = new NamespacedKey(plugin, "orig_speed");
        this.keyOrigDamage = new NamespacedKey(plugin, "orig_damage");
        reloadEligible();
    }

    public void reloadEligible() {
        Set<EntityType> set = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("eligible-mobs")) {
            try {
                set.add(EntityType.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown mob type in eligible-mobs: " + s);
            }
        }
        this.eligible = set;
    }

    // --- identity checks -----------------------------------------------------

    public UUID ownerOf(Entity entity) {
        if (entity == null) return null;
        String raw = entity.getPersistentDataContainer().get(keyOwner, PersistentDataType.STRING);
        if (raw == null) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException ex) { return null; }
    }

    public NemesisData getByOwner(UUID owner) { return byOwner.get(owner); }

    public boolean isEligible(EntityType type) { return eligible.contains(type); }

    public boolean rollChance() {
        return ThreadLocalRandom.current().nextInt(100) < plugin.getConfig().getInt("promote-chance", 100);
    }

    // --- lifecycle -----------------------------------------------------------

    public void createNemesis(Player victim, LivingEntity mob) {
        NemesisData data = new NemesisData(victim.getUniqueId(), victim.getName(), mob.getUniqueId(),
                mob.getType().name(), NameGenerator.randomName(), 1, System.currentTimeMillis());
        data.setLocation(mob.getLocation());
        byOwner.put(victim.getUniqueId(), data);
        applyTraits(mob, data);
        save();

        victim.sendTitle(ChatColor.DARK_RED + "☠ A NEMESIS RISES",
                ChatColor.RED + data.getBaseName() + " now hunts you", 10, 70, 20);
        victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.8f);
        plugin.send(victim, "rise", "name", displayName(data));
        if (plugin.getConfig().getBoolean("broadcast.on-rise", false)) {
            plugin.getServer().broadcastMessage(plugin.format("rise-broadcast",
                    "name", displayName(data), "owner", data.getOwnerName()));
        }
        plugin.getLogger().info("Nemesis born: " + ChatColor.stripColor(displayName(data))
                + " (" + data.getMobType() + ") vs " + data.getOwnerName());
    }

    public void levelUp(Player victim, LivingEntity mob) {
        NemesisData data = byOwner.get(victim.getUniqueId());
        if (data == null) return;
        int max = plugin.getConfig().getInt("max-level", 5);
        if (data.getLevel() < max) data.setLevel(data.getLevel() + 1);
        data.setEntityUUID(mob.getUniqueId());
        data.setLocation(mob.getLocation());
        applyTraits(mob, data);
        save();

        victim.sendTitle(ChatColor.DARK_RED + "YOUR NEMESIS GROWS STRONGER",
                ChatColor.RED + data.getBaseName() + " — Lv." + data.getLevel(), 10, 70, 20);
        victim.playSound(victim.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        plugin.send(victim, "levelup", "name", displayName(data), "level", data.getLevel());
        if (plugin.getConfig().getBoolean("broadcast.on-levelup", false)) {
            plugin.getServer().broadcastMessage(plugin.format("levelup-broadcast",
                    "name", displayName(data), "owner", data.getOwnerName()));
        }
        plugin.getLogger().info("Nemesis levelled up: " + ChatColor.stripColor(displayName(data))
                + " vs " + data.getOwnerName());
    }

    public void handleDeath(EntityDeathEvent event, NemesisData data) {
        Player killer = event.getEntity().getKiller();
        boolean revenge = killer != null && killer.getUniqueId().equals(data.getOwnerUUID());

        if (revenge) {
            applyRevengeLoot(event, data);
            plugin.send(killer, "revenge", "name", displayName(data));
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            if (plugin.getConfig().getBoolean("broadcast.on-revenge", true)) {
                plugin.getServer().broadcastMessage(plugin.format("revenge-broadcast",
                        "owner", data.getOwnerName(), "name", displayName(data)));
            }
            plugin.getLogger().info(data.getOwnerName() + " slew their nemesis "
                    + ChatColor.stripColor(displayName(data)));
        } else {
            Player owner = Bukkit.getPlayer(data.getOwnerUUID());
            if (owner != null) plugin.send(owner, "lost-other", "name", displayName(data));
        }
        byOwner.remove(data.getOwnerUUID());
        save();
    }

    private void applyRevengeLoot(EntityDeathEvent event, NemesisData data) {
        FileConfiguration c = plugin.getConfig();
        int lvl = data.getLevel();

        event.setDroppedExp(c.getInt("revenge.xp-base", 20) + lvl * c.getInt("revenge.xp-per-level", 15));

        for (Map<?, ?> drop : c.getMapList("revenge.drops")) {
            Material mat = Material.matchMaterial(String.valueOf(drop.get("material")));
            if (mat == null) continue;
            int amount = toInt(drop.get("amount"), 0) + lvl * toInt(drop.get("per-level"), 0);
            if (amount > 0) event.getDrops().add(new ItemStack(mat, amount));
        }

        if (c.getBoolean("revenge.trophy.enabled", true)) {
            Material mat = Material.matchMaterial(c.getString("revenge.trophy.material", "PLAYER_HEAD"));
            if (mat == null) mat = Material.PLAYER_HEAD;
            ItemStack trophy = new ItemStack(mat);
            ItemMeta meta = trophy.getItemMeta();
            if (meta != null) {
                String plainName = ChatColor.stripColor(displayName(data));
                meta.setDisplayName(color(c.getString("revenge.trophy.name", "&6Trophy of {name}")
                        .replace("{name}", plainName).replace("{owner}", data.getOwnerName())));
                List<String> lore = new ArrayList<>();
                for (String line : c.getStringList("revenge.trophy.lore")) {
                    lore.add(color(line.replace("{name}", plainName).replace("{owner}", data.getOwnerName())));
                }
                if (!lore.isEmpty()) meta.setLore(lore);
                trophy.setItemMeta(meta);
            }
            event.getDrops().add(trophy);
        }
    }

    // --- trait application (all config-driven) -------------------------------

    public void applyTraits(LivingEntity mob, NemesisData data) {
        int lvl = data.getLevel();
        FileConfiguration cfg = plugin.getConfig();

        mob.setCustomName(displayName(data));
        mob.setCustomNameVisible(true);
        mob.setRemoveWhenFarAway(false);
        mob.setPersistent(true);
        mob.setGlowing(cfg.getBoolean("glowing", true));

        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        pdc.set(keyOwner, PersistentDataType.STRING, data.getOwnerUUID().toString());
        pdc.set(keyLevel, PersistentDataType.INTEGER, lvl);
        pdc.set(keyBase, PersistentDataType.STRING, data.getBaseName());

        // Never let a baby mob become the nemesis — inherently too fast and too hard to see.
        if (mob instanceof Zombie zombie) zombie.setBaby(false);

        // Scale from the mob's OWN natural stats (captured once), never the attribute's
        // generic default, which differs for movement speed (0.7 vs a zombie's 0.23).
        scaleAttr(mob, Attribute.MAX_HEALTH, keyOrigHealth, 1.0 + cfg.getDouble("scaling.health.per-level", 0.6) * lvl);
        scaleAttr(mob, Attribute.ATTACK_DAMAGE, keyOrigDamage, 1.0 + cfg.getDouble("scaling.damage.per-level", 0.4) * lvl);
        scaleAttr(mob, Attribute.MOVEMENT_SPEED, keyOrigSpeed,
                Math.min(cfg.getDouble("scaling.speed.cap", 1.25), 1.0 + cfg.getDouble("scaling.speed.per-level", 0.06) * (lvl - 1)));
        AttributeInstance hp = mob.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) mob.setHealth(hp.getValue());

        applyLevelAbilities(mob, lvl);
    }

    /** Scale an attribute from the mob's own natural base value, captured once on promotion. */
    private void scaleAttr(LivingEntity e, Attribute a, NamespacedKey origKey, double factor) {
        AttributeInstance inst = e.getAttribute(a);
        if (inst == null) return; // e.g. skeletons have no attack-damage attribute
        Double orig = e.getPersistentDataContainer().get(origKey, PersistentDataType.DOUBLE);
        if (orig == null) {
            orig = inst.getBaseValue();
            e.getPersistentDataContainer().set(origKey, PersistentDataType.DOUBLE, orig);
        }
        inst.setBaseValue(orig * factor);
    }

    /** Potion effects, gear and knockback resistance for a level, read from config. */
    private void applyLevelAbilities(LivingEntity mob, int lvl) {
        for (PotionEffect pe : new ArrayList<>(mob.getActivePotionEffects())) mob.removePotionEffect(pe.getType());
        if (plugin.getConfig().getBoolean("fire-immune", true)) {
            mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, INFINITE, 0, false, false));
        }

        ConfigurationSection block = levelBlock(lvl);
        AttributeInstance kb = mob.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(block != null ? block.getDouble("knockback-resistance", 0.0) : 0.0);

        if (block != null) {
            for (String s : block.getStringList("effects")) {
                PotionEffect eff = parseEffect(s);
                if (eff != null) mob.addPotionEffect(eff);
            }
        }
        applyEquipment(mob, block == null ? null : block.getConfigurationSection("equipment"));
    }

    /** The 'levels.<n>' block for a level, falling back to the highest one defined at or below it. */
    private ConfigurationSection levelBlock(int level) {
        ConfigurationSection levels = plugin.getConfig().getConfigurationSection("levels");
        if (levels == null) return null;
        Set<String> keys = levels.getKeys(false);
        if (keys.contains(String.valueOf(level))) return levels.getConfigurationSection(String.valueOf(level));
        int best = -1;
        for (String k : keys) {
            try {
                int n = Integer.parseInt(k);
                if (n <= level && n > best) best = n;
            } catch (NumberFormatException ignored) {}
        }
        return best > 0 ? levels.getConfigurationSection(String.valueOf(best)) : null;
    }

    private PotionEffect parseEffect(String spec) {
        String[] parts = spec.split(":");
        PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase());
        if (type == null) {
            plugin.getLogger().warning("Unknown potion effect in config: " + spec);
            return null;
        }
        int amp = 0;
        if (parts.length > 1) {
            try { amp = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException ignored) {}
        }
        return new PotionEffect(type, INFINITE, amp, false, false);
    }

    private void applyEquipment(LivingEntity mob, ConfigurationSection equip) {
        EntityEquipment eq = mob.getEquipment();
        if (eq == null) return;
        // Clear managed slots first so lower levels don't retain higher-level gear.
        eq.setHelmet(null); eq.setChestplate(null); eq.setLeggings(null);
        eq.setBoots(null); eq.setItemInMainHand(null);
        if (equip != null) {
            setGear(eq, equip, "helmet");
            setGear(eq, equip, "chestplate");
            setGear(eq, equip, "leggings");
            setGear(eq, equip, "boots");
            setGear(eq, equip, "weapon");
        }
        eq.setHelmetDropChance(0f); eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f); eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
    }

    private void setGear(EntityEquipment eq, ConfigurationSection equip, String slot) {
        String matName = equip.getString(slot);
        if (matName == null) return;
        Material mat = Material.matchMaterial(matName);
        if (mat == null) {
            plugin.getLogger().warning("Unknown equipment material in config: " + matName);
            return;
        }
        ItemStack item = new ItemStack(mat);
        switch (slot) {
            case "helmet" -> eq.setHelmet(item);
            case "chestplate" -> eq.setChestplate(item);
            case "leggings" -> eq.setLeggings(item);
            case "boots" -> eq.setBoots(item);
            case "weapon" -> eq.setItemInMainHand(item);
        }
    }

    public String displayName(NemesisData d) {
        String title = NameGenerator.title(d.getBaseName(), d.getLevel(), d.getOwnerName());
        return color("&4&l" + d.getBaseName() + " " + title + " &7[Lv." + d.getLevel() + "]");
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o != null) {
            try { return Integer.parseInt(o.toString()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    // --- hunting task --------------------------------------------------------

    public void huntTick() {
        if (!plugin.getConfig().getBoolean("hunt.enabled", true)) return;
        double range = plugin.getConfig().getDouble("hunt.range", 40);
        double warnRange = plugin.getConfig().getDouble("hunt.warn-range", 18);
        boolean warn = plugin.getConfig().getBoolean("hunt.warn", true);

        for (NemesisData d : new ArrayList<>(byOwner.values())) {
            Entity e = Bukkit.getEntity(d.getEntityUUID());
            if (!(e instanceof LivingEntity le) || e.isDead()) continue;
            d.setLocation(le.getLocation());

            Player owner = Bukkit.getPlayer(d.getOwnerUUID());
            if (owner == null || !owner.getWorld().equals(le.getWorld())) continue;
            double dist = owner.getLocation().distance(le.getLocation());
            if (dist > range) continue;

            if (le instanceof Mob mob) mob.setTarget(owner);
            if (warn && dist <= warnRange) {
                owner.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(plugin.format("near-warn", "name", d.getBaseName())));
            }
        }
    }

    // --- persistence ---------------------------------------------------------

    public void load() {
        byOwner.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("nemeses");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            try {
                NemesisData d = new NemesisData(
                        UUID.fromString(key),
                        s.getString("ownerName", "Unknown"),
                        UUID.fromString(s.getString("entity")),
                        s.getString("mobType", "ZOMBIE"),
                        s.getString("baseName", "Nemesis"),
                        s.getInt("level", 1),
                        s.getLong("createdAt"));
                d.setLocationRaw(s.getString("world"), s.getDouble("x"), s.getDouble("y"), s.getDouble("z"));
                byOwner.put(d.getOwnerUUID(), d);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Skipping malformed nemesis '" + key + "'", ex);
            }
        }
        plugin.getLogger().info("Loaded " + byOwner.size() + " nemesis record(s).");
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (NemesisData d : byOwner.values()) {
            String p = "nemeses." + d.getOwnerUUID();
            yml.set(p + ".ownerName", d.getOwnerName());
            yml.set(p + ".entity", d.getEntityUUID().toString());
            yml.set(p + ".mobType", d.getMobType());
            yml.set(p + ".baseName", d.getBaseName());
            yml.set(p + ".level", d.getLevel());
            yml.set(p + ".createdAt", d.getCreatedAt());
            yml.set(p + ".world", d.getWorld());
            yml.set(p + ".x", d.getX());
            yml.set(p + ".y", d.getY());
            yml.set(p + ".z", d.getZ());
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save nemeses.yml", ex);
        }
    }

    public Collection<NemesisData> all() { return new ArrayList<>(byOwner.values()); }
}
