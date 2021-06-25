package de.hglabor.plugins.kitapi.kit.kits;

import com.google.common.collect.ImmutableMap;
import de.hglabor.plugins.kitapi.KitApi;
import de.hglabor.plugins.kitapi.kit.AbstractKit;
import de.hglabor.plugins.kitapi.kit.events.KitEvent;
import de.hglabor.plugins.kitapi.kit.settings.IntArg;
import de.hglabor.plugins.kitapi.kit.settings.PotionEffectArg;
import de.hglabor.plugins.kitapi.player.KitPlayer;
import de.hglabor.utils.localization.Localization;
import de.hglabor.utils.noriskutils.ChatUtils;
import de.hglabor.utils.noriskutils.TimeConverter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScoutKit extends AbstractKit {
    public final static ScoutKit INSTANCE = new ScoutKit();

    @PotionEffectArg
    private final PotionEffectType potionEffectType;
    @IntArg
    private final int duration, amplifier, potionAmount, supplyInterval;
    private final String runnableKey;
    private final String timeLeftKey;

    private ScoutKit() {
        super("Scout", Material.POTION);
        this.potionEffectType = PotionEffectType.SPEED;
        this.duration = 67;
        this.amplifier = 1;
        this.potionAmount = 2;
        this.supplyInterval = 300;
        this.runnableKey = this.getName() + "runnableKey";
        this.timeLeftKey = this.getName() + "timeLeft";
        this.setMainKitItem(createScoutPotion(), potionAmount);
        this.setDisplayItem(createScoutPotion());
    }

    @KitEvent
    @Override
    public void onEntityDamage(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player))return;
        Player player = (Player) e.getEntity();
        if(!e.getCause().equals(EntityDamageEvent.DamageCause.FALL))return;
        if (player.hasPotionEffect(PotionEffectType.SPEED))
        {
            if(e.getFinalDamage()>4){
                e.setDamage(4);
            }
        }
    }

    @Override
    public void onEnable(KitPlayer kitPlayer) {
        PotionSupplierTask potionSupplierTask = new PotionSupplierTask(kitPlayer);
        kitPlayer.putKitAttribute(runnableKey, potionSupplierTask);
        int timeTilNextPotion = kitPlayer.getKitAttributeOrDefault(timeLeftKey, supplyInterval);
        Localization.INSTANCE.getMessage("scout.nextSupplyIn", ImmutableMap.of("time", TimeConverter.stringify(timeTilNextPotion * 20)), ChatUtils.getPlayerLocale(kitPlayer.getUUID()));
        potionSupplierTask.runTaskLater(KitApi.getInstance().getPlugin(), timeTilNextPotion * 20L);
    }

    @Override
    public void onDeactivation(KitPlayer kitPlayer) {
        PotionSupplierTask potionSupplierTask = kitPlayer.getKitAttribute(runnableKey);
        potionSupplierTask.cancel();
        long timeLeft = (System.currentTimeMillis() / 1000L) - (potionSupplierTask.startTime / 1000L);
        kitPlayer.putKitAttribute(timeLeftKey, (int) (kitPlayer.getKitAttributeOrDefault(timeLeftKey, supplyInterval) - timeLeft));
    }

    private ItemStack createScoutPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(potionEffectType, duration * 20, amplifier), true);
        meta.setDisplayName(ChatColor.AQUA + "Scout Potion");
        meta.setColor(Color.AQUA);
        potion.setItemMeta(meta);
        return potion;
    }

    private class PotionSupplierTask extends BukkitRunnable {
        private final KitPlayer kitPlayer;
        private final long startTime;

        PotionSupplierTask(KitPlayer kitPlayer) {
            this.kitPlayer = kitPlayer;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            if (isCancelled()) return;
            List<ItemStack> potions = IntStream.rangeClosed(0, potionAmount).mapToObj(i -> createScoutPotion()).collect(Collectors.toList());
            Localization.INSTANCE.getMessage("scout.newPotions", ImmutableMap.of("amount", String.valueOf(potionAmount)), ChatUtils.getPlayerLocale(kitPlayer.getUUID()));
            KitApi.getInstance().giveKitItemsIfSlotEmpty(kitPlayer, ScoutKit.INSTANCE, potions);
            kitPlayer.putKitAttribute(timeLeftKey, supplyInterval);
            onEnable(kitPlayer);
        }
    }
}
