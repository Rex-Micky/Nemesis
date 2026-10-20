# Nemesis

When a hostile mob kills a player, Nemesis makes that mob the player's rival. It gets a random name,
never despawns, glows through walls and goes after its player whenever they come within 40 blocks.
Every time it kills the same player again it gains a level and a nastier title. If the player kills
it, they get revenge loot and the server is told. It is on [SpigotMC](https://www.spigotmc.org/resources/137297/).

## How it works

- A player has one nemesis at a time. Only mobs on the `eligible-mobs` list can become one; the
  default list has 18 types, from zombies and skeletons to ravagers and blazes.
- Stats are scaled from the mob's own base values. Each level adds 60% health and 40% damage, and
  from level 2 it gets 6% faster per level, up to 1.25x its normal speed.
- With the default config, levels 3 to 5 add Resistance, Regeneration, Strength, Speed, knockback
  resistance and iron or diamond gear. Level 5 is the cap until you raise `max-level`.
- It has Fire Resistance so sunlight and lava don't kill it, and it heals fully when it levels up.
- Killing your own nemesis drops 20 XP plus 15 per level, two diamonds per level and a named player
  head as a trophy. If it dies any other way, the rivalry ends.
- The player gets an action bar warning when their nemesis is within 18 blocks.

## Commands

```
/nemesis info     name, level, mob type and where it was last seen
/nemesis locate   last known position and how far away it is
/nemesis list     every active nemesis on the server
/nemesis reload   reload config.yml
```

`/nem` works too. `info` and `locate` are open to every player. `list` and `reload` need
`nemesis.admin`, which ops have by default.

## Config

Everything above is set in `config.yml`, including the stat scaling, the effects and gear for each
level, the revenge drops and every message. You can add levels past 5; a level without its own entry
uses the highest one defined below it. Broadcasts are off for a new nemesis and level ups and on for
revenge kills. Active nemeses are saved to `plugins/Nemesis/nemeses.yml` and survive restarts.

## Installing and building

Put the jar from the [releases page](https://github.com/Rex-Micky/Nemesis/releases) in `plugins/`
and restart. It needs Spigot or Paper 1.20.6 to 1.21.x on Java 21. I have tested it in-game on
1.20.6, 1.21.1, 1.21.4 and 1.21.11.

To build it yourself, run `mvn clean package` with JDK 21. The jar ends up in `target/`.

Questions and bug reports: [Discord](https://discord.gg/pfF5TpQ9UE) or the issue tracker here.
MIT licensed, see [LICENSE](LICENSE).
