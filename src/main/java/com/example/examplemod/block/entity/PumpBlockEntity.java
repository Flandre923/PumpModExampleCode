package com.example.examplemod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PumpBlockEntity extends BlockEntity {
    // 存液体的能力
    private PumpTank tank = new PumpTank();
    // 存储能量的能力
    private IEnergyStorage energy = new EnergyStorage(32000);

    private final LazyOptional<IEnergyStorage> energyProxyCap = LazyOptional.of(() -> energy);
    private final LazyOptional<IFluidHandler> fluidHandlerCap = LazyOptional.of(() -> tank);

    private int ticks;

    /**
     * 当前抽取液体方块相关的内容
     *  当前的方块坐标，范围，以及抽取表面的队列
     */
    @Nullable
    private BlockPos currentPos;
    private int range = -1;
    private Queue<BlockPos> surfaces = new LinkedList<>();
    private Block blockToReplaceLiquidsWith;


    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP_BLOCK_ENTITY_TYPE.get(), pos, state);
    }

    /**
     *  实体方块每tick方法
     *  这次我们实现整个水泵抽水的方法！
     */
    public void tick() {
        // 补满电，因为没有科技模组
        energy.receiveEnergy(energy.getMaxEnergyStored(), false);

        if (!tank.getFluid().isEmpty()) { // 有液体
            List<IFluidHandler> fluidHandlers = new LinkedList<>();
            // 获得当前方块其他各个面的实体的方块
            for (Direction facing : Direction.values()) {
                BlockEntity blockEntity = level.getBlockEntity(worldPosition.relative(facing));
                // 如果方块不是空
                if (blockEntity != null) {
                    // 查看改方块是否具有液体处理能力
                    IFluidHandler handler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, facing.getOpposite()).orElse(null);
                    // 如果具有液体处理能力，就将改实体加入到链表中
                    if (handler != null) {
                        fluidHandlers.add(handler);
                    }
                }
            }
            // 如果 具有存储流体的容器
            if (!fluidHandlers.isEmpty()) {
                // 则均分抽取到的液体，传入各个容器
                int transfer = (int) Math.floor((float) tank.getFluidAmount() / (float) fluidHandlers.size());

                for (IFluidHandler fluidHandler : fluidHandlers) {
                    FluidStack toFill = tank.getFluid().copy();
                    toFill.setAmount(transfer);
                    // 向各个容器中加入液体，并删除自身容器中的液体
                    tank.drain(fluidHandler.fill(toFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        // 每8tick并且在工作状体下进入改if一次
        if ( (ticks % 8 == 0) && getState() == PumpState.WORKING) {
            // 当前的方块为空时候，或者已经到达最小数值的时候
            // curpos就是当前抽取的方块，从surface队列中获得
            if(currentPos == null || currentPos.getY() == level.dimensionType().minY()){
                // 如果表面队列为空，则代表处理完成。
                if(surfaces.isEmpty()){
                    range ++;// 当前范围已经抽完，增加范围
                    if(range > 64){
                        // DONE
                        return;
                    }
                    // 表示当前表面的液体已经处理完了，从新构建表面
                    rebuildSurfaces();
                }
                // 否则，当前表面没有处理完毕，出队列
                currentPos = surfaces.poll();
            }else{
                // 抽当方块的下一个方块，直到最低层位置
                currentPos = currentPos.below();
            }
            // 减少能量，每搜索一次就减少能量
            energy.extractEnergy(32,false);

            // 尝试当位置的流体方块消除
            FluidStack drained = drainAt(currentPos, IFluidHandler.FluidAction.SIMULATE);
            // 如果可以消除，则将当前流体尝试添加到本身的水槽中去
            if (!drained.isEmpty() && tank.fillInternal(drained, IFluidHandler.FluidAction.SIMULATE) == drained.getAmount()) {
                // 将当前位置的流体删除
                drained = drainAt(currentPos, IFluidHandler.FluidAction.EXECUTE);
                if(!drained.isEmpty()){
                    // 真正将流体加入到自己的水槽中
                    tank.fillInternal(drained, IFluidHandler.FluidAction.EXECUTE);
                    energy.extractEnergy(32, false);
                }
            }
            setChanged();// save
        }

        ticks++;
    }

    /**
     * 从新构建surface表面的抽取的方块
     */
    private void rebuildSurfaces() {
        surfaces.clear();

        if (range == -1) {// 最开始的时候，抽取机器下面的方块
            // 机器方块下面的方块
            surfaces.add(worldPosition.below());
            return;
        }//等会我们详细说这个逻辑，大家可以打断点的方式调试这个代码。信息说下这个逻辑
        int hl = 3 + 2 * range;
        int vl = 1 + 2 * range;

        // Top
        for (int i = 0; i < hl; ++i) {
            surfaces.add(worldPosition.offset(-range - 1 + i, -1, -range - 1));
        }

        // Right
        for (int i = 0; i < vl; ++i) {
            surfaces.add(worldPosition.offset(-range - 1 + vl + 1, -1, -range - 1 + i + 1));
        }

        // Bottom
        for (int i = 0; i < hl; ++i) {
            surfaces.add(worldPosition.offset(-range - 1 + hl - i - 1, -1, -range - 1 + hl - 1));
        }

        // Left
        for (int i = 0; i < vl; ++i) {
            surfaces.add(worldPosition.offset(-range - 1, -1, -range - 1 + vl - i));
        }
    }

    // 加载区块从新设置抽取的方块
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (surfaces.isEmpty()) {
            rebuildSurfaces();
        }
    }

    /**
     * 删除pos位置的流体方块，并返回FluidStack
     * @param pos
     * @param action
     * @return
     */
    @Nonnull
    private FluidStack drainAt(BlockPos pos, IFluidHandler.FluidAction action) {
        // 获得当前流体位置的方块
        BlockState frontBlockState = level.getBlockState(pos);
        Block frontBlock = frontBlockState.getBlock();
        // 如果是流体方块
        if (frontBlock instanceof LiquidBlock) {
            if (frontBlockState.getValue(LiquidBlock.LEVEL) == 0) {//源头
                Fluid fluid = ((LiquidBlock) frontBlock).getFluid();
                // 如果action 是execute
                if (action == IFluidHandler.FluidAction.EXECUTE) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);//替换为空气方块
                }
                return new FluidStack(fluid, FluidType.BUCKET_VOLUME);
            }
        }else if(frontBlock instanceof IFluidBlock){
            // 如果是实现了IFluid的方块
            IFluidBlock fluidBlock = (IFluidBlock) frontBlock;
            if (fluidBlock.canDrain(level, pos)) {
                return fluidBlock.drain(level, pos, action);
            }
        }
        return FluidStack.EMPTY;

    }

    /**
     * 返回水槽
     * @return
     */
    public FluidTank getTank() {
        return tank;
    }

    /**
     * 保存数据到NBT
     * @param tag
     */
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt("Energy", energy.getEnergyStored());

        if (currentPos != null) {
            tag.putLong("CurrentPos", currentPos.asLong());
        }

        tag.putInt("Range", range);

        ListTag surfaces = new ListTag();

        this.surfaces.forEach(s -> surfaces.add(LongTag.valueOf(s.asLong())));

        tag.put("Surfaces", surfaces);

        tank.writeToNBT(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("Energy")) {
            energy.receiveEnergy(tag.getInt("Energy"), false);
        }

        if (tag.contains("CurrentPos")) {
            currentPos = BlockPos.of(tag.getLong("CurrentPos"));
        }

        if (tag.contains("Range")) {
            range = tag.getInt("Range");
        }

        if (tag.contains("Surfaces")) {
            ListTag surfaces = tag.getList("Surfaces", Tag.TAG_LONG);

            for (Tag surface : surfaces) {
                this.surfaces.add(BlockPos.of(((LongTag) surface).getAsLong()));
            }
        }
        tank.readFromNBT(tag);
    }



    /***
     * 获得能力，和之前的讲的教程一样
     * @param cap The capability to check
     * @param direction The Side to check from,
     *   <strong>CAN BE NULL</strong>. Null is defined to represent 'internal' or 'self'
     * @return
     * @param <T>
     */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction direction) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyProxyCap.cast();
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandlerCap.cast();
        }

        return super.getCapability(cap, direction);
    }

    /**
     *  返回目前机器的状态
     * @return
     */
    PumpState getState() {
        // 如果超出了范围就是完成了
        if (range > 64) {
            return PumpState.DONE;
            // 有红石信号
        } else if (level.hasNeighborSignal(worldPosition)) {
            return PumpState.REDSTONE;
            // 没有能量
        } else if (energy.getEnergyStored() == 0) {
            return PumpState.ENERGY;
            // 满了
        } else if (tank.getFluidAmount() > tank.getCapacity() - FluidType.BUCKET_VOLUME) {
            return PumpState.FULL;
        } else {
            // 正常工作
            return PumpState.WORKING;
        }
    }

    /**
     * 获得当前抽取的方块
     * @return
     */
    BlockPos getCurrentPosition() {
        return currentPos == null ? worldPosition.below() : currentPos;
    }

    /**
     * 获取当前的挖掘范围
     * @return
     */
    int getRange() {
        return range;
    }


    /**
     * 水槽代码
     * 封装下下，加了一个方法，你、将原来的fill方法写成了额返回0
     */
    private static class PumpTank extends FluidTank {
        public PumpTank() {
            super(64000);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        public int fillInternal(FluidStack resource, FluidAction action) {
            return super.fill(resource, action);
        }
    }

}
