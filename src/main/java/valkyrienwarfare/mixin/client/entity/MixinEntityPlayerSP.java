package valkyrienwarfare.mixin.client.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import valkyrienwarfare.ValkyrienWarfareMod;
import valkyrienwarfare.mod.coordinates.ISubspacedEntity;
import valkyrienwarfare.mod.coordinates.ISubspacedEntityRecord;
import valkyrienwarfare.mod.network.SubspacedEntityRecordMessage;
import valkyrienwarfare.mod.physmanagement.interaction.IDraggable;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

	private final ISubspacedEntity thisAsSubspaced = ISubspacedEntity.class.cast(this);
	private final EntityPlayerSP player = EntityPlayerSP.class.cast(this);
	
	/**
	 * This method is to send the position of the player relative to the subspace
	 * its on. Specifically sent right before the game regularly sends the player
	 * position update to the server.
	 * 
	 * @param info
	 */
	// @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
	// Disabled for now since we're using the @Overwrite
	private void preOnUpdateWalkingPlayer(CallbackInfo info) {
		IDraggable draggable = IDraggable.class.cast(this);
		if (draggable.getWorldBelowFeet() != null) {
			draggable.getWorldBelowFeet().getPhysicsObject().getSubspace().snapshotSubspacedEntity(thisAsSubspaced);
			ISubspacedEntityRecord entityRecord = draggable.getWorldBelowFeet().getPhysicsObject().getSubspace()
					.getRecordForSubspacedEntity(thisAsSubspaced);
			SubspacedEntityRecordMessage recordMessage = new SubspacedEntityRecordMessage(entityRecord);
			ValkyrienWarfareMod.physWrapperNetwork.sendToServer(recordMessage);
		}
	}
	
	@Shadow
    private boolean serverSneakState;
    @Shadow
    private boolean serverSprintState;
    @Shadow
    private double lastReportedPosX;
    @Shadow
    private double lastReportedPosY;
    @Shadow
    private double lastReportedPosZ;
    @Shadow
    private float lastReportedYaw;
    @Shadow
    private float lastReportedPitch;
	@Shadow
    private int positionUpdateTicks;
	@Shadow
    private boolean prevOnGround;
	@Shadow
    private boolean autoJumpEnabled;
	@Shadow
    protected Minecraft mc;
	
	// @reason is because we need to ensure the CPacketPlayer is always sent no matter what.
	@Overwrite
    private void onUpdateWalkingPlayer()
    {
		// ===== Injection code starts here =====
		
		IDraggable draggable = IDraggable.class.cast(this);
		if (draggable.getWorldBelowFeet() != null) {
			draggable.getWorldBelowFeet().getPhysicsObject().getSubspace().snapshotSubspacedEntity(thisAsSubspaced);
			ISubspacedEntityRecord entityRecord = draggable.getWorldBelowFeet().getPhysicsObject().getSubspace()
					.getRecordForSubspacedEntity(thisAsSubspaced);
			SubspacedEntityRecordMessage recordMessage = new SubspacedEntityRecordMessage(entityRecord);
			ValkyrienWarfareMod.physWrapperNetwork.sendToServer(recordMessage);
		}
		
		// ===== Injection code ends here =====
		
        boolean flag = player.isSprinting();

        if (flag != serverSprintState)
        {
            if (flag)
            {
            	player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_SPRINTING));
            }
            else
            {
            	player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SPRINTING));
            }

            serverSprintState = flag;
        }

        boolean flag1 = player.isSneaking();

        if (flag1 != serverSneakState)
        {
            if (flag1)
            {
            	player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING));
            }
            else
            {
            	player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING));
            }

            serverSneakState = flag1;
        }

        if (isCurrentViewEntity())
        {
            AxisAlignedBB axisalignedbb = player.getEntityBoundingBox();
            double d0 = player.posX - lastReportedPosX;
            double d1 = axisalignedbb.minY - lastReportedPosY;
            double d2 = player.posZ - lastReportedPosZ;
            double d3 = (double)(player.rotationYaw - lastReportedYaw);
            double d4 = (double)(player.rotationPitch - lastReportedPitch);
            ++positionUpdateTicks;
            // Always true because why not.
            boolean flag2 = true; // d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || positionUpdateTicks >= 20;
            boolean flag3 = true; // d3 != 0.0D || d4 != 0.0D;

            if (player.isRiding())
            {
            	player.connection.sendPacket(new CPacketPlayer.PositionRotation(player.motionX, -999.0D, player.motionZ, player.rotationYaw, player.rotationPitch, player.onGround));
                flag2 = false;
            }
            else if (flag2 && flag3)
            {
            	player.connection.sendPacket(new CPacketPlayer.PositionRotation(player.posX, axisalignedbb.minY, player.posZ, player.rotationYaw, player.rotationPitch, player.onGround));
            }
            else if (flag2)
            {
            	player.connection.sendPacket(new CPacketPlayer.Position(player.posX, axisalignedbb.minY, player.posZ, player.onGround));
            }
            else if (flag3)
            {
            	player.connection.sendPacket(new CPacketPlayer.Rotation(player.rotationYaw, player.rotationPitch, player.onGround));
            }

            if (flag2)
            {
            	lastReportedPosX = player.posX;
                lastReportedPosY = axisalignedbb.minY;
                lastReportedPosZ = player.posZ;
                positionUpdateTicks = 0;
            }

            if (flag3)
            {
            	lastReportedYaw = player.rotationYaw;
            	lastReportedPitch = player.rotationPitch;
            }

            prevOnGround = player.onGround;
            autoJumpEnabled = mc.gameSettings.autoJump;
        }
    }
	
	@Shadow
    protected boolean isCurrentViewEntity() {
		return false;
	}
}