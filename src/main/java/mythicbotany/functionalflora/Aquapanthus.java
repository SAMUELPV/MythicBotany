package mythicbotany.functionalflora;

import io.github.noeppi_noeppi.libx.LibX;
import mythicbotany.functionalflora.base.FunctionalFlowerBase;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CauldronBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import vazkii.botania.api.item.IPetalApothecary;
import vazkii.botania.api.subtile.RadiusDescriptor;
import vazkii.botania.client.fx.WispParticleData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Aquapanthus extends FunctionalFlowerBase {

    public static final int MAX_TICK_TO_NEXT_CHECK = 5;
    public static final int MANA_PER_TICK = 2;
    public static final int TICKS_TO_FILL = 20;

    private transient int tickToNextCheck = 0;
    @Nullable
    private BlockPos currentlyFilling = null;
    private int fillingSince = 0;

    public Aquapanthus(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn, 0x4444FF, false);
    }

    @Override
    protected void tickFlower() {
        //noinspection ConstantConditions
        if (!world.isRemote) {
            if (currentlyFilling != null) {
                if (mana >= MANA_PER_TICK) {
                    if (fill()) {
                        mana = MathHelper.clamp(mana - MANA_PER_TICK, 0, maxMana);
                        didWork = true;
                        fillingSince += 1;
                    } else {
                        fillingSince = 0;
                        currentlyFilling = null;
                    }
                }
                LibX.getNetwork().updateTE(world, pos);
                markDirty();
            } else {
                if (tickToNextCheck > 0) {
                    tickToNextCheck -= 1;
                    return;
                }
                tickToNextCheck = MAX_TICK_TO_NEXT_CHECK;

                BlockPos basePos = pos.toImmutable();
                outer: for (int xd = -3; xd <= 3; xd++) {
                    for (int zd = -3; zd <= 3; zd++) {
                        BlockPos pos = basePos.add(xd, 0, zd);
                        BlockState state = world.getBlockState(pos);
                        TileEntity te = world.getTileEntity(pos);
                        if ((state.getBlock() == Blocks.CAULDRON && state.get(CauldronBlock.LEVEL) < 3)
                                || (te instanceof IPetalApothecary && ((IPetalApothecary) te).getFluid() == IPetalApothecary.State.EMPTY)) {
                            currentlyFilling = pos;
                            fillingSince = 0;
                            markDirty();
                            break outer;
                        }
                    }
                }
            }
        } else {
            if (currentlyFilling != null && fillingSince > 0) {
                double progress = fillingSince / (double) TICKS_TO_FILL;

                double x = ((currentlyFilling.getX() - pos.getX()) * progress) + pos.getX() + 0.5;
                double y = pos.getY() + (1.5 * Math.sin(progress * Math.PI));
                double z = ((currentlyFilling.getZ() - pos.getZ()) * progress) + pos.getZ() + 0.5;

                double xd = ((currentlyFilling.getX() - pos.getX()) * progress) / 10;
                double yd = Math.sin(progress * Math.PI) / 10;
                double zd = ((currentlyFilling.getZ() - pos.getZ()) * progress) / 10;

                WispParticleData data = WispParticleData.wisp(0.85F, 0.1f, 0.1f, 1, 0.25F);
                world.addParticle(data, x, y, z, xd, yd, zd);
                data = WispParticleData.wisp((float) Math.random() * 0.1F + 0.1F, 0.2f, 0.2f, 1, 0.9F);
                world.addParticle(data, x, y, z, (float) (Math.random() - 0.5) * 0.05F, (float) (Math.random() - 0.5) * 0.05F, (float) (Math.random() - 0.5) * 0.05F);
            }
        }
    }

    private boolean fill() {
        //noinspection ConstantConditions
        BlockState state = world.getBlockState(currentlyFilling);
        TileEntity te = world.getTileEntity(currentlyFilling);
        if ((state.getBlock() == Blocks.CAULDRON && state.get(CauldronBlock.LEVEL) < 3)
                || (te instanceof IPetalApothecary && ((IPetalApothecary) te).getFluid() == IPetalApothecary.State.EMPTY)) {
            if (fillingSince >= TICKS_TO_FILL) {
                if (state.getBlock() == Blocks.CAULDRON) {
                    world.setBlockState(currentlyFilling, state.with(CauldronBlock.LEVEL, MathHelper.clamp(state.get(CauldronBlock.LEVEL) + 1, 0, 3)));
                } else if (te instanceof IPetalApothecary) {
                    ((IPetalApothecary) te).setFluid(IPetalApothecary.State.WATER);
                    te.markDirty();
                }
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public RadiusDescriptor getRadius() {
        return new RadiusDescriptor.Square(pos, 3);
    }

    @Override
    public void read(@Nonnull BlockState stateIn, @Nonnull CompoundNBT nbtIn) {
        super.read(stateIn, nbtIn);
        if (nbtIn.contains("waterFilling")) {
            CompoundNBT fillingTag = nbtIn.getCompound("waterFilling");
            currentlyFilling = new BlockPos(fillingTag.getInt("x"), fillingTag.getInt("y"), fillingTag.getInt("z"));
        } else {
            currentlyFilling = null;
        }
        fillingSince = nbtIn.getInt("filling_since");
    }

    @Nonnull
    @Override
    public CompoundNBT write(@Nonnull CompoundNBT compound) {
        if (currentlyFilling != null) {
            CompoundNBT fillingTag = new CompoundNBT();
            fillingTag.putInt("x", currentlyFilling.getX());
            fillingTag.putInt("y", currentlyFilling.getY());
            fillingTag.putInt("z", currentlyFilling.getZ());
            compound.put("waterFilling", fillingTag);
            compound.putInt("filling_since", fillingSince);
        }
        return super.write(compound);
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT updateTag = super.getUpdateTag();
        //noinspection ConstantConditions
        if (!world.isRemote) {
            if (currentlyFilling != null) {
                CompoundNBT fillingTag = new CompoundNBT();
                fillingTag.putInt("x", currentlyFilling.getX());
                fillingTag.putInt("y", currentlyFilling.getY());
                fillingTag.putInt("z", currentlyFilling.getZ());
                updateTag.put("waterFilling", fillingTag);
            }
            updateTag.putInt("filling_since", fillingSince);
        }
        return updateTag;
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        //noinspection ConstantConditions
        if (world.isRemote) {
            if (tag.contains("waterFilling")) {
                CompoundNBT fillingTag = tag.getCompound("waterFilling");
                currentlyFilling = new BlockPos(fillingTag.getInt("x"), fillingTag.getInt("y"), fillingTag.getInt("z"));
            } else {
                currentlyFilling = null;
            }
            fillingSince = tag.getInt("filling_since");
        }
        super.handleUpdateTag(state, tag);
    }
}
