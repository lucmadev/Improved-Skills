# Improved-Skills

Extension plugin for [openRPG](https://github.com/lucmadev/openRPG). Provides over 100 skills across three classes (Warrior, Mage, Assassin) registered programmatically into openRPG's talent tree system.

## Features

- 100+ skills defined in YAML (no recompilation needed to add or modify skills)
- Automatic registration into openRPG via API bridge
- Fallback: manual export to `plugins/openRPG/skills.yml`

## Requirements

- Paper 26.2+
- [openRPG](https://github.com/lucmadev/openRPG) plugin loaded before Improved-Skills

## Build

```bash
./gradlew build
```

Output JAR is in `build/libs/`.

## Usage

Place the built JAR in your server's `plugins/` folder alongside openRPG. Skills are loaded automatically on server start.
