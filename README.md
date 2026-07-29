# SEssentials — Sourby Essentials

A Folia-safe, clean-room "essentials" suite for **SourbyCraft** (Paper 1.21.9, Java 25,
Gradle KTS). Original implementation of standard server-admin commands — not a copy of
any proprietary plugin. Built as an independent module system so features stay isolated.

> Deliberately **excludes** `ping` and the tab list (handled elsewhere).

## Modules & commands

| Module | Commands |
|--------|----------|
| **teleport** | `tp`, `tphere`, `tpall`, `tppos`, `top`, `tpa`, `tpahere`, `tpaccept`/`tpyes`, `tpdeny`/`tpno`, `back`/`return`, `spawn`, `setspawn` |
| **home** | `home`, `sethome`, `delhome`, `homes`/`homelist` (per-tier limits via `sessentials.homes.<n>`) |
| **warp** | `warp`, `setwarp`, `delwarp`, `warps`/`warplist` |
| **playerstate** | `heal`, `feed`, `god`, `fly`, `speed`, `gamemode`/`gm` + `gmc`/`gms`/`gma`/`gmsp`, `hat`, `repair`/`fix`, `workbench`/`wb`, `anvil`, `enderchest`/`ec`, `grindstone`, `cartography`, `loom`, `smithingtable`, `stonecutter` |
| **items** | `give`/`i`/`item`, `more`, `enchant`, `unenchant`, `rename`/`itemname`, `lore`, `clear`/`ci`, `condense` |
| **worldtime** | `time`, `day`, `night`, `weather`/`sun`, `ptime`, `pweather` |
| **messaging** | `msg`/`tell`/`w`/`pm`/`whisper`, `reply`/`r`, `socialspy`/`ss`, `broadcast`/`bc`, `me` |
| **identity** | `nick`/`nickname`, `realname`, `afk`, `seen`/`lastseen` |
| **moderation** | `kick`, `ban`, `tempban`, `unban`/`pardon`, `mute`, `unmute`, `kill`, `vanish`/`v`, `invsee`, `endersee` |
| **economy** | `balance`/`bal`/`money`, `pay`, `baltop`, `eco` (Vault-hooked; amounts accept `1k`/`1.5m`/`2b`) |
| **kits** | `kit`, `kits`/`kitlist`, `kit create`, `kit delete` (per-kit cooldown + permission) |
| **utility** | `near`/`nearby`, `list`/`online`/`who`, `sudo`, `spawnmob`/`mob`, `lightning`/`strike`, `gc`/`memory` |

## Architecture

- `SEssentialsPlugin` enables a list of `EssModule`s (`Modules.all()`); each module
  registers its own Brigadier commands + listeners in `enable(plugin)`.
- Shared services: `Msg` (Small Caps + palette messaging), `economy()` (Vault hook),
  `stores()` (per-feature YAML data files), `Schedulers` (Folia entity/async/region),
  `Cmds` (Brigadier helpers + online-player suggestions).
- **Folia-safe throughout**: teleports use `teleportAsync`; entity/world work hops to the
  owning region thread; no legacy `Bukkit.getScheduler()`.

## Build

```
./gradlew build      # → build/libs/SEssentials-1.0.0.jar
```

## Notes

Permissions are `sessentials.<command>` (+ `.others` for target variants), default op.
Chat input for private messages is sent as plain text (never parsed as MiniMessage),
preventing formatting injection.
