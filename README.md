# OneMillionCrops

A Paper 1.21.11 plugin for a shared **one million of every crop** challenge.

[Documentation](https://byalex33.github.io/one-million-crops/) · [Download latest release](https://github.com/byalex33/one-million-crops/releases/latest)

All players contribute to the same totals by default. Each item physically picked up adds one: picking up a stack of 37 wheat adds 37 wheat. Progress is stored in SQLite, shown on a live rotating scoreboard, available in an animated `/progress` inventory, and streamed to a built-in web dashboard.

## Requirements and installation

- Paper 1.21.11
- Java 21 or newer
- PlaceholderAPI 2.12.3 or newer (optional, for placeholders)

Copy [`target/OneMillionCrops-1.0.7.jar`](target/OneMillionCrops-1.0.7.jar) into the server's `plugins` folder, then restart the server. The plugin creates `config.yml`, `crops.yml`, `messages.yml`, and `progress.db` inside `plugins/OneMillionCrops/`.

To build it again locally:

```shell
cd web
npm ci
npm run build
cd ..
mvn clean package
```

The compiled dashboard is checked into `src/main/resources/web`, so a Java-only Maven rebuild also packages the latest committed web assets. Node.js 22+ is only needed when changing the React frontend.

The deployable shaded JAR is `target/OneMillionCrops-1.0.7.jar`. The smaller `original-*.jar` does not contain SQLite and must not be installed.

## Counting rules

The defaults are designed for a private server:

- `participants.mode: EVERYONE` means no usernames or team setup are needed.
- Manual and automatic farms are accepted.
- Water- and piston-produced crop items retain their provenance through hoppers and storage.
- Hopper-collected crops count when a participating player removes them from storage.
- Crop stacks placed into storage by a player do not become eligible, preventing deposit-and-withdraw recount loops.
- Items deliberately dropped by a player are marked ineligible, preventing drop-and-repickup loops.
- Crop items ejected by droppers or dispensers remain ineligible, so machines cannot launder an existing stack.
- Rebreaking a player-placed crop source does not count until the block has genuinely grown.
- Crop items produced by breaking unrelated blocks or dropped on player death are also excluded.
- Partially collected stacks count only the amount that entered the inventory.
- Rapid pickups are combined into one coloured harvest action-bar message.
- At the interval configured by `harvestSummary.amount` in `messages.yml`, its action list runs with a ranked harvest summary for everyone currently online.
- Admins can inspect its countdown with `/1mill summary` or broadcast it immediately with `/1mill summary now`.
- Totals stop exactly at the configured target.

Set `counting.allow-automated-farms: false` in `config.yml` to accept only drops the plugin traces to a mature player-harvested crop. Run `/1mill reload` afterward.

If the server later gains more players, set `participants.mode: ALLOWLIST` and add player UUIDs to `participants.allowlist`.

## Commands

| Command | Purpose | Permission |
|---|---|---|
| `/progress [crop]` | Open the animated progress GUI | `onemillion.progress` |
| `/1mill status` | Print every crop total | `onemillion.progress` |
| `/1mill scoreboard` | Toggle the live sidebar | `onemillion.progress` |
| `/1mill web` | Show the live dashboard address | `onemillion.progress` |
| `/1mill crops` | Open the crop enable/disable GUI | `onemillion.admin` |
| `/1mill reload` | Reload YAML configuration | `onemillion.admin` |
| `/1mill backup` | Create a timestamped SQLite backup | `onemillion.admin` |
| `/1mill reset confirm` | Back up and reset everything | `onemillion.admin` |
| `/1mill reset <crop> confirm` | Back up and reset one crop | `onemillion.admin` |

`onemillion.progress` is granted to everyone. `onemillion.admin` defaults to server operators.

## PlaceholderAPI

When PlaceholderAPI is installed, OneMillionCrops registers an internal expansion automatically. No eCloud expansion download is required. All numeric placeholders have a raw variant for calculations and a `_formatted` variant with thousands separators.

| Placeholder | Value |
|---|---|
| `%onemillioncrops_total%` | Harvested total across all enabled crops |
| `%onemillioncrops_total_formatted%` | Formatted harvested total |
| `%onemillioncrops_target%` | Target for each crop |
| `%onemillioncrops_target_formatted%` | Formatted target for each crop |
| `%onemillioncrops_goal%` | Combined target across all enabled crops |
| `%onemillioncrops_goal_formatted%` | Formatted combined target |
| `%onemillioncrops_remaining%` | Remaining amount across the whole challenge |
| `%onemillioncrops_remaining_formatted%` | Formatted remaining amount |
| `%onemillioncrops_percent%` | Overall completion percentage with two decimal places |
| `%onemillioncrops_completed_crops%` | Number of completed crops |
| `%onemillioncrops_crop_count%` | Number of enabled crops |
| `%onemillioncrops_all_completed%` | `true` when every enabled crop is complete |
| `%onemillioncrops_player_total%` | Viewing player's total contribution |
| `%onemillioncrops_player_total_formatted%` | Formatted player contribution |

Crop-specific placeholders use the crop ID from `crops.yml`, including IDs containing underscores. Replace `<crop>` below with values such as `wheat` or `nether_wart`.

| Placeholder | Value |
|---|---|
| `%onemillioncrops_crop_<crop>%` | Current amount (short form) |
| `%onemillioncrops_crop_<crop>_amount%` | Current amount |
| `%onemillioncrops_crop_<crop>_amount_formatted%` | Formatted current amount |
| `%onemillioncrops_crop_<crop>_target%` | Target for the crop |
| `%onemillioncrops_crop_<crop>_target_formatted%` | Formatted crop target |
| `%onemillioncrops_crop_<crop>_remaining%` | Remaining amount |
| `%onemillioncrops_crop_<crop>_remaining_formatted%` | Formatted remaining amount |
| `%onemillioncrops_crop_<crop>_percent%` | Completion percentage with two decimal places |
| `%onemillioncrops_crop_<crop>_completed%` | `true` when the crop is complete |
| `%onemillioncrops_crop_<crop>_player_amount%` | Viewing player's contribution to the crop |
| `%onemillioncrops_crop_<crop>_player_amount_formatted%` | Formatted player crop contribution |

Player placeholders return `0` when the calling plugin does not provide player context. Unknown crop IDs and statistics remain unresolved, making configuration typos visible.

## Live web dashboard

The plugin serves a responsive React dashboard from the same JAR. It includes live totals, completion and velocity metrics, a session momentum graph, per-crop ranking graph, every crop objective, the contributor leaderboard, online player count, and a recent pickup feed. Updates arrive over Server-Sent Events without refreshing the page.

The dashboard is read-only: it has no reset, command, database, or server-control endpoint. By default it listens at `http://127.0.0.1:8765`, which is only reachable from the Minecraft host. Use `/1mill web` to print the configured address.

To expose it directly on a trusted LAN, set `web.bind-address: 0.0.0.0` and allow the configured port through the host firewall. For a public server, keep the localhost binding and put an HTTPS reverse proxy in front of it, then set `web.public-url` to the external address. `/1mill reload` restarts the listener when web settings change.

Useful endpoints:

| Endpoint | Purpose |
|---|---|
| `/` | Dashboard application |
| `/api/v1/progress` | Current immutable JSON snapshot |
| `/api/v1/events` | Live Server-Sent Events stream |
| `/health` | Lightweight health check |

## Configuration

- `config.yml` controls the target, participant policy, automation, scoreboard timing and title animation, web listener, autosaves, and celebrations.
- `crops.yml` controls enabled crops, counted item materials, harvest source blocks, and MiniMessage display names. Operators can also change each `enabled` value live with `/1mill crops`; disabled crop progress is preserved.
- `messages.yml` contains all announcements, titles, and command messages in MiniMessage format.
- `progress.db` contains shared totals, individual contributions, completion state, and queued celebrations.
- `backups/` contains automatic pre-reset backups and backups created with `/1mill backup`.

The included catalogue contains wheat, carrots, potatoes, beetroot, nether wart, pumpkins, melon slices, sugar cane, cactus, cocoa beans, bamboo, kelp, sweet berries, glow berries, chorus fruit, and both mushroom types.

The default scoreboard title accepts a multi-stop MiniMessage gradient, then renders each visible character with a directly calculated color at 20 frames per second. The cyclic palette includes a blended transition from the final blue back to the first red, without relying on MiniMessage gradient phases at runtime.

## Harvest summary actions

Every configurable output in `messages.yml` has an `enabled` switch and an ordered `actions` list. The harvest summary additionally has its interval in minutes at `harvestSummary.amount`. Summary actions containing `%player%` or `%amount%` run once per leaderboard row; `%total%` and `%minutes%` are available to every summary action. Placeholders always use `%name%`, while angle brackets are reserved for MiniMessage tags.

| Action | Syntax and defaults |
|---|---|
| `[message]` | `[message] <text>` sends a chat line to every online player. A blank value creates a spacer line. |
| `[broadcast]` | `[broadcast] <text>` broadcasts a chat line to the whole server audience. |
| `[sound]` | `[sound] SOUND [volume] [pitch]`; volume defaults to `0.7` and pitch to `1.2`. |
| `[bossbar]` | `[bossbar] text \| seconds \| color \| overlay \| progress`; defaults: `5`, `GREEN`, `PROGRESS`, `1.0`. |
| `[title]` | `[title] title \| subtitle \| fade-in ticks \| stay ticks \| fade-out ticks`; timing defaults to `10, 60, 10`. |
| `[actionbar]` | `[actionbar] <text>` sends a MiniMessage action bar. |
| `[lightning]` | `[lightning]` shows a visual-only lightning strike at every online player. |
| `[particles]` | `[particles] TYPE [amount] [x] [y] [z] [speed]`; defaults to `HAPPY_VILLAGER 20 0.6 0.8 0.6 0.05`. |
| `[firework]` | `[firework] hex-colors \| type \| power \| count \| gap-ticks`; for example `[firework] #55FF55,#FFD54A | BALL_LARGE | 1 | 3 | 10`. |

Run `/1mill reload` after editing the section. Reloading also reschedules the summary with the new interval; setting `enabled: false` cancels it.

## Celebration behaviour

At 25%, 50%, 75%, and 90%, participating online players receive a sound, particles, and an action-bar announcement. At one million, the completion is persisted before it can trigger again and the server receives the full title, sound, broadcast, and firework sequence. Contributors who are offline receive their crop celebration after their next login. Completing every enabled crop triggers a separate grand finale.

## Verification

The project includes unit tests for target clamping, milestone transitions, contribution tracking, resets, database transactions, and JSON safety. It has also been started and stopped successfully on Paper `1.21.11-132` with the generated database and administrative commands exercised.
