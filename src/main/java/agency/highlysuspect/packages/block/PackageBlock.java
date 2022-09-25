package agency.highlysuspect.packages.block;

import agency.highlysuspect.packages.junk.PackageContainer;
import agency.highlysuspect.packages.junk.TwelveDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;

public class PackageBlock extends Block implements EntityBlock {
	public PackageBlock(Properties settings) {
		super(settings);
		
		registerDefaultState(defaultBlockState().setValue(FACING, TwelveDirection.NORTH));
	}
	
	//States.
	public static final Property<TwelveDirection> FACING = EnumProperty.create("facing", TwelveDirection.class);
	
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		TwelveDirection facing;
		
		if(ctx.getPlayer() == null) facing = TwelveDirection.fromDirection(ctx.getClickedFace());
		else facing = TwelveDirection.fromEntity(ctx.getPlayer()).getOpposite();
		
		return defaultBlockState().setValue(FACING, facing);
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}
	
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return PBlockEntityTypes.PACKAGE.get().create(pos, state);
	}
	
	//Behaviors.
	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}
	
	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if(!(blockEntity instanceof PackageBlockEntity pkg)) return;
		
		if(stack.hasCustomHoverName()) {
			pkg.setCustomName(stack.getHoverName());
		}
		
		if(world.isClientSide) {
			//Load the tag clientside. Fixes some flickering (wrong style/count) when placing the item.
			//Kinda surprised the game doesn't do this itself; it explicitly only does this server-side.
			CompoundTag blockEntityTag = BlockItem.getBlockEntityData(stack);
			if(blockEntityTag != null) pkg.load(blockEntityTag);
		}
	}
	
	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}
	
	@Override
	public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos));
	}
	
	@Override
	public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		super.playerWillDestroy(world, pos, state, player);
		
		if(!world.isClientSide && player.isCreative()) {
			//Spawn a drop, even in creative mode. This echoes what shulker boxes do.
			getDrops(state, (ServerLevel) world, pos, world.getBlockEntity(pos)).forEach(s -> {
				ItemEntity ent = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
				ent.setDefaultPickUpDelay();
				ent.setDeltaMovement(0, 0, 0);
				world.addFreshEntity(ent);
			});
		}
	}
	
	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		//Don't ask me why the cast is there but Mojang does it too
		if(world.getBlockEntity(pos) instanceof PackageBlockEntity) world.updateNeighbourForOutputSignal(pos, this);
		
		super.onRemove(state, world, pos, newState, moved);
	}
	
	//middle-click pick block, without holding Ctrl in creative
	@Override
	public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
		ItemStack stack = super.getCloneItemStack(world, pos, state);
		
		if(world.getBlockEntity(pos) instanceof PackageBlockEntity be) {
			//just copy the style, not the contents (like shulker boxes). Vanilla ctrl-pick will handle the other case.
			be.getStyle().writeToStackTag(stack);
			new PackageContainer().writeToStackTag(stack); //but write an empty tag anyway, so it stacks with block drops :thinking:
		}
		
		return stack;
	}
}
