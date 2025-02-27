package mythicbotany.mjoellnir;

import mythicbotany.register.ModBlocks;
import mythicbotany.MythicBotany;
import mythicbotany.config.MythicConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.moddingx.libx.base.tile.BlockBE;
import org.moddingx.libx.mod.ModX;
import org.moddingx.libx.registration.Registerable;
import org.moddingx.libx.registration.RegistrationContext;
import org.moddingx.libx.registration.SetupContext;
import vazkii.botania.common.item.relic.RingOfThorItem;

import javax.annotation.Nonnull;
import java.util.List;

public class BlockMjoellnir extends BlockBE<TileMjoellnir> implements Registerable {

    public static final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 3, 16, 9, 13),
            box(7, 9, 7, 9, 22, 9)
    );
    
    private final ItemMjoellnir item;
    private final EntityType<Mjoellnir> entityType;

    public BlockMjoellnir(ModX mod, Properties properties, Item.Properties itemProperties) {
        super(mod, TileMjoellnir.class, properties, null);
        this.item = new ItemMjoellnir(this, itemProperties);
        this.entityType = EntityType.Builder.<Mjoellnir>of(Mjoellnir::new, MobCategory.MISC).sized(0.6f, 0.9f).clientTrackingRange(20).build(MythicBotany.getInstance().modid + "_mjoellnir");
    }

    public EntityType<Mjoellnir> getEntityType() {
        return this.entityType;
    }

    @Override
    public void registerAdditional(RegistrationContext ctx, EntryCollector builder) {
        super.registerAdditional(ctx, builder);
        builder.register(Registry.ITEM_REGISTRY, this.item);
        builder.register(Registry.ENTITY_TYPE_REGISTRY, this.entityType);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void registerClient(SetupContext ctx) {
        ctx.enqueue(() -> BlockEntityRenderers.register(this.getBlockEntityType(), mgr -> new RenderMjoellnir()));
        ctx.enqueue(() -> EntityRenderers.register(this.entityType, RenderEntityMjoellnir::new));
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (!level.isClientSide) {
            if (canHold(player)) {
                TileMjoellnir tile = this.getBlockEntity(level, pos);
                if (putInInventory(player, tile.getStack().copy(), getHotbarSlot(player, hand))) {
                    level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
                } else {
                    return InteractionResult.FAIL;
                }
            } else {
                player.sendSystemMessage(Component.translatable("message.mythicbotany.mjoellnir_heavy_pick").withStyle(ChatFormatting.GRAY));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    
    public static boolean placeInWorld(ItemStack stack, Level level, BlockPos pos) {
        return placeInWorld(stack, level, pos, true);
    }
    
    public static boolean placeInWorld(ItemStack stack, Level level, BlockPos pos, boolean dropOld) {
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (state.getBlock() != ModBlocks.mjoellnir && ((hardness >= 0 && hardness <= 60) || state.getMaterial().isReplaceable())) {
            List<ItemStack> drops = null;
            if (dropOld && level instanceof ServerLevel) {
                drops = Block.getDrops(state, (ServerLevel) level, pos, level.getBlockEntity(pos));
            }
            if (level.setBlock(pos, ModBlocks.mjoellnir.defaultBlockState(), 11)) {
                if (drops != null) {
                    drops.forEach(drop -> {
                        ItemEntity ie = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), drop.copy());
                        level.addFreshEntity(ie);
                    });
                }
                ModBlocks.mjoellnir.getBlockEntity(level, pos).setStack(stack.copy());
                if (!level.isClientSide) {
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1, 1);
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static void putInWorld(ItemStack stack, Level level, BlockPos pos) {
        putInWorld(stack, level, pos, true);
    }
    
    public static void putInWorld(ItemStack stack, Level level, BlockPos pos, boolean dropOldOnReplace) {
        if (!placeInWorld(stack, level, pos, dropOldOnReplace)) {
            ItemEntity ie = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            ie.setInvulnerable(true);
            level.addFreshEntity(ie);
        }
    }

    public static boolean canHold(Player player) {
        return player.isCreative() || player.isSpectator() || MythicConfig.mjoellnir.requirement.test(player)
                || (!RingOfThorItem.getThorRing(player).isEmpty() && MythicConfig.mjoellnir.requirement_thor.test(player));
    }

    public static boolean putInInventory(Player player, ItemStack stack, int hotbarSlot) {
        boolean inserted = false;
        if (hotbarSlot < 9) {
            ItemStack current = player.getInventory().getItem(hotbarSlot);
            if (current.isEmpty()) {
                player.getInventory().setItem(hotbarSlot, stack);
                inserted = true;
            }
        } else if (hotbarSlot == 9) {
            ItemStack current = player.getInventory().offhand.get(0);
            if (current.isEmpty()) {
                player.getInventory().offhand.set(0, stack);
                inserted = true;
            }
        }
        if (!inserted) {
            int slot = player.getInventory().getFreeSlot();
            if (slot >= 0) {
                player.getInventory().setItem(slot, stack);
                inserted = true;
            }
        }
        return inserted;
    }

    public static int getHotbarSlot(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 9;
    }
}
