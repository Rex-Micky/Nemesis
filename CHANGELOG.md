# Changelog

All notable changes to Nemesis are documented here.
This project adheres to [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-07-13

Initial release.

### Added
- Mobs that kill a player are promoted into a named, glowing, persistent nemesis.
- Nemeses actively hunt their owner, with an action-bar warning when nearby.
- Level-up system (Lv.1–5 by default): each kill on the owner grants a new rank,
  scarier title, higher stats and new abilities.
- Revenge system: slaying your nemesis grants configurable loot, XP and a named trophy.
- Fully config-driven - stat scaling, per-level abilities (effects, gear, knockback
  resistance), reward drops, trophy and all messages. Supports adding custom levels.
- `/nemesis info`, `/nemesis locate`, `/nemesis list` and `/nemesis reload`.
- Persistent across restarts.

### Notes
- Stats scale from each mob's own natural values, so every mob type stays balanced.
- Baby mobs are promoted to adults so a nemesis is never unfairly fast or hard to see.
- The trophy defaults to a player head - a decorative keepsake, not a farmable resource.
