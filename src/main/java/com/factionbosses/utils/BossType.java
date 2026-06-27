package com.factionbosses.utils;

import org.bukkit.entity.EntityType;

public enum BossType {
    ZOMBIE(EntityType.ZOMBIE),
    SKELETON(EntityType.SKELETON);

    private final EntityType entityType;

    BossType(EntityType entityType) {
        this.entityType = entityType;
    }

    public EntityType getEntityType() {
        return entityType;
    }
}