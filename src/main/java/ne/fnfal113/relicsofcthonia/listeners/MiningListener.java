package ne.fnfal113.relicsofcthonia.listeners;

import com.github.drakescraft_labs.slimefun4.api.events.BlockPlacerPlaceEvent;
import ne.fnfal113.relicsofcthonia.RelicsOfCthonia;
import ne.fnfal113.relicsofcthonia.RelicsRegistry;
import ne.fnfal113.relicsofcthonia.slimefun.relics.AbstractRelic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static ne.fnfal113.relicsofcthonia.core.Keys.PLACED_BLOCK;

public class MiningListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled() || event.getPlayer().getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }

        Block block = event.getBlock();

        // Only naturally generated blocks are accepted to prevent place and break farming
        if (block.hasMetadata(PLACED_BLOCK)) {
            block.removeMetadata(PLACED_BLOCK, RelicsOfCthonia.getInstance());
            return;
        }

        int maxDrops = Math.max(0, RelicsOfCthonia.getInstance().getConfig()
            .getInt("mining.max-relic-drops-per-block", 1));
        if (maxDrops == 0) {
            return;
        }

        double multiplier = block.getType() == org.bukkit.Material.NETHERRACK
            ? RelicsOfCthonia.getInstance().getConfig().getDouble("mining.netherrack-drop-chance-multiplier", 0.05)
            : RelicsOfCthonia.getInstance().getConfig().getDouble("mining.material-drop-chance-multiplier", 0.20);
        multiplier = Math.clamp(multiplier, 0.0, 1.0);

        int dropped = 0;
        List<AbstractRelic> relics = RelicsRegistry.BLOCK_SOURCES.get(block.getType());
        if (relics == null || relics.isEmpty()) {
            return;
        }

        // The registry is shared by every break event. Shuffle a copy so one
        // player's mining cannot mutate the source list used by another event.
        relics = new ArrayList<>(relics);
        java.util.Collections.shuffle(relics);
        for (AbstractRelic relic : relics) {
            if (relic.isDisabledIn(event.getPlayer().getWorld()) || relic.isDisabled()) {
                continue;
            }

            if (ThreadLocalRandom.current().nextDouble() < relic.getDropChance() * multiplier) {
                ItemStack drop = relic.randomRelic();
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
                if (++dropped >= maxDrops) {
                    break;
                }
            }
        }
    }

    /*
     * Prevent players from block place farming any relics by tracking blocks placed by players/block placers
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        event.getBlock().setMetadata(PLACED_BLOCK, new FixedMetadataValue(RelicsOfCthonia.getInstance(), "placed"));
    }

    /*
     * Prevent players from block place farming any relics by tracking blocks placed by players/block placers
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlacerPlaced(BlockPlacerPlaceEvent event) {
        event.getBlock().setMetadata(PLACED_BLOCK, new FixedMetadataValue(RelicsOfCthonia.getInstance(), "placed"));
    }
}
