# FactionBosses - Minecraft 1.21 Boss Plugin

A Minecraft Spigot/Paper plugin that spawns bosses at the median location of all online players.

## Features

- **Two Boss Types**: Zombie Boss and Skeleton Boss
- **Automatic Spawning**: Bosses spawn automatically every 45-60 minutes
- **Median Location**: Bosses spawn at the center point of all online players
- **Custom Equipment**: Enchanted diamond armor and weapons
- **Random Drops**: 35% chance for enchanted weapon, 65% chance for 5-10 diamonds
- **Death Announcements**: Chat message when a boss is killed with killer's name

## Installation

1. Download the latest release JAR from the [Releases](https://github.com/abelliardadresse-alt/Faction-Boss/releases) page
2. Place the `.jar` file in your server's `plugins` folder
3. Restart your server

## Building from Source

### With Maven
```bash
mvn clean package
```
JAR will be in `target/`

### With Gradle
```bash
./gradlew build
```
JAR will be in `build/libs/`

## Commands

| Command | Description |
|---------|-------------|
| `/spawnboss` | Spawn a Zombie Boss at median player location |
| `/spawnboss zombie` | Spawn a Zombie Boss |
| `/spawnboss skeleton` | Spawn a Skeleton Boss |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `factionbosses.spawn` | Allow spawning bosses | OP |

## Boss Stats

### Zombie Boss
- **Health**: 100 HP
- **Speed**: 0.25 (Speed II effect)
- **Weapon**: Diamond Sword (Sharpness 3 + Mending)
- **Armor**: Full Diamond (Protection 2)
- **Drops**: Enchanted Sword (35%) or 5-10 Diamonds (65%)

### Skeleton Boss
- **Health**: 80 HP
- **Speed**: 0.23 (Speed I effect)
- **Weapon**: Bow (Power 2 + Mending)
- **Armor**: Full Diamond (Protection 2)
- **Drops**: Enchanted Bow (35%) or 5-10 Diamonds (65%)

## Configuration

Edit `plugins/FactionBosses/config.yml`:

```yaml
auto-spawn:
  enabled: true
  min-interval-minutes: 45
  max-interval-minutes: 60
```

## Requirements

- Minecraft 1.21
- Spigot or Paper server
- Java 21