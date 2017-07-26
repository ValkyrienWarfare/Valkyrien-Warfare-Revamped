package ValkyrienWarfareBase.Physics;

import ValkyrienWarfareBase.PhysicsSettings;
import ValkyrienWarfareBase.API.RotationMatrices;
import ValkyrienWarfareBase.API.Vector;
import ValkyrienWarfareBase.PhysicsManagement.PhysicsObject;
import ValkyrienWarfareControl.NodeNetwork.IPhysicsProcessorNode;
import ValkyrienWarfareControl.NodeNetwork.Node;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class PhysicsCalculationsManualControl extends PhysicsCalculations {

	public double yawRate;
	public double forwardRate;
	public double upRate;

	public double setRoll = 0.01D;
	public double setPitch = 0.01D;

	public boolean useLinearMomentumForce;

	public PhysicsCalculationsManualControl(PhysicsObject toProcess) {
		super(toProcess);
	}

	@Override
	public void calculateForces() {
		double modifiedDrag = Math.pow(drag, physTickSpeed / .05D);
		linearMomentum.multiply(modifiedDrag);
		angularVelocity.multiply(modifiedDrag);

		if (PhysicsSettings.doPhysicsBlocks) {
			for(Node node : parent.nodesWithinShip) {
				TileEntity nodeTile = node.parentTile;
				if(nodeTile instanceof IPhysicsProcessorNode) {
//					System.out.println("test");
					((IPhysicsProcessorNode) nodeTile).onPhysicsTick(parent, this, physRawSpeed);
				}
			}
		}
	}

	@Override
	public void applyGravity() {

	}

	@Override
	public void rawPhysTickPostCol() {
		applyLinearVelocity();

		double previousYaw = parent.wrapper.yaw;

		applyAngularVelocity();

		//We don't want the up normal to exactly align with the world normal, it causes problems with collision
		parent.wrapper.pitch = setPitch;
		parent.wrapper.roll = setRoll;
		parent.wrapper.yaw = previousYaw;

		parent.wrapper.yaw -= (yawRate * physTickSpeed);

		double[] existingRotationMatrix = RotationMatrices.getRotationMatrix(0, parent.wrapper.yaw, 0);

		Vector linearForce = new Vector(forwardRate, upRate, 0, existingRotationMatrix);

		if(useLinearMomentumForce) {
			linearForce = new Vector(linearMomentum);
		}

		linearForce.multiply(physTickSpeed);

		parent.wrapper.posX += linearForce.X;
		parent.wrapper.posY += linearForce.Y;
		parent.wrapper.posZ += linearForce.Z;

		parent.coordTransform.updateAllTransforms();
	}

	@Override
	public void writeToNBTTag(NBTTagCompound compound) {
		super.writeToNBTTag(compound);
		compound.setDouble("yawRate", yawRate);
		compound.setDouble("forwardRate", forwardRate);
		compound.setDouble("upRate", upRate);
	}

	@Override
	public void readFromNBTTag(NBTTagCompound compound) {
		super.readFromNBTTag(compound);
		yawRate = compound.getDouble("yawRate");
//		forwardRate = compound.getDouble("forwardRate");
//		upRate = compound.getDouble("upRate");
	}

}