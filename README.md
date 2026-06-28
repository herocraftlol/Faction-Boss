# FactionBoss - Minecraft 1.21 Boss Plugin

Plugin de boss aléatoire pour serveur Survival Faction Minecraft 1.21.

## Fonctionnalités

- **Deux types de boss** : Zombie Boss et Squelette Boss
- **Spawn automatique** : Les boss spawn toutes les 45 minutes à 1 heure
- **Position médiane** : Les boss spawn au point central de tous les joueurs en ligne
- **Équipement personnalisé** : Armure en diamant enchantée et armes enchantées
- **Drops aléatoires** : 50% de chance pour une arme enchantée, 50% pour 5-10 diamants
- **Annonces de mort** : Message dans le chat avec le nom du tueur

## Installation

1. Téléchargez le JAR depuis la page des [Releases](https://github.com/abelliardadresse-alt/Faction-Boss/releases)
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

## Statistiques des Boss

### Zombie Boss
- **Vie** : 100 HP
- **Vitesse** : 0.35 (Speed II)
- **Arme** : Épée en diamant (Tranchant 3 + Mending)
- **Armure** : Full diamant (Protection 3 + Solidité 3)
- **Drops** : Épée enchantée (50%) ou 5-10 diamants (50%)

### Squelette Boss
- **Vie** : 80 HP
- **Vitesse** : 0.30 (Speed I)
- **Arme** : Arc (Puissance 2 + Mending)
- **Armure** : Full diamant (Protection 3 + Solidité 3)
- **Drops** : Arc enchanté (50%) ou 5-10 diamants (50%)

## Configuration

Aucune configuration nécessaire - le plugin fonctionne directement !

## Prérequis

- Minecraft 1.21
- Serveur Spigot ou Paper
- Java 21