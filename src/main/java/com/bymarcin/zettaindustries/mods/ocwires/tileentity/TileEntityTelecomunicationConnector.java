package com.bymarcin.zettaindustries.mods.ocwires.tileentity;

import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.TileEntityImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.util.Utils;
import com.bymarcin.zettaindustries.ZettaIndustries;
import com.bymarcin.zettaindustries.mods.ocwires.TelecommunicationWireType;
import com.google.common.collect.Iterables;
import joptsimple.internal.Strings;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Set;


public class TileEntityTelecomunicationConnector extends TileEntityImmersiveConnectable implements Environment, SidedEnvironment, ITickable, IEBlockInterfaces.IDirectionalTile, IEBlockInterfaces.IBlockBounds {
    protected Node node;
    private boolean needUpdate = false;
	public boolean isStateLoaded = false;
	public EnumFacing f = EnumFacing.NORTH;

    public TileEntityTelecomunicationConnector() {
    	node = Network.newNode(this, Visibility.None).create();
	}

	@Override
	public float[] getBlockBounds() {
		float length = .51f;
		float wMin = .3125f;
		float wMax = .6875f;
		switch(f.getOpposite() )
		{
			case UP:
				return new float[]{wMin,0,wMin,  wMax,length,wMax};
			case DOWN:
				return new float[]{wMin,1-length,wMin,  wMax,1,wMax};
			case SOUTH:
				return new float[]{wMin,wMin,0,  wMax,wMax,length};
			case NORTH:
				return new float[]{wMin,wMin,1-length,  wMax,wMax,1};
			case EAST:
				return new float[]{0,wMin,wMin,  length,wMax,wMax};
			case WEST:
				return new float[]{1-length,wMin,wMin,  1,wMax,wMax};
		}
		return new float[]{0,0,0,1,1,1};
	}

	@Override
	public int hashCode() {
		return pos.hashCode();
	}

	@Override
	public void setFacing(EnumFacing enumFacing) {
		f = enumFacing;
	}

	@Override
	public int getFacingLimitation() {
		return 0;
	}

	@Override
	public boolean mirrorFacingOnPlacement(EntityLivingBase entityLivingBase) {
		return true;
	}

	@Override
	public boolean canHammerRotate(EnumFacing enumFacing, float v, float v1, float v2, EntityLivingBase entityLivingBase) {
		return false;
	}

	@Override
	public boolean canRotate(EnumFacing axis) {
		return false;
	}

	@Override
	public EnumFacing getFacing() {
		return f;
	}

	@Override
	public boolean canConnectCable(WireType wiretype, TargetingInfo target) {
		return TelecommunicationWireType.TELECOMMUNICATION.getUniqueName().equals(wiretype.getUniqueName());
	}

	@Override
	public void connectCable(WireType wireType, TargetingInfo targetingInfo, IImmersiveConnectable iImmersiveConnectable) {
		checkConnections();
	}

	@Override
	public void removeCable(Connection con) {
		if(con==null){
			ZettaIndustries.logger.warn("Try to removed empty connection.");
			return;
		}
		if(con.start.equals(Utils.toCC(this))){
			if(PosToTileEntity(con.end)!=null && node!=null){
				node.disconnect(PosToTileEntity(con.end).node());
				//System.out.println("Disconnecting:  " + CCToTileEntity(con.end) + " --FROM-- " + this);
			}
		}
		//getWorld().markBlockForUpdate(xCoord, yCoord, zCoord);
	}
	
	public void checkConnections(){
		if (node.network() == null) {
			ZettaIndustries.logger.error("attempted to update connections before network created!");
			return;
		}

		Set<Connection> a = ImmersiveNetHandler.INSTANCE.getConnections(getWorld(), Utils.toCC(this));
		if(a==null)return;
		for(Connection s : a){
			if(s.start.equals(Utils.toCC(this)) && PosToTileEntity(s.end)!=null){
				TileEntityTelecomunicationConnector te = PosToTileEntity(s.end);
				if (!te.isStateLoaded) continue; // do not connect until nbt loads to allow addresses to be created and prevent duplication

				Node n = te.node();
				if (n == null) continue;

				li.cil.oc.api.network.Network net = n.network();
				if(net != null && !node.isNeighborOf(n)){
					// after chunk reload, we need to remove the stale node entry from the peer's network
					String address = node.address();
					if (!Strings.isNullOrEmpty(address)) {
						Node stale = net.node(address);
						if (stale != null) stale.remove();
					}

					node.connect(n);
					//System.out.println( Utils.toCC(this).equals(s.start)+ " +++ "+ CCToTileEntity(s.start) + "-k-" + CCToTileEntity(s.end));

//					int size = Iterables.size(this.node.network().nodes());
//					ZettaIndustries.logger.info("connections added, new total whole-net node count: {}", size);
				}
			}
		}
	}
	
