package mythicbotany.functionalflora.base;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.registries.ForgeRegistries;
import org.moddingx.libx.base.tile.BlockBE;
import org.moddingx.libx.mod.ModX;
import org.moddingx.libx.registration.RegistrationContext;
import org.moddingx.libx.registration.SetupContext;
import vazkii.botania.common.block.BotaniaBlocks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockFunctionalFlower<T extends FunctionalFlowerBase> extends BlockBE<T> implements IPlantable {
    
    private static final VoxelShape SHAPE = box(4.8D, 0.0D, 4.8D, 12.8D, 16.0D, 12.8D);
    
    private final BlockFloatingFunctionalFlower<T> floatingBlock;
    public final boolean isGenerating;

    public BlockFunctionalFlower(ModX mod, Class<T> beClass, Properties properties, boolean isGenerating) {
        this(mod, beClass, properties, new Item.Properties(), isGenerating);
    }

    public BlockFunctionalFlower(ModX mod, Class<T> beClass, Properties properties, Item.Properties itemProperties, boolean isGenerating) {
        super(mod, beClass, properties.noCollission()
                .isRedstoneConductor((state, world, pos) -> false)
                .instabreak().sound(SoundType.GRASS)
                .offsetType(OffsetType.XZ), itemProperties);
        this.isGenerating = isGenerating;
        this.floatingBlock = new BlockFloatingFunctionalFlower<>(mod, beClass, this);
    }

    @Override
    public void registerAdditional(RegistrationContext ctx, EntryCollector builder) {
        super.registerAdditional(ctx, builder);
        builder.registerNamed(Registry.BLOCK_REGISTRY, "floating", this.floatingBlock);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void registerClient(SetupContext ctx) {
        ctx.enqueue(() -> BlockEntityRenderers.register(this.getBlockEntityType(), mgr -> new RenderFunctionalFlower<>()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAnalogOutputSignal(@Nonnull BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getAnalogOutputSignal(@Nonnull BlockState blockState, @Nonnull Level level, @Nonnull BlockPos pos) {
        FunctionalFlowerBase te = this.getBlockEntity(level, pos);
        if (te.getCurrentMana() > 0) {
            return 1 + (int) ((te.getCurrentMana() / (double) te.maxMana) * 14);
        } else {
            return 0;
        }
    }

    @SuppressWarnings("deprecation")
    @Nonnull
    public VoxelShape getShape(BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        Vec3 shift = state.getOffset(level, pos);
        return SHAPE.move(shift.x, shift.y, shift.z);
    }

    @SuppressWarnings("deprecation")
    @Nonnull
    public BlockState updateShape(BlockState state, @Nonnull Direction facing, @Nonnull BlockState facingState, @Nonnull LevelAccessor level, @Nonnull BlockPos currentPos, @Nonnull BlockPos facingPos) {
        return !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    protected boolean isValidGround(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.FARMLAND) || state.is(BotaniaBlocks.enchantedSoil)
                || state.is(Blocks.MYCELIUM) || state.canSustainPlant(level, pos, Direction.UP, this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(@Nonnull BlockState state, @Nonnull LevelReader level, BlockPos pos) {
        BlockPos blockpos = pos.below();
        return this.isValidGround(level.getBlockState(blockpos), level, blockpos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, @Nonnull BlockGetter reader, @Nonnull BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull PathComputationType type) {
        return type == PathComputationType.AIR && !this.hasCollision || super.isPathfindable(state, level, pos, type);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable BlockGetter level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (this.isGenerating) {
            tooltip.add(Component.translatable("botania.flowerType.generating").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("botania.flowerType.functional").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));

        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(this);
        if (id != null) {
            tooltip.add(Component.translatable("block." + id.getNamespace() + "." + id.getPath() + ".description").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean useShapeForLightOcclusion(@Nonnull BlockState state) {
        return true;
    }

    public BlockFloatingFunctionalFlower<T> getFloatingBlock() {
        return this.floatingBlock;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getPlant(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != this) return this.defaultBlockState();
        return state;
    }
}
