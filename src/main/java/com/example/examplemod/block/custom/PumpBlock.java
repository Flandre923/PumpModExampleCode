package com.example.examplemod.block.custom;

import com.example.examplemod.block.entity.PumpBlockEntity;
import com.example.examplemod.block.entity.PumpState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 增加了该方块具有方块实体
 */
public class PumpBlock extends Block implements EntityBlock {

    public PumpBlock() {
        super(Block.Properties.of().strength(1.9F).sound(SoundType.STONE));
    }

    /**
     * 玩家右键方块会调用的方法
     * @param state
     * @param level
     * @param pos
     * @param player
     * @param hand
     * @param hit
     * @return
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(!level.isClientSide){
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PumpBlockEntity pump) {
                IEnergyStorage energy = pump.getCapability(ForgeCapabilities.ENERGY).orElse(null);
                if (energy == null) {
                    return InteractionResult.SUCCESS;
                }
                // 根据pump状态获得提示词
                Component message = PumpState.getMessage(pump);

                if (message != null) {
                    player.sendSystemMessage(message);
                }
                // 打印方块实体的数据。
                player.sendSystemMessage(Component.translatable("当前能量" +energy.getEnergyStored()+" 最大能量:"+ energy.getMaxEnergyStored()));
                player.sendSystemMessage(Component.translatable("当前流体容量" +pump.getTank().getFluidAmount()+" 当前流体是:"+pump.getTank().getFluid().getDisplayName()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 实体方块必须实现的方法
     * @param pos
     * @param state
     * @return
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PumpBlockEntity(pos, state);
    }

    /**
     * 调用tick方法
     * @param level
     * @param state
     * @param type
     * @return
     * @param <T>
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide ? (levelTicker, pos, stateTicker, blockEntity) -> ((PumpBlockEntity) blockEntity).tick() : null;
    }
}