	public TileEntityTelecomunicationConnector PosToTileEntity(BlockPos pos){
		TileEntity te  = getWorld().getTileEntity(pos);
		if(te instanceof TileEntityTelecomunicationConnector){
			return (TileEntityTelecomunicationConnector)te;
		}else{
			return null;
		}
	}
	
	@Override
	public Vec3d getRaytraceOffset(IImmersiveConnectable arg0) {
		EnumFacing fd = getFacing().getOpposite();
		return new Vec3d(.5+fd.getXOffset()*.0625, .5+fd.getYOffset()*.0625, .5+fd.getZOffset()*.0625);
	}
	
	@Override
	public Vec3d getConnectionOffset(Connection con)
	{
		EnumFacing fd = getFacing().getOpposite();
		double conRadius = .03125;
		return new Vec3d(.5-conRadius*fd.getXOffset(), .5-conRadius*fd.getYOffset(), .5-conRadius*fd.getZOffset());
	}

	@Override
	public WireType getCableLimiter(TargetingInfo arg0) {
		return TelecommunicationWireType.TELECOMMUNICATION;
	}
	
	@Override
	public boolean isEnergyOutput() {
		return false;
	}

	@Override
	public int outputEnergy(int arg0, boolean arg1, int arg2) {
		return 0;
	}

	@Override
	public BlockPos getConnectionMaster(WireType wireType, TargetingInfo targetingInfo) {
		return getPos();
	}

	@Override
	public boolean canConnect() {
		return true;
	}
	
	
	// OC
	@Override
	public Node node() {
		return node;
	}

	@Override
	public void onConnect(Node node) {
		if(node == node())
			needUpdate = true;
	}

	@Override
	public void onDisconnect(Node node) {
			
	}

	@Override
	public void onMessage(Message message) {
		
	}

    @Override
    public void update() {
    	if(getWorld().isRemote || node == null) return;

        if (node.network() == null) {
            Network.joinOrCreateNetwork(this);
        }

        if(needUpdate) {
			checkConnections();
			needUpdate = false;
		}
    }

//    @Override
//    public void onChunkUnload() {
//        super.onChunkUnload();
//		ZettaIndustries.logger.info("unloading chunk");
//
//        // Make sure to remove the node from its network when its environment,
//        // meaning this tile entity, gets unloaded.
//        if (node != null) node.remove();
//    }

//    @Override
//    public void invalidate() {
//        super.invalidate();
//
//		ZettaIndustries.logger.info(this.blockType != null);
//		ZettaIndustries.logger.info("invalidating");
//
//        // Make sure to remove the node from its network when its environment,
//        // meaning this tile entity, gets unloaded.
//        if (node != null) node.remove();
//    }

    // ----------------------------------------------------------------------- //

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket) {
		super.readCustomNBT(nbt, descPacket);
		f = EnumFacing.byIndex(nbt.getInteger("facing"));
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket) {
		super.writeCustomNBT(nbt, descPacket);
		nbt.setInteger("facing", f.getIndex());
	}
	

    @Override
    public void readFromNBT(final NBTTagCompound nbt) {
        super.readFromNBT(nbt);
		f = EnumFacing.byIndex(nbt.getInteger("facing"));

        // The host check may be superfluous for you. It's just there to allow
        // some special cases, where getNode() returns some node managed by
        // some other instance (for example when you have multiple internal
        // nodes in this tile entity).
        if (node != null && node.host() == this) {
            // This restores the node's address, which is required for networks
            // to continue working without interruption across loads. If the
            // node is a power connector this is also required to restore the
            // internal energy buffer of the node.
            node.load(nbt.getCompoundTag("oc:node"));
        }

		isStateLoaded = true;
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound nbt) {
        super.writeToNBT(nbt);
		nbt.setInteger("facing", f.getIndex());

        // See readFromNBT() regarding host check.
        if (node != null && node.host() == this) {
            final NBTTagCompound nodeNbt = new NBTTagCompound();
            node.save(nodeNbt);
            nbt.setTag("oc:node", nodeNbt);
        }
        return nbt;
    }

	@Override
	public Node sidedNode(EnumFacing side) {
		return node();
	}

	@Override
	public boolean canConnect(EnumFacing side) {
		return side == f;
	}

	@Override
	public boolean allowEnergyToPass(Connection arg0) {
		return false;
	}

	@Override
	public void onEnergyPassthrough(int arg0) {

	}


}
