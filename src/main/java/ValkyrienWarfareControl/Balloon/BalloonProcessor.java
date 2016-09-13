package ValkyrienWarfareControl.Balloon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import ValkyrienWarfareBase.API.Vector;
import ValkyrienWarfareBase.PhysicsManagement.PhysicsWrapperEntity;
import gnu.trove.iterator.TIntIterator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public class BalloonProcessor {

	public PhysicsWrapperEntity parent;
	
	public HashSet<BlockPos> balloonWalls;
	public HashSet<BlockPos> internalAirPositions;
	public HashSet<BlockPos> balloonHoles;
	
	public int minX,minY,minZ,maxX,maxY,maxZ;
	
	public Vector currentBalloonCenter = new Vector();
	public int currentBalloonSize;
	
	public BalloonProcessor(PhysicsWrapperEntity parent,HashSet<BlockPos> balloonWalls,HashSet<BlockPos> internalAirPositons){
		this.parent = parent;
		this.balloonWalls = balloonWalls;
		this.internalAirPositions = internalAirPositons;
		balloonHoles = new HashSet<BlockPos>();
		updateBalloonCenter();
	}
	

	public void processBlockUpdates(ArrayList<BlockPos> updates){
		checkHolesForFix();
		for(BlockPos pos:updates){
			if(isBlockPosInRange(pos)){
				IBlockState state = parent.wrapping.VKChunkCache.getBlockState(pos);
				Block block = state.getBlock();
				if(block.blockMaterial.blocksMovement()){
					if(internalAirPositions.contains(block)){
						//No longer an air position
						internalAirPositions.remove(pos);
						balloonWalls.add(pos);
					}else{
						//Possibly add it to internalAirPositions?
						//Or maybe fill in a hole?
					}
					balloonHoles.remove(pos);
				}else{
					if(balloonWalls.contains(pos)){
						//Just created a hole
						System.out.println("Hole Created");
						balloonHoles.add(pos);
					}
				}
			}
		}
		updateBalloonCenter();
		updateBalloonRange();
	}
	
	public void checkHolesForFix(){

		ArrayList<BlockPos> balloonHoleCopy = new ArrayList<BlockPos>(balloonHoles);
		
		for(BlockPos pos:balloonHoleCopy){
			BlockPos[] adjacentPositions = getAdjacentPositions(pos);
			
			if(!balloonHoles.contains(pos)){
				break;
			}
			
			if(doFirstHoleCheck(pos,adjacentPositions)){
				balloonHoles.remove(pos);
				break;
			}
			if(doSecondHoleCheck(pos,adjacentPositions)){
				balloonHoles.remove(pos);
				break;
			}
			if(doLastHoleCheck(pos,adjacentPositions)){
				balloonHoles.remove(pos);
				break;
			}
			//continue the condition check
		}
		System.out.println("balloonHoles size is "+balloonHoles.size());
	}
	
	//Just check if the hole is even connected to the internal air of the ballon; if not, get rid of it!
	//return true if you want to verify hole has been fixed (or if you just want it to be removed)
	private boolean doFirstHoleCheck(BlockPos holeToCheck,BlockPos[] adjacentPositions){
		for(BlockPos nearbyPosition:adjacentPositions){
			if(/*balloonWalls.contains(nearbyPosition)||*/internalAirPositions.contains(nearbyPosition)/*||balloonHoles.contains(nearbyPosition)*/){
				//Connected to balloon, go onto next check
				return false;
			}
		}
		//Not connected, hole should be removed
		return true;
	}
	
	//This checks if the adjacentPositions are completely filled
	private boolean doSecondHoleCheck(BlockPos holeToCheck,BlockPos[] adjacentPositions){
		for(BlockPos nearbyPosition:adjacentPositions){
			if(!(balloonWalls.contains(nearbyPosition)||internalAirPositions.contains(nearbyPosition))){
				//A nearby position isnt included in the balloon, KEEP GOING!
				return false;
				
				//TODO: Maybe re-add this shit, probably not though
//				IBlockState nearbyState = parent.wrapping.VKChunkCache.getBlockState(nearbyPosition);
//				if(nearbyState.getBlock().blockMaterial.blocksMovement()){
//					
//				}
			}
		}
		return true;
	}
	
	private boolean doLastHoleCheck(BlockPos holeToCheck,BlockPos[] adjacentPositions){
		BalloonHoleDetector holeDetector = new BalloonHoleDetector(holeToCheck, parent.worldObj, 500, this);
		if(!holeDetector.cleanHouse){
			//Wow the hole is actually filled! Add the new positions here!
			
			TIntIterator newBallonWallIterator = holeDetector.newBalloonWalls.iterator();
			TIntIterator newAirPostitionsIterator = holeDetector.foundSet.iterator();
			
			while(newAirPostitionsIterator.hasNext()){
				int hash = newAirPostitionsIterator.next();
				BlockPos fromHash = holeDetector.getPosWithRespectTo(hash, holeDetector.firstBlock);
				internalAirPositions.add(fromHash);
				balloonHoles.remove(fromHash);
			}
			
			while(newBallonWallIterator.hasNext()){
				int hash = newBallonWallIterator.next();
				BlockPos fromHash = holeDetector.getPosWithRespectTo(hash, holeDetector.firstBlock);
				balloonWalls.add(fromHash);
				balloonHoles.remove(fromHash);
			}
			
			return true;
		}
		return false;
	}
	
	private BlockPos[] getAdjacentPositions(BlockPos pos){
		BlockPos up = pos.up();
		BlockPos down = pos.down();
		BlockPos north = pos.north();
		BlockPos east = pos.east();
		BlockPos south = pos.south();
		BlockPos west = pos.west();
		
		BlockPos[] positions = new BlockPos[6];
		
		positions[0] = up;
		positions[1] = down;
		positions[2] = north;
		positions[3] = east;
		positions[4] = south;
		positions[5] = west;
		
		return positions;
	}
	
	public void updateBalloonCenter(){
		currentBalloonCenter.zero();
		currentBalloonSize = internalAirPositions.size();
		Iterator<BlockPos> blockPosIterator = internalAirPositions.iterator();
		while(blockPosIterator.hasNext()){
			BlockPos current = blockPosIterator.next();
			currentBalloonCenter.X+=current.getX();
			currentBalloonCenter.Y+=current.getY();
			currentBalloonCenter.Z+=current.getZ();
		}
		currentBalloonCenter.multiply(1D/currentBalloonSize);
		currentBalloonCenter.X+=.5D;currentBalloonCenter.Y+=.5D;currentBalloonCenter.Z+=.5D;
	}
	
	public void updateBalloonRange(){
		Iterator<BlockPos> blockPosIterator = balloonWalls.iterator();
		
		BlockPos firstPos = blockPosIterator.next();
		
		minX = maxX = firstPos.getX();
		minY = maxY = firstPos.getY();
		minZ = maxZ = firstPos.getZ();
		
		while(blockPosIterator.hasNext()){
			BlockPos pos = blockPosIterator.next();
			minX = Math.min(minX, pos.getX());minY = Math.min(minY, pos.getY());minZ = Math.min(minZ, pos.getZ());
			maxX = Math.max(maxX, pos.getX());maxY = Math.max(maxY, pos.getY());maxZ = Math.max(maxZ, pos.getZ());
		}
	}
	
	//A fast way to rule out most block positions when looking through the HashSets
	public boolean isBlockPosInRange(BlockPos toCheck){
		if(toCheck.getX()>=minX && toCheck.getX()<=maxX){
			if(toCheck.getY()>=minY && toCheck.getY()<=maxY){
				if(toCheck.getZ()>=minZ && toCheck.getZ()<=maxZ){
					return true;
				}
			}
		}
		return false;
	}
	
	public static BalloonProcessor makeProcessorForDetector(PhysicsWrapperEntity wrapper,BalloonDetector detector){
		TIntIterator ballonWallIterator = detector.balloonWalls.iterator();
		TIntIterator airPostitionsIterator = detector.foundSet.iterator();
		
		HashSet<BlockPos> staticBalloonWalls = new HashSet<BlockPos>();
		HashSet<BlockPos> staticInternalPositions = new HashSet<BlockPos>();
		
		int minX,maxX,minY,maxY,minZ,maxZ;
		
		minX = maxX = detector.firstBlock.getX();
		minY = maxY = detector.firstBlock.getY();
		minZ = maxZ = detector.firstBlock.getZ();
		
		while(ballonWallIterator.hasNext()){
			int hash = ballonWallIterator.next();
			BlockPos fromHash = detector.getPosWithRespectTo(hash, detector.firstBlock);
			staticBalloonWalls.add(fromHash);
			
			minX = Math.min(minX, fromHash.getX());minY = Math.min(minY, fromHash.getY());minZ = Math.min(minZ, fromHash.getZ());
			maxX = Math.max(maxX, fromHash.getX());maxY = Math.max(maxY, fromHash.getY());maxZ = Math.max(maxZ, fromHash.getZ());
		}
		
		while(airPostitionsIterator.hasNext()){
			int hash = airPostitionsIterator.next();
			BlockPos fromHash = detector.getPosWithRespectTo(hash, detector.firstBlock);
			staticInternalPositions.add(fromHash);
		}
		
		BalloonProcessor toReturn = new BalloonProcessor(wrapper,staticBalloonWalls,staticInternalPositions);
		
		toReturn.minX = minX;toReturn.minY = minY;toReturn.minZ = minZ;
		toReturn.maxX = maxX;toReturn.maxY = maxY;toReturn.maxZ = maxZ;
		
		return toReturn;
	}
	
}