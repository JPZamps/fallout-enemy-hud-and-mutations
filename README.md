# Fallout Enemy HUD and Mutations

A Fallout flavoured combat layer for Minecraft. Enemies get a target readout, a stealth
indicator, mid fight mutations and higher ranked variants that can outclass you.

Everything is built from vanilla parts. No new items, no new models, no new textures, no
dependencies.

[Download on CurseForge](https://www.curseforge.com/minecraft/mc-mods/fallout-enemy-hud-and-mutations)

## What it adds

- **Target readout.** Look at a mob within 25 blocks and its name and health appear at the
  top of the screen, in a fixed width bar that shows what was lost rather than shrinking. It
  stops at walls and steps aside for boss bars.
- **Stealth indicator.** While crouching, HIDDEN, CAUTION or DANGER above the crosshair,
  with brackets that close in as the nearest threat approaches.
- **Legendary mobs.** Eight prefixes that change stats and emit a signature particle. Below
  half health they get one chance to mutate: full heal, a power spike and a new name.
- **Elite ranks.** A separate axis, from Tough up to General, wearing real armour.
- **Legendary gear.** Weapon and armour effects that keep working after you loot them.
- **Loot.** Over enchanted drops past the vanilla ceiling, added through the mob's own loot
  table so other mods' loot hooks still apply.

## Branches

| Branch | Minecraft | Loader | Status |
|--------|-----------|--------|--------|
| `mc-1.20.1` | 1.20.1 | Forge 47.4.13 | Released as 0.9.0 |
| `mc-1.21.1` | 1.21.1 | NeoForge | In progress |

`main` tracks the most current released branch.

## Building

Requires JDK 17. The Gradle wrapper handles the rest.

```
./gradlew build
```

The jar lands in `build/libs/`. To run a development client or server:

```
./gradlew runClient
./gradlew runServer
```

## Configuration

Two files are generated on first launch:

- `config/falloutenemyhud-common.toml` for the rules. On a server this is the copy that
  counts.
- `config/falloutenemyhud-client.toml` for the HUD, so every player controls their own
  display.

Every system has its own switch, and colours accept either hex values or Minecraft dye names.

## Testing commands

Permission level 2 required.

```
/mutate legendary <targets> [prefix]
/mutate elite <targets> [rank]
/mutate <targets>
/mutate clear <targets>
```

## Uninstalling

Stat changes are written into mobs as permanent attribute modifiers. Run `/mutate clear @e`
in loaded chunks before removing the mod, or any legendary or elite already in your world
stays renamed and buffed with nothing left to clean it up.

## License

MIT. See [LICENSE](LICENSE).
