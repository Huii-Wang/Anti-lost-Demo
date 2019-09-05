package com.example.androidbledemo.EventBus;

public enum  EventType {
    /**
     * 发现BLE设备
     */
    BLE_DEVICEFOUND,
    /**
     * 蓝牙连接成功
     */
    BLE_CONNECT_SUCCEED,
    /**
     * 接受到蓝牙消息
     */
    BLE_MESSAGE,
    /**
     * 蓝牙断开连接
     */
    BLE_DISONNECT,
}
