package org.valkyrienskies.mod.common.tileentity;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public class TileEntityWaterPump extends TileEntity implements ITickable {

    private final double pumpRadius;

    public TileEntityWaterPump() {
        this.pumpRadius = 5;
    }

    @Override
    public void update() {
        final AxisAlignedBB pumpRangeBB = new AxisAlignedBB(pos, pos).grow(pumpRadius);
        final List<Entity> entitiesInPumpRadius = world.getEntitiesWithinAABBExcludingEntity(null, pumpRangeBB);

        for (final Entity entity : entitiesInPumpRadius) {
            // System.out.println("entity " + entity.getClass() + " in pump range!");
        }
    }
}
