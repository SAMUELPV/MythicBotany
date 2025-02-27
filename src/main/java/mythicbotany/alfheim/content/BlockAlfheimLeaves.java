package mythicbotany.alfheim.content;

import mythicbotany.register.tags.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.moddingx.libx.mod.ModX;
import org.moddingx.libx.registration.Registerable;
import org.moddingx.libx.registration.RegistrationContext;
import org.moddingx.libx.util.data.TagAccess;

import javax.annotation.Nonnull;

public class BlockAlfheimLeaves extends LeavesBlock implements Registerable {

    protected final ModX mod;
    private final Item item;

    public BlockAlfheimLeaves(ModX mod) {
        this(mod, new net.minecraft.world.item.Item.Properties());
    }

    public BlockAlfheimLeaves(ModX mod, net.minecraft.world.item.Item.Properties itemProperties) {
        super(BlockBehaviour.Properties.of(Material.LEAVES).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion().isValidSpawn((a, b, c, d) -> false).isSuffocating((a, b, c) -> false).isViewBlocking((a, b, c) -> false));
        this.mod = mod;
        if (mod.tab != null) {
            itemProperties.tab(mod.tab);
        }
        this.item = new BlockItem(this, itemProperties);
    }

    @Override
    public void registerAdditional(RegistrationContext ctx, EntryCollector builder) {
        builder.register(Registry.ITEM_REGISTRY, this.item);
    }

    @Nonnull
    @Override
    public BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction facing, @Nonnull BlockState facingState, @Nonnull LevelAccessor level, @Nonnull BlockPos currentPos, @Nonnull BlockPos facingPos) {
        int distance = this.getDistanceAt(facingState) + 1;
        if (distance != 1 || state.getValue(DISTANCE) != distance) {
            level.scheduleTick(currentPos, this, 1);
        }
        return state;
    }

    @Override
    public void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        level.setBlock(pos, this.updateDistance(state, level, pos), 3);
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return this.updateDistance(this.defaultBlockState().setValue(PERSISTENT, true), context.getLevel(), context.getClickedPos());
    }

    protected int getDistanceAt(BlockState neighbor) {
        if (TagAccess.ROOT.has(ModBlockTags.ALFHEIM_LOGS, neighbor.getBlock())) {
            return 0;
        } else {
            return neighbor.getBlock() instanceof LeavesBlock ? neighbor.getValue(DISTANCE) : 7;
        }
    }

    protected BlockState updateDistance(BlockState state, LevelAccessor level, BlockPos pos) {
        int distance = 7;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            mpos.setWithOffset(pos, dir);
            distance = Math.min(distance, this.getDistanceAt(level.getBlockState(mpos)) + 1);
            if (distance == 1) {
                break;
            }
        }
        return state.setValue(DISTANCE, distance);
    }
}
