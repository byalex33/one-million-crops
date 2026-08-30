package com.onemillioncrops.listener;

import com.onemillioncrops.OneMillionCropsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Replants cocoa pods broken by water or by moving their supporting jungle logs with a piston.
 * The replacement pod is paid for by one bean from the block's drops.
 */
public final class CocoaAutoReplantListener implements Listener {
    private static final int PENDING_DROP_TICKS = 20;
    private static final int REPLANT_ATTEMPTS = 100;
    private static final List<BlockFace> SIDES = List.of(
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    );
    private static final Set<Material> JUNGLE_SUPPORTS = EnumSet.of(
            Material.JUNGLE_LOG,
            Material.JUNGLE_WOOD,
            Material.STRIPPED_JUNGLE_LOG,
            Material.STRIPPED_JUNGLE_WOOD
    );

    private final OneMillionCropsPlugin plugin;
    private final Map<BlockPosition, ReplantPlan> pending = new HashMap<>();

    public CocoaAutoReplantListener(OneMillionCropsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        prepare(event.getBlocks(), pistonMovement(event.getDirection(), true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        prepare(event.getBlocks(), pistonMovement(event.getDirection(), false));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWaterFlow(BlockFromToEvent event) {
        if (!isWater(event.getBlock().getType())) {
            return;
        }

        Block pod = event.getToBlock();
        if (!(pod.getBlockData() instanceof Cocoa cocoa)
                || !isJungleSupport(pod.getRelative(cocoa.getFacing()).getType())) {
            return;
        }

        prepare(pod, BlockPosition.of(pod), cocoa);
    }

    private void prepare(List<Block> movedBlocks, BlockFace movement) {
        for (Block support : movedBlocks) {
            if (!isJungleSupport(support.getType())) {
                continue;
            }
            for (BlockFace side : SIDES) {
                Block pod = support.getRelative(side);
                if (!(pod.getBlockData() instanceof Cocoa cocoa)
                        || !isAttachedToSupport(cocoa.getFacing(), side)) {
                    continue;
                }

                BlockPosition source = BlockPosition.of(pod);
                prepare(pod, source.relative(movement), cocoa);
            }
        }
    }

    private void prepare(Block pod, BlockPosition target, Cocoa cocoa) {
        Cocoa seed = (Cocoa) cocoa.clone();
        seed.setAge(0);
        BlockPosition source = BlockPosition.of(pod);
        ReplantPlan plan = new ReplantPlan(source, target, seed);
        pending.put(source, plan);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> pending.remove(source, plan), PENDING_DROP_TICKS);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCocoaDrop(ItemSpawnEvent event) {
        Item item = event.getEntity();
        ItemStack stack = item.getItemStack();
        if (stack.getType() != Material.COCOA_BEANS) {
            return;
        }

        ReplantPlan plan = pending.remove(BlockPosition.of(item.getLocation().getBlock()));
        if (plan == null) {
            return;
        }

        int remaining = remainingAfterReplantCost(stack.getType(), stack.getAmount());
        if (remaining < 0) {
            return;
        }
        if (remaining == 0) {
            event.setCancelled(true);
        } else {
            stack.setAmount(remaining);
            item.setItemStack(stack);
        }
        Bukkit.getScheduler().runTask(plugin, () -> replant(plan, REPLANT_ATTEMPTS));
    }

    private void replant(ReplantPlan plan, int attemptsRemaining) {
        World world = Bukkit.getWorld(plan.source().worldId());
        if (world == null) {
            return;
        }
        if (replantAt(world, plan.target(), plan.seed())
                || replantAt(world, plan.source(), plan.seed())) {
            return;
        }
        if (attemptsRemaining > 1) {
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> replant(plan, attemptsRemaining - 1), 1L);
        }
    }

    private boolean replantAt(World world, BlockPosition position, Cocoa seed) {
        Block target = world.getBlockAt(position.x(), position.y(), position.z());
        if (!target.isEmpty()) {
            return false;
        }
        Block support = target.getRelative(seed.getFacing());
        if (!isJungleSupport(support.getType())) {
            return false;
        }
        target.setBlockData(seed, false);
        return true;
    }

    static BlockFace pistonMovement(BlockFace pistonDirection, boolean extending) {
        return extending ? pistonDirection : pistonDirection.getOppositeFace();
    }

    static boolean isJungleSupport(Material material) {
        return material != null && JUNGLE_SUPPORTS.contains(material);
    }

    static boolean isWater(Material material) {
        return material == Material.WATER;
    }

    static boolean isAttachedToSupport(BlockFace cocoaFacing, BlockFace sideFromSupport) {
        return cocoaFacing == sideFromSupport.getOppositeFace();
    }

    static int remainingAfterReplantCost(Material material, int amount) {
        return material == Material.COCOA_BEANS && amount > 0 ? amount - 1 : -1;
    }

    private record ReplantPlan(BlockPosition source, BlockPosition target, Cocoa seed) {
    }

    private record BlockPosition(UUID worldId, int x, int y, int z) {
        static BlockPosition of(Block block) {
            return new BlockPosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        BlockPosition relative(BlockFace face) {
            return new BlockPosition(worldId, x + face.getModX(), y + face.getModY(), z + face.getModZ());
        }
    }
}
