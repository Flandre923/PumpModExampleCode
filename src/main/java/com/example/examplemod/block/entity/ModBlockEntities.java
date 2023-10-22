package com.example.examplemod.block.entity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 这次我们增加BlockEntity
 */
public class ModBlockEntities {
    // 和之前教程的一样
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ExampleMod.MODID);
    //

    public static RegistryObject<BlockEntityType<PumpBlockEntity>> PUMP_BLOCK_ENTITY_TYPE =
            PUMP_BLOCK_ENTITY_TYPE = BLOCK_ENTITIES.register("pump", () -> BlockEntityType.Builder.of(PumpBlockEntity::new, ModBlocks.PUMP_BLOCK.get()).build(null));

    //
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }

}
