package dev.rexmicky.nemesis;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class NemesisPlugin extends JavaPlugin {

    private NemesisManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new NemesisManager(this);
        manager.load();

        getServer().getPluginManager().registerEvents(new NemesisListener(this), this);
        NemesisCommand cmd = new NemesisCommand(this);
        getCommand("nemesis").setExecutor(cmd);
        getCommand("nemesis").setTabCompleter(cmd);

        // Nemeses hunt their owners.
        getServer().getScheduler().runTaskTimer(this, manager::huntTick, 40L, 40L);

        getLogger().info("Nemesis enabled - your rivals await.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.save();
    }

    public NemesisManager getManager() { return manager; }

    // Messaging

    public String format(String key, Object... placeholders) {
        String raw = getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public void send(CommandSender to, String key, Object... placeholders) {
        to.sendMessage(format("prefix") + format(key, placeholders));
    }

    public void sendRaw(CommandSender to, String key, Object... placeholders) {
        to.sendMessage(format(key, placeholders));
    }
}
