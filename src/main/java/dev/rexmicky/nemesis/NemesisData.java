package dev.rexmicky.nemesis;

import org.bukkit.Location;

import java.util.UUID;

/** Persistent record of one player's active nemesis. */
public class NemesisData {

    private final UUID ownerUUID;
    private final String ownerName;
    private UUID entityUUID;
    private final String mobType;
    private final String baseName;
    private int level;
    private final long createdAt;

    // last-seen location (for /nemesis locate)
    private String world;
    private double x, y, z;

    public NemesisData(UUID ownerUUID, String ownerName, UUID entityUUID, String mobType,
                       String baseName, int level, long createdAt) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.entityUUID = entityUUID;
        this.mobType = mobType;
        this.baseName = baseName;
        this.level = level;
        this.createdAt = createdAt;
    }

    public UUID getOwnerUUID()      { return ownerUUID; }
    public String getOwnerName()    { return ownerName; }
    public UUID getEntityUUID()     { return entityUUID; }
    public void setEntityUUID(UUID id) { this.entityUUID = id; }
    public String getMobType()      { return mobType; }
    public String getBaseName()     { return baseName; }
    public int getLevel()           { return level; }
    public void setLevel(int level) { this.level = level; }
    public long getCreatedAt()      { return createdAt; }

    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public void setLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        this.world = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
    }

    /** Restore last-seen location straight from saved values. */
    public void setLocationRaw(String world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
