# FactionBoss - Minecraft 1.21 Boss Plugin

Plugin de boss avancé pour serveur Survival Faction Minecraft 1.21 avec 6 types de boss uniques, pouvoirs spéciaux et système de regen désactivée.

## Fonctionnalités

- **6 types de boss** : Zombie, Squelette, Wither, Araignée, Blaze, Sorcière
- **Spawn automatique** : Les boss spawn toutes les 1h15
- **Position sécurisée** : Spawn à 50+ blocs de tous les joueurs
- **Pouvoirs spéciaux** : Chaque boss a un pouvoir unique qui s'active toutes les 60 secondes
- **Pas de régénération** : Les boss ne peuvent pas se soigner
- **500 XP** : Bonus d'expérience à la mort du boss
- **Drops uniques** : Chaque boss drop des objets spécifiques + récompense
- **Annonces de mort** : Message dans le chat avec le nom du tueur

## Installation

1. Téléchargez le JAR depuis la page des [Releases](https://github.com/herocraftlol/Faction-Boss/releases)
2. Placez le fichier `.jar` dans le dossier `plugins` de votre serveur
3. Redémarrez le serveur

## Compilation depuis les sources

### Avec Maven
```bash
mvn clean package
```
Le JAR sera dans `target/`

## Commandes

| Commande | Description | Permission |
|----------|-------------|------------|
| `/bossspawn` | Force le spawn d'un boss (admin) | `factionboss.admin` |
| `/bossinfo` | Affiche les infos du boss actuel | `factionboss.info` |

## Les 6 Boss

### Zombie Boss (150 HP)
- **Pouvoir** : Invoque des zombies serviteurs
- **Drops** : Épée Tranchant 4 + Mending ou 5-10 diamants

### Squelette Boss (120 HP)
- **Pouvoir** : Convoque des archers + éclairs
- **Drops** : Arc Power 3 + Punch 2 ou 5-10 diamants

### Wither Boss (200 HP)
- **Pouvoir** : Répand le Wither + Lenteur
- **Drops** : Tête de Wither + Épée Netherite Tranchant 5 ou lingots netherite

### Araignée Boss (100 HP)
- **Pouvoir** : Envoie des araignées + Poison + Cécité
- **Drops** : Trident Loyalty 3 ou émeraudes

### Blaze Boss (130 HP)
- **Pouvoir** : Flammes + éclairs + blazes
- **Drops** : Épée Fire Aspect 3 ou lingots d'or

### Sorcière Boss (110 HP)
- **Pouvoir** : Potions maléfiques + téléportation
- **Drops** : Bâton de Sorcière ou améthystes

## Prérequis

- Minecraft 1.21
- Serveur Spigot ou Paper
- Java 21