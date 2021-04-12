package de.hglabor.plugins.kitapi.kit.events;

import de.hglabor.plugins.kitapi.KitApi;
import de.hglabor.plugins.kitapi.kit.AbstractKit;
import de.hglabor.plugins.kitapi.player.KitPlayer;
import de.hglabor.plugins.kitapi.supplier.KitPlayerSupplier;
import de.hglabor.plugins.kitapi.util.Logger;
import de.hglabor.utils.localization.Localization;
import de.hglabor.utils.noriskutils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public abstract class KitEventHandler extends KitEvents {
    protected final KitPlayerSupplier playerSupplier;

    public KitEventHandler(KitPlayerSupplier playerSupplier) {
        this.playerSupplier = playerSupplier;
    }

    public static boolean canUseKit(Event event, KitPlayer kitPlayer, AbstractKit kit) {
        Player player = Bukkit.getPlayer(kitPlayer.getUUID());
        if (player == null) {
            return false;
        }

        if (!kit.isEnabled()) {
            return false;
        }

        if (!kit.isUsable()) {
            return false;
        }

        if (!kitPlayer.isValid()) {
            return false;
        }

        Logger.debug(String.format("%s, %s", kit.getName(), event.getEventName()));

        //other events will be also triggered like playermoveevent and print cooldown
        if (kit.getKitEvents().stream().noneMatch(kitEventInfo -> kitEventInfo.getEvent().equals(event.getClass()))) {
            //Complete Garbage I hope this doesnt break something
            if (kit.getKitEvents().stream().noneMatch(kitEvent -> event.getClass().getSuperclass().equals(kitEvent.getEvent()))) {
                Logger.debug(String.format("%s, %s §4DIDNT FIT", kit.getName(), event.getEventName()));
                return false;
            }
        }

        //Player doesnt have kit
        if (!kitPlayer.hasKit(kit)) {
            Logger.debug(String.format("%s no kit %s", player.getName(), kit.getName()));
            return false;
        }
        //Players kits are disabled
        if (kitPlayer.areKitsDisabled()) {
            player.sendActionBar(Localization.INSTANCE.getMessage("kit.disabled", ChatUtils.getPlayerLocale(player)));
            return false;
        }
        //Player is on kitcooldown
        KitEventInfo kitEventInfo = kit.getKitEvents().stream().filter(info -> info.getEvent().equals(event.getClass())).findFirst().orElse(null);
        //ugly lol?
        if (kitEventInfo != null) {
            if (!kitEventInfo.isIgnoreCooldown()) {
                if (KitApi.getInstance().sendCooldownMessage(kitPlayer, kit)) {
                    return false;
                }
            }
        } else {
            if (KitApi.getInstance().sendCooldownMessage(kitPlayer, kit)) {
                return false;
            }
        }

        Logger.debug(String.format("%s, %s §aSUCCESSFULL", kit.getName(), event.getEventName()));
        return true;
    }

    public static boolean canUseKitItem(Event event, KitPlayer kitPlayer, AbstractKit kit) {
        Player player = Bukkit.getPlayer(kitPlayer.getUUID());

        if (player == null) {
            return false;
        }

        Logger.debug(String.format("Kit %s, Player %s, Event %s", kit.getName(), player.getName(), event.getEventName()));

        //Kit has no kititem
        if (kit.getMainKitItem() == null) {
            Logger.debug(String.format("Kit %s, Player %s, Event %s §4NO KIT ITEM", kit.getName(), player.getName(), event.getEventName()));
            return false;
        }

        if (!KitApi.getInstance().hasKitItemInAnyHand(player, kit)) {
            Logger.debug(String.format("Kit %s, Player %s, Event %s §4NO KIT ITEM IN HAND", kit.getName(), player.getName(), event.getEventName()));
            return false;
        }

        if (canUseKit(event, kitPlayer, kit)) {
            return true;
        }

        return false;
    }
}
