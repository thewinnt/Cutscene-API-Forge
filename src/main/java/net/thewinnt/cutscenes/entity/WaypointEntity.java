package net.thewinnt.cutscenes.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class WaypointEntity extends Entity {
    public static final EntityDataAccessor<String> NAME = SynchedEntityData.defineId(WaypointEntity.class, EntityDataSerializers.STRING);

    public WaypointEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.firstTick = false;
    }

    @Override
    public void tick() {}

    @Override
    protected void defineSynchedData() {
        this.entityData.define(NAME, "undefined"); // we need the client to know 
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.entityData.set(NAME, nbt.getString("Name"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putString("Name", this.entityData.get(NAME));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public String getWaypointName() {
        return this.entityData.get(NAME);
    }
}
