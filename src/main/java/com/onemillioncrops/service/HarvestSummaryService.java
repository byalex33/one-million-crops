package com.onemillioncrops.service;

import com.onemillioncrops.OneMillionCropsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Announces how much each currently online player harvested during the configured time window. */
public final class HarvestSummaryService {
    private final OneMillionCropsPlugin plugin;
    private final SummaryActionService actions;
    private final Map<UUID, Long> harvested = new HashMap<>();
    private BukkitTask task;
    private long nextRunAtMillis;

    public HarvestSummaryService(OneMillionCropsPlugin plugin) {
        this.plugin = plugin;
        this.actions = new SummaryActionService(plugin);
    }

    public void start() {
        stopTask();
        if (plugin.configManager().harvestSummary().enabled()) {
            schedule();
        } else {
            harvested.clear();
        }
    }

    public void record(Player player, long amount) {
        if (amount <= 0 || !plugin.configManager().harvestSummary().enabled()) {
            return;
        }
        harvested.merge(player.getUniqueId(), amount, HarvestSummaryService::saturatingAdd);
    }

    public void stop() {
        stopTask();
        harvested.clear();
    }

    public SummaryStatus status() {
        long total = 0L;
        for (long amount : harvested.values()) {
            total = saturatingAdd(total, amount);
        }
        boolean scheduled = task != null && !task.isCancelled();
        long remainingMillis = scheduled ? Math.max(0L, nextRunAtMillis - System.currentTimeMillis()) : 0L;
        return new SummaryStatus(scheduled, remainingMillis, harvested.size(), total);
    }

    public boolean announceNow() {
        if (!plugin.configManager().harvestSummary().enabled()) {
            return false;
        }
        stopTask();
        announce();
        schedule();
        return true;
    }

    private void announce() {
        int intervalMinutes = plugin.configManager().harvestSummary().intervalMinutes();
        nextRunAtMillis = System.currentTimeMillis() + intervalMillis(intervalMinutes);
        Map<UUID, Long> completedWindow = new HashMap<>(harvested);
        harvested.clear();

        List<PlayerTotal> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(new PlayerTotal(
                    player,
                    completedWindow.getOrDefault(player.getUniqueId(), 0L)
            ));
        }
        if (players.isEmpty()) {
            return;
        }
        players.sort(Comparator.comparingLong(PlayerTotal::amount).reversed()
                .thenComparing(summary -> summary.player().getName(), String.CASE_INSENSITIVE_ORDER));

        long total = 0L;
        List<SummaryActionService.SummaryEntry> entries = new ArrayList<>();
        for (PlayerTotal summary : players) {
            total = saturatingAdd(total, summary.amount());
            entries.add(new SummaryActionService.SummaryEntry(summary.player().getName(), summary.amount()));
        }
        actions.execute(plugin.configManager().harvestSummary().actions(),
                players.stream().map(PlayerTotal::player).toList(), entries, total, intervalMinutes);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        nextRunAtMillis = 0L;
    }

    private void schedule() {
        int minutes = plugin.configManager().harvestSummary().intervalMinutes();
        long intervalTicks = intervalTicks(minutes);
        nextRunAtMillis = System.currentTimeMillis() + intervalMillis(minutes);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::announce, intervalTicks, intervalTicks);
    }

    static long intervalTicks(int minutes) {
        return minutes * 60L * 20L;
    }

    static long intervalMillis(int minutes) {
        return minutes * 60L * 1_000L;
    }

    private static long saturatingAdd(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private record PlayerTotal(Player player, long amount) {
    }

    public record SummaryStatus(boolean scheduled, long remainingMillis, int contributors, long harvested) {
    }
}
