# Nemesis

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen)
![Platform](https://img.shields.io/badge/platform-Spigot%20%7C%20Paper-orange)
![Java](https://img.shields.io/badge/Java-21%2B-blue)
![Version](https://img.shields.io/badge/version-1.0.0-informational)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

> The monster that kills you becomes your rival. It survives, hunts you, and grows stronger every time
> it kills you again. Slay it to claim your revenge.

Inspired by the acclaimed **Nemesis system** from *Shadow of Mordor*, reimagined for Minecraft servers.
Ordinary deaths become an ongoing, personal grudge: the mob that beat you is promoted into a named,
glowing rival that remembers you, tracks you down, and levels up with every kill — until you finally
hunt it down and settle the score for a hard-earned reward.

Every rivalry is unique, persists across restarts, and is tuned entirely through a deep, friendly
config — from stat curves to per-level abilities to the exact reward you earn.

---

## Table of contents

- [How it works](#how-it-works)
- [Level progression](#level-progression-default-config)
- [Installation](#installation)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
  - [Stat scaling](#stat-scaling)
  - [Per-level abilities](#per-level-abilities)
  - [Revenge rewards](#revenge-rewards)
- [Building from source](#building-from-source)
- [Roadmap](#roadmap)
- [Support](#support)
- [License](#license)

---

## How it works

1. **A monster kills you.** Instead of despawning, it's promoted to your personal **Nemesis** — given a
   name and title (*Grishnak the Cruel*), boosted stats, a glowing outline, immunity to sunlight, and it
   is marked never to despawn.
2. **It hunts you.** Whenever you're within range, it locks onto you and gives chase; you get an
   action-bar warning when it's close.
3. **It kills you again → it levels up.** Each rank adds new abilities (see below) and a scarier title,
   and it heals to full.
4. **You kill it → revenge.** You earn configurable loot (diamonds, XP and a named trophy by default),
   the server is told of your victory, and the grudge is settled.

Only one nemesis exists per player at a time, so the rivalry stays personal and meaningful.

## Level progression (default config)

| Level | Health | Damage | Speed | Added abilities |
|:---:|:---:|:---:|:---:|---|
| 1 | ×1.6 | ×1.4 | normal | Named, glowing, fireproof, persistent |
| 2 | ×2.2 | ×1.8 | +6% | — |
| 3 | ×2.8 | ×2.2 | +12% | Resistance I, knockback resistance, iron helmet |
| 4 | ×3.4 | ×2.6 | +18% | Regeneration I, Strength I, iron sword |
| 5 | ×4.0 | ×3.0 | +24% | Resistance II, Strength II, Speed I, full diamond kit |

*All of the above is configurable, and you can add ranks beyond 5.*

## Installation

1. Download `Nemesis-1.0.0.jar`.
2. Drop it into your server's `plugins/` folder.
3. Restart the server. A `config.yml` is generated on first run.

## Commands

Base command: `/nemesis` — alias `/nem`.

| Command | Description |
|---|---|
| `/nemesis info` | View your current nemesis — name, level, type, last-seen location |
| `/nemesis locate` | Where your nemesis was last seen, and how far away |
| `/nemesis list` | (admin) All active nemeses on the server |
| `/nemesis reload` | (admin) Reload the configuration |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `nemesis.use` | Inspect and locate your own nemesis | everyone |
| `nemesis.admin` | List all nemeses and reload the config | op |

## Configuration

Everything below is editable; run `/nemesis reload` to apply without a restart.

```yaml
promote-chance: 100     # % chance a mob that kills you becomes your nemesis
max-level: 5            # raise as high as you like — stats keep scaling
glowing: true           # nemesis glows through walls
fire-immune: true       # won't burn away in sunlight/lava
hunt:
  enabled: true
  range: 40             # locks onto you within this many blocks
  warn: true
  warn-range: 18        # action-bar warning when this close
eligible-mobs: [ ZOMBIE, SKELETON, SPIDER, PILLAGER, ... ]
```

### Stat scaling

Scaled from each mob's **own** natural stats, so a nemesis skeleton and a nemesis ravager both feel fair.

```yaml
scaling:
  health: { per-level: 0.6 }   # +60% max health per level
  damage: { per-level: 0.4 }   # +40% attack damage per level
  speed:  { per-level: 0.06, cap: 1.25 }  # gentle; hard-capped at 1.25x natural speed
```

### Per-level abilities

Give each level potion effects, gear and knockback resistance. **Add more levels (6, 7, 8…)** and raise
`max-level` to match — any level you don't define inherits the highest one you did.

```yaml
levels:
  3:
    knockback-resistance: 0.6
    effects: ["RESISTANCE:0"]           # "TYPE:amplifier" (0 = level I)
    equipment: { helmet: IRON_HELMET }
  5:
    knockback-resistance: 0.8
    effects: ["RESISTANCE:1", "STRENGTH:1", "SPEED:0"]
    equipment: { helmet: DIAMOND_HELMET, chestplate: DIAMOND_CHESTPLATE, weapon: DIAMOND_SWORD }
```

### Revenge rewards

Fully customizable drops, XP and trophy. Each drop is `amount + (per-level × level)`.

```yaml
revenge:
  xp-base: 20
  xp-per-level: 15
  drops:
    - { material: DIAMOND, amount: 0, per-level: 2 }
  trophy:
    enabled: true
    material: PLAYER_HEAD   # a decorative, uncraftable keepsake — not a farmable resource
    name: "&6Trophy of {name}"
    lore:
      - "&7Slain by {owner}"
      - "&8A nemesis vanquished."
```

## Building from source

Requires **JDK 21+** and **Maven**.

```bash
mvn clean package
# → target/Nemesis-1.0.0.jar
```

Only standard Bukkit APIs are used, so the built jar runs on both Spigot and Paper.

## Roadmap

- 👹 Nemesis **minions** it can summon at high level.
- 🎭 Multiple simultaneous nemeses per player — a rogues' gallery.
- 📖 A **grudge book** GUI showing your rivalry history and past trophies.
- 📊 Optional **boss bar** when your nemesis is nearby.

## Support

- 🌐 Website: [studio.rexmicky.dev](https://studio.rexmicky.dev)
- 💬 Discord: [discord.gg/pfF5TpQ9UE](https://discord.gg/pfF5TpQ9UE)

## License

Released under the [MIT License](LICENSE) — © 2026 Rex Micky · Rex's Studio.
