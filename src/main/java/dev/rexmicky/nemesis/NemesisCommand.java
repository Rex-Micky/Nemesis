package dev.rexmicky.nemesis;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class NemesisCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList("info", "locate", "list", "reload");

    private final NemesisPlugin plugin;

    public NemesisCommand(NemesisPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "info" : args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("nemesis.admin")) { plugin.send(sender, "info-none"); return true; }
                plugin.reloadConfig();
                plugin.getManager().reloadEligible();
                sender.sendMessage(plugin.format("prefix") + ChatColor.GREEN + "Config reloaded.");
            }
            case "list" -> {
                if (!sender.hasPermission("nemesis.admin")) return true;
                Collection<NemesisData> all = plugin.getManager().all();
                sender.sendMessage(plugin.format("prefix") + ChatColor.RED + "Active nemeses: " + all.size());
                for (NemesisData d : all) {
                    sender.sendMessage(ChatColor.GRAY + " • "
                            + ChatColor.stripColor(plugin.getManager().displayName(d))
                            + ChatColor.DARK_GRAY + " vs " + d.getOwnerName());
                }
            }
            case "locate" -> {
                if (sender instanceof Player p) doLocate(p);
            }
            default -> {
                if (sender instanceof Player p) doInfo(p);
                else sender.sendMessage("Players only.");
            }
        }
        return true;
    }

    private void doInfo(Player p) {
        NemesisData d = plugin.getManager().getByOwner(p.getUniqueId());
        if (d == null) { plugin.send(p, "info-none"); return; }
        plugin.sendRaw(p, "info-header");
        plugin.sendRaw(p, "info-name", "name", plugin.getManager().displayName(d));
        plugin.sendRaw(p, "info-level", "level", d.getLevel());
        plugin.sendRaw(p, "info-mob", "mob", pretty(d.getMobType()));
        sendSeen(p, d);
        plugin.sendRaw(p, "info-tip");
    }

    private void doLocate(Player p) {
        NemesisData d = plugin.getManager().getByOwner(p.getUniqueId());
        if (d == null) { plugin.send(p, "info-none"); return; }
        sendSeen(p, d);
    }

    private void sendSeen(Player p, NemesisData d) {
        plugin.sendRaw(p, "info-seen",
                "world", d.getWorld() == null ? "?" : d.getWorld(),
                "x", (int) d.getX(), "y", (int) d.getY(), "z", (int) d.getZ(),
                "distance", distanceStr(p, d));
    }

    private String distanceStr(Player p, NemesisData d) {
        if (d.getWorld() == null || !p.getWorld().getName().equals(d.getWorld())) return "another world";
        double dist = p.getLocation().distance(new Location(p.getWorld(), d.getX(), d.getY(), d.getZ()));
        return ((int) dist) + " blocks away";
    }

    private String pretty(String type) {
        return type.charAt(0) + type.substring(1).toLowerCase().replace('_', ' ');
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS.stream().filter(x -> x.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
