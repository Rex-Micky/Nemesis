package dev.rexmicky.nemesis;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class NemesisListener implements Listener {

    private final NemesisPlugin plugin;

    public NemesisListener(NemesisPlugin plugin) {
        this.plugin = plugin;
    }

    private NemesisManager mgr() { return plugin.getManager(); }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        LivingEntity mob = resolveKillerMob(victim);
        if (mob == null) return;

        UUID mobOwner = mgr().ownerOf(mob);
        if (mobOwner != null) {
            // Killed by a nemesis. If it's mine, it levels up. If it's someone else's, ignore.
            if (mobOwner.equals(victim.getUniqueId())) mgr().levelUp(victim, mob);
            return;
        }
        // Killed by an ordinary mob — maybe a nemesis is born.
        if (mgr().getByOwner(victim.getUniqueId()) != null) return; // one nemesis at a time
        if (!mgr().isEligible(mob.getType())) return;
        if (!mgr().rollChance()) return;
        mgr().createNemesis(victim, mob);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        UUID owner = mgr().ownerOf(dead);
        if (owner == null) return;
        NemesisData data = mgr().getByOwner(owner);
        if (data == null) return;
        mgr().handleDeath(event, data);
    }

    /** Find the living mob responsible for a player's death, following projectiles to their shooter. */
    private LivingEntity resolveKillerMob(Player victim) {
        EntityDamageEvent cause = victim.getLastDamageCause();
        if (!(cause instanceof EntityDamageByEntityEvent ee)) return null;
        Entity damager = ee.getDamager();
        if (damager instanceof Player) return null;
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof LivingEntity le && !(src instanceof Player)) return le;
            return null;
        }
        if (damager instanceof LivingEntity le) return le;
        return null;
    }
}
