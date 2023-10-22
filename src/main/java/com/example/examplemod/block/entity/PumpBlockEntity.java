package com.example.examplemod.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.Queue;

public class PumpBlockEntity extends BlockEntity {
    // 存液体的能力
    private PumpTank tank = new PumpTank();
    // 存储能量的能力
    private IEnergyStorage energy = new EnergyStorage(32000);

    private final LazyOptional<IEnergyStorage> energyProxyCap = LazyOptional.of(() -> energy);
    private final LazyOptional<IFluidHandler> fluidHandlerCap = LazyOptional.of(() -> tank);

    /**
     * 当前抽取液体方块相关的内容
     *  当前的方块坐标，范围，以及抽取表面的队列
     */
    @Nullable
    private BlockPos currentPos;
    private int range = -1;
    private Queue<BlockPos> surfaces = new LinkedList<>();


    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP_BLOCK_ENTITY_TYPE.get(), pos, state);
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
