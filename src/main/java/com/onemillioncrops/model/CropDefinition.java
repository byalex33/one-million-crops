package com.onemillioncrops.model;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;

import java.util.EnumSet;
import java.util.Set;

public record CropDefinition(
        String id,
        Material item,
        Set<Material> sources,
        String displayMiniMessage
) {
    /*
     * These blocks use Ageable as a growth timer or structural state rather
     * than as an indication that the block is ready to harvest. Requiring
     * maximum age for them prevents natural vertical growth from ever
     * becoming eligible. Player-placed re-drops are handled separately by
     * PlacedSourceTracker.
     */
    private static final Set<Material> AGE_INDEPENDENT_SOURCES = EnumSet.of(
            Material.SUGAR_CANE,
            Material.CACTUS,
            Material.BAMBOO,
            Material.BAMBOO_SAPLING,
            Material.KELP,
            Material.KELP_PLANT,
            Material.CHORUS_PLANT,
            Material.CHORUS_FLOWER
    );

    public boolean matchesSource(BlockState state, boolean requireMature) {
        if (!sources.contains(state.getType())) {
            return false;
        }
        if (!requireMature) {
            return true;
        }
        if (maturityAgeApplies(state.getType()) && state.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return true;
    }

    static boolean maturityAgeApplies(Material source) {
        return !AGE_INDEPENDENT_SOURCES.contains(source);
    }
}
