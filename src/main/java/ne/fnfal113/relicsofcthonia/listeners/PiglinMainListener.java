package ne.fnfal113.relicsofcthonia.listeners;

import com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem;
import com.github.drakescraft_labs.slimefun4.core.attributes.PiglinBarterDrop;
import ne.fnfal113.relicsofcthonia.RelicsOfCthonia;
import ne.fnfal113.relicsofcthonia.RelicsRegistry;
import ne.fnfal113.relicsofcthonia.slimefun.relics.AbstractRelic;
import ne.fnfal113.relicsofcthonia.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static ne.fnfal113.relicsofcthonia.core.Keys.CURRENTLY_TRADING_PLAYER;
import static ne.fnfal113.relicsofcthonia.core.Keys.CURRENTLY_TRADING_MATERIAL;
import static ne.fnfal113.relicsofcthonia.core.Keys.CURRENTLY_TRADING_RELIC;
import static ne.fnfal113.relicsofcthonia.core.Keys.CURRENTLY_TRADING_CONDITION;
import static ne.fnfal113.relicsofcthonia.core.Keys.CURRENTLY_TRADING_TOKEN;

public class PiglinMainListener implements Listener {

    private static final Map<UUID, ItemStack> DROPPING_ITEM = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void cancelInteractingWhileTrading(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Piglin piglin && isCurrentlyTradingRelic(piglin)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiglinRightClick(PlayerInteractEntityEvent event){
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (!(event.getRightClicked() instanceof Piglin piglin) || !piglin.isAdult() || piglin.hasMetadata("NPC")
                || event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        if (!(SlimefunItem.getByItem(mainHandItem) instanceof AbstractRelic relic)) {
            return;
        }

        if (isCurrentlyTradingRelic(piglin)) {
            event.setCancelled(true);
            return;
        }

        // Heads cannot be traded by default so we have to add relics as a barter material.
        // Persist the whole transaction before vanilla can replace the input stack.
        Material type = mainHandItem.getType();
        piglin.addBarterMaterial(type);
        UUID token = UUID.randomUUID();
        setCurrentTrade(piglin, player, relic, type, AbstractRelic.getRelicCondition(mainHandItem), token);
        RelicsOfCthonia.getInstance().getServer().getScheduler().runTaskLater(RelicsOfCthonia.getInstance(), () -> {
            if (!piglin.isValid()) {
                return;
            }
            TradeState pending = getCurrentTrade(piglin);
            if (pending != null && pending.token().equals(token)) {
                piglin.removeBarterMaterial(pending.material());
                clearCurrentTrade(piglin);
            }
        }, 200L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBarter(PiglinBarterEvent event){
        Piglin piglin = event.getEntity();
        TradeState trade = clearCurrentTrade(piglin);
        if (trade == null) {
            return;
        }

        piglin.removeBarterMaterial(trade.material());
        Optional<Player> player = Optional.ofNullable(Bukkit.getPlayer(trade.playerId()));
        if (event.isCancelled()) {
            player.ifPresent(p -> Utils.sendRelicMessage("&cThe trade was cancelled by an external source!", p));
            return;
        }

        SlimefunItem storedItem = SlimefunItem.getById(trade.relicId());
        if (!(storedItem instanceof AbstractRelic relic)) {
            RelicsOfCthonia.getInstance().getLogger().warning("Se descartó un intercambio de reliquia con estado PDC inválido: " + trade.relicId());
            return;
        }

        if (event.getInput() == null || event.getInput().getType() != trade.material()) {
            RelicsOfCthonia.getInstance().getLogger().fine("Paper o un plugin normalizó la entrada de un trueque de " + relic.getId() + "; se usará el estado PDC original.");
        }

        // Random chance for trade to succeed
        if (ThreadLocalRandom.current().nextInt(100) > trade.condition()) {
            piglin.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, piglin.getLocation().add(0, 2.2, 0), 0);
            piglin.getWorld().playSound(piglin.getLocation(), Sound.ENTITY_PIGLIN_ANGRY, 1.0F, 1.0F);
            return;
        }

        List<ItemStack> rewards = RelicsRegistry.RELIC_OUTPUTS.get(relic);
        if (rewards == null || rewards.isEmpty()) {
            RelicsOfCthonia.getInstance().getLogger().severe("&cThe relic " + relic.getId() + " does not have a corresponding reward list");
            player.ifPresent(p -> Utils.sendRelicMessage("&cThe relic has no configured rewards, notify your server!", p));
            return;
        }

        int rewardIndex = ThreadLocalRandom.current().nextInt(0, rewards.size());
        ItemStack reward = rewards.get(rewardIndex).clone();
        reward.setAmount(relic.getPiglinRewardAmount());
        event.getOutcome().clear();
        event.getOutcome().add(reward);
        DROPPING_ITEM.put(piglin.getUniqueId(), reward);

        piglin.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, piglin.getLocation().add(0, 2.2, 0), 0);
        piglin.getWorld().playSound(piglin.getLocation(), Sound.ENTITY_PIGLIN_ADMIRING_ITEM, 1.0F, 1.0F);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // When piglins are damaged by an entity, they stop the active trade (vanilla behavior)
        if (event.getEntity() instanceof Piglin piglin) {
            TradeState trade = clearCurrentTrade(piglin);
            if (trade != null) {
                piglin.removeBarterMaterial(trade.material());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(EntityDropItemEvent event){
        // Prevent slimefun from overriding relic sourced barter drops
        ItemStack droppingItem = DROPPING_ITEM.remove(event.getEntity().getUniqueId());
        if (droppingItem != null && SlimefunItem.getByItem(event.getItemDrop().getItemStack()) instanceof PiglinBarterDrop) {
            event.getItemDrop().setItemStack(droppingItem);
        }
    }

    private void setCurrentTrade(Piglin piglin, Player player, AbstractRelic relic, Material material, int condition, UUID token) {
        piglin.getPersistentDataContainer().set(CURRENTLY_TRADING_PLAYER, PersistentDataType.STRING, player.getUniqueId().toString());
        piglin.getPersistentDataContainer().set(CURRENTLY_TRADING_RELIC, PersistentDataType.STRING, relic.getId());
        piglin.getPersistentDataContainer().set(CURRENTLY_TRADING_MATERIAL, PersistentDataType.STRING, material.name());
        piglin.getPersistentDataContainer().set(CURRENTLY_TRADING_CONDITION, PersistentDataType.INTEGER, condition);
        piglin.getPersistentDataContainer().set(CURRENTLY_TRADING_TOKEN, PersistentDataType.STRING, token.toString());
    }

    private TradeState clearCurrentTrade(Piglin piglin) {
        TradeState trade = getCurrentTrade(piglin);
        piglin.getPersistentDataContainer().remove(CURRENTLY_TRADING_PLAYER);
        piglin.getPersistentDataContainer().remove(CURRENTLY_TRADING_RELIC);
        piglin.getPersistentDataContainer().remove(CURRENTLY_TRADING_MATERIAL);
        piglin.getPersistentDataContainer().remove(CURRENTLY_TRADING_CONDITION);
        piglin.getPersistentDataContainer().remove(CURRENTLY_TRADING_TOKEN);
        return trade;
    }

    private TradeState getCurrentTrade(Piglin piglin) {
        String idString = piglin.getPersistentDataContainer().get(CURRENTLY_TRADING_PLAYER, PersistentDataType.STRING);
        String relicId = piglin.getPersistentDataContainer().get(CURRENTLY_TRADING_RELIC, PersistentDataType.STRING);
        String material = piglin.getPersistentDataContainer().get(CURRENTLY_TRADING_MATERIAL, PersistentDataType.STRING);
        Integer condition = piglin.getPersistentDataContainer().get(CURRENTLY_TRADING_CONDITION, PersistentDataType.INTEGER);
        String token = piglin.getPersistentDataContainer().get(CURRENTLY_TRADING_TOKEN, PersistentDataType.STRING);
        if (idString == null || relicId == null || material == null || condition == null || token == null) {
            return null;
        }

        try {
            return new TradeState(UUID.fromString(idString), relicId, Material.valueOf(material), Math.clamp(condition, 0, 99), UUID.fromString(token));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isCurrentlyTradingRelic(Piglin piglin) {
        return getCurrentTrade(piglin) != null;
    }

    private record TradeState(UUID playerId, String relicId, Material material, int condition, UUID token) {}
}
