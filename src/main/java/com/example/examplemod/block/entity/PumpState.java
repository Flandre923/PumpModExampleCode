package com.example.examplemod.block.entity;

import net.minecraft.network.chat.Component;

/**
 * 这次添加一个状体的枚举类，用于表示当前的机器处于什么样子的状态以及进行什么处理
 */
public enum PumpState {
    ENERGY,
    REDSTONE,
    WORKING,
    FULL,
    DONE;

    /**
     * 工具方法，用于根据状态返回对应的提示文字
     * @param pump
     * @return
     */
    public static Component getMessage(PumpBlockEntity pump) {
        return switch (pump.getState()) {
            case ENERGY -> Component.literal("没有能量");
            case REDSTONE -> Component.literal("存在红石信号");
            case WORKING ->
                    Component.literal("正在工作，抽取方块坐标是:"+pump.getCurrentPosition().getX()+ pump.getCurrentPosition().getY()+" " +pump.getCurrentPosition().getZ()+" " +pump.getRange()+"/64");
            case FULL -> Component.literal("已满");
            case DONE -> Component.literal("完成");
        };
    }

}
