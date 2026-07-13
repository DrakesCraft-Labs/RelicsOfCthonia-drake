package ne.fnfal113.relicsofcthonia.core;

import ne.fnfal113.relicsofcthonia.RelicsOfCthonia;
import org.bukkit.NamespacedKey;

public class Keys {
    public static final String SPAWNER_MOB = "relic_spawned_mob";
    public static final String PLACED_BLOCK = "placed_block";

    public static final NamespacedKey CURRENTLY_TRADING_PLAYER = createKey("currently_trading_player");
    public static final NamespacedKey CURRENTLY_TRADING_RELIC = createKey("currently_trading_relic");
    public static final NamespacedKey CURRENTLY_TRADING_MATERIAL = createKey("currently_trading_material");
    public static final NamespacedKey CURRENTLY_TRADING_CONDITION = createKey("currently_trading_condition");
    public static final NamespacedKey CURRENTLY_TRADING_TOKEN = createKey("currently_trading_token");
    public static final NamespacedKey RELIC_CONDITION = createKey("relic_condition");
    public static final NamespacedKey RELIC_CONDITION_QUOTA = createKey("condition_quota");

    private static NamespacedKey createKey(String id){
        return new NamespacedKey(RelicsOfCthonia.getInstance(), id.toLowerCase());
    }
}
