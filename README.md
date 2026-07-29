# SEssentials — Sourby Essentials

A **Folia-safe, clean-room** "essentials" suite for **SourbyCraft** — an original
implementation of the standard (and not-so-standard) server-admin toolkit, built from
scratch as an independent module system. **Not** a copy of, or a dependent on, any
proprietary plugin.

- **Paper 1.21.9 · Java 25 · Gradle KTS · Shadow 9.6.1**
- **`folia-supported: true`** — teleports use `teleportAsync`, all entity/world work hops
  to the owning region thread, and there is no legacy `Bukkit.getScheduler()` anywhere.
- **Standalone** — needs no external library plugin. Vault and PlaceholderAPI are
  *optional* soft-hooks (`compileOnly`); the SQLite driver used by the CMI importer is
  bundled and relocated.
- **101 self-contained modules**, ~285 commands. Each module lives in its own package and
  registers its own Brigadier commands + listeners in `enable(plugin)`.

> Deliberately **excludes** `ping` and the tab list (handled elsewhere), and the
> standalone `tps` command.

---

## Feature overview

Commands below are representative — most have aliases and `.others` target variants.

### Teleport & movement
`tp` · `tphere` · `tpall` · `tppos` · `top` · `tpa`/`tpahere` · `tpaccept`/`tpdeny` ·
`back` · `spawn`/`setspawn` · `warp`/`setwarp`/`delwarp`/`warps` ·
`home`/`sethome`/`delhome`/`homes` (per-tier limits via `sessentials.homes.<n>`) ·
`rtp` · `up`/`down`/`jump` · `elytra` · `tempfly`.

### Player state, vitals & items
`heal` · `feed` · `god` · `extinguish` · `suicide` · `fly` · `speed` · `gamemode`/`gm` ·
`hat` · `repair` · `enderchest` · virtual `workbench`/`anvil`/`grindstone`/`cartography`/
`loom`/`smithingtable`/`stonecutter` · `give`/`more`/`enchant`/`rename`/`lore`/`condense` ·
item `nbt` tools · `backpack` · `savedinv` · `invtools` (openshulker/sort/stack).

### Economy
`balance`/`pay`/`baltop`/`eco` (Vault-hooked; amounts accept `1k`/`1.5m`/`2b`) ·
`worth`/`sellhand`/`cheque` · `coinflip` · `lottery` · configurable `anvil-cost`.

### Chat, messaging & social
`msg`/`reply`/`socialspy` · `me` · `mail` · `ignore` · `staffchat` · `helpop`/`report`/
`note` · `localchat` · `chatcolor`/`clearchat` · rotating `autobroadcast` · `emoji` ·
`ctext`/`ctellraw` · `mutechat`/`slowmode`/`antispam`/`chatfilter`/`colorlimits` ·
custom `chatformat`, join/quit & death message control, `firstjoin` actions.

### Moderation & admin
`kick`/`ban`/`tempban`/`unban` · `mute`/`unmute` · `ipban`/`ipbanlist` · `jail`/`unjail` ·
`warn`/`warnings` · `freeze`/`cuff` + combat-tagging · `vanish` · `commandspy` ·
editable **invsee** GUI (view + force-take another player's inventory & armour) ·
`lockdown`/`kickall` · `sudo`/`batch` · `inspect` (whois/blockinfo/checkperm/…) ·
`import` from CMI or EssentialsX.

### Worlds & building
`worlds` (create/load/tp/setspawn) · `worldtime`/`ptime`/`pweather` · `worldtools`
(distance/getpos/findbiome/fixlight) · **holograms** (TextDisplay) · **portals** (wand
region select) · action **signs** · custom **recipes** · `spawner` · `entities`/`butcher`/
`clearground` · `itemframe` · `chestlock` · `silentchest` · `disableenchant`.

### Fun & cosmetics
`fun` (effect/firework/dye/skull/book) · `glow` · `kittycannon` · `expbottle` ·
`nightvision` · `painting` · `armorstand` editor · `title`/`titleall` · `broadcastsound` ·
`hpbar`.

### Utility & meta
`playtime`/`playtimerewards` · `scavenger` (keep-inv toggle) · `stats` · `counter` ·
`kits` (per-kit cooldown + permission) · PlaceholderAPI `%sessentials_*%` · `customalias` ·
`cmdcontrol` (per-command cooldown/cost) · `cmdwarmup` · `timedcommands` ·
`attachedcommands` · `armoreffects` · `afkkick` · `playeroptions` · `vopen` (virtual
enchant/brewing/furnace/hopper) · dupe-safe player-to-player `trade` · in-memory `grave`.

---

## SELIB — the built-in GUI/effects library

`dev.iyanz.sessentials.selib` is an original, internal library (no external plugin
needed) powering every custom screen:

- **`selib.gui`** — `Menu` (chest-menu base with per-slot handlers, editable slots, and
  click/drag/close routing), `PagedMenu` (automatic pagination), `ConfirmMenu` (yes/no
  dialog), `ItemBuilder` (MiniMessage names/lore, player heads, glow, custom model data),
  `Buttons` (common nav/filler icons).
- **`selib.effect`** — Folia-safe `Titles`, `ActionBars`, `BossBars`.

Custom screens built on SELIB: paged **`/homes` · `/warps` · `/kits` · `/emoji`**, the
**`/gmmenu`** game-mode picker, **`/ptimemenu`** personal time & weather picker, and the
paginated **`/helpmenu`** category browser — plus the editable inventory GUIs (invsee,
backpack, shulker editor, trade, dispose, grave).

---

## Architecture

- `SEssentialsPlugin.onEnable` enables every `EssModule` in `Modules.all()`; modules are
  fully independent (own package, own commands + listeners) so they stay isolated.
- Shared services: `util.Msg` (Small Caps + palette messaging), `economy()` (lazy Vault
  hook), `stores()` → thread-safe `YamlStore` (per-feature YAML data files),
  `scheduler.Schedulers` (Folia entity/async/region), `command.Cmds` (Brigadier helpers +
  online-player suggestions).
- **Data storage** is per-feature YAML under the plugin folder; `YamlStore` is
  monitor-guarded and serialises on the caller thread with an async disk write, so it is
  safe under Folia's parallel region threads.

## Security & correctness

The whole suite has been through a module-by-module audit. Notable guarantees:

- **No chat/name injection** — player-supplied text (chat, names, item/entity display
  names) is rendered as literal `Component`s, never re-parsed as MiniMessage. Only
  operator-gated command arguments may use MiniMessage.
- **Folia region-safety** — every entity/world mutation runs on the owning region thread;
  async chat/login listeners touch only thread-safe state.
- **Dupe-safe inventory GUIs** — the backpack, shulker editor, invsee and trade flows use
  single-live-inventory / re-entrancy guards and per-slot diffs so items can't be
  duplicated by re-opening or concurrent edits.
- **No money loss** — economy transfers refund on failed deposit; the lottery rolls its
  pot over rather than destroying it.

## Data import

`/sess import cmi` reads a server's own `plugins/CMI/cmi.sqlite.db` (homes) and
`/sess import essentials` reads EssentialsX `userdata/` + `warps/` — into SEssentials'
stores, never overwriting existing data. (These read other plugins' **data files**;
SEssentials contains none of their code.)

## Build

```bash
./gradlew build      # → build/libs/SEssentials-1.0.0.jar
```

## Permissions

`sessentials.<command>` (+ `.others` for target variants), default op; `sessentials.*`
grants everything. Home tiers via `sessentials.homes.<n>`.

## License

Original work by Yan for SourbyCraft.
