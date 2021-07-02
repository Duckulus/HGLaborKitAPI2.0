//Created by Duckulus on 02 Jul, 2021 

package de.hglabor.plugins.kitapi.kit.kits;

import de.hglabor.plugins.kitapi.KitApi;
import de.hglabor.plugins.kitapi.kit.AbstractKit;
import de.hglabor.plugins.kitapi.kit.events.KitEvent;
import de.hglabor.plugins.kitapi.kit.settings.FloatArg;
import de.hglabor.plugins.kitapi.kit.settings.IntArg;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.UUID;

public class TimelordKit extends AbstractKit implements Listener {

    public static final TimelordKit INSTANCE = new TimelordKit();
    @IntArg
    private final int duration;
    @FloatArg
    private final float cooldown;
    @IntArg
    private final int radius;

    private final ArrayList<UUID> frozenPlayers;

    protected TimelordKit() {
        super("Timelord", Material.CLOCK);
        duration = 10;
        radius = 7;
        cooldown = 50;
        frozenPlayers = new ArrayList<>();
        setMainKitItem(getDisplayMaterial());
    }

    @KitEvent
    @Override
    public void onPlayerRightClickKitItem(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        int count = 0;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player) {
                count++;
                frozenPlayers.add(entity.getUniqueId());
                entity.sendMessage("§c§lYour body stops moving...");
                entity.sendMessage("§c§lTime stops around you...");
                entity.sendMessage("§c§lIt's a Timelord!");
                Bukkit.getScheduler().scheduleSyncDelayedTask(KitApi.getInstance().getPlugin(), () -> frozenPlayers.remove(entity.getUniqueId()), duration * 20L);
            }
        }
        if (count == 0) {
            player.sendMessage("§cNo nearby players");
        } else {
            player.sendMessage("§eStopped time for " + count + " Players");
            KitApi.getInstance().getPlayer(player).activateKitCooldown(this);
            spawnSmoke(player.getLocation());

        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (frozenPlayers.contains(e.getPlayer().getUniqueId())) {
            e.getPlayer().teleport(e.getPlayer().getLocation());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        frozenPlayers.remove(e.getEntity().getUniqueId());
    }

    private void spawnSmoke(Location location) {
        for (int x = -radius; x < radius; x++) {
            for (int y = -radius; y < radius; y++) {
                for (int z = -radius; z < radius; z++) {
                    location.clone().getWorld().playEffect(location.clone().add(x, y, z), Effect.SMOKE, 10);
                }
            }
        }
    }

    @Override
    public float getCooldown() {
        return cooldown;
    }
}