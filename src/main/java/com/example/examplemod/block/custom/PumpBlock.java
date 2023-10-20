package com.example.examplemod.block.custom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class PumpBlock extends Block {

    public PumpBlock() {
        super(Block.Properties.of().strength(1.9F).sound(SoundType.STONE));
    }


}
