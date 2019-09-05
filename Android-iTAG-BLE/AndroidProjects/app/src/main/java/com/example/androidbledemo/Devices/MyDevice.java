package com.example.androidbledemo.Devices;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

//蓝牙设备
public class MyDevice {

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }



    BluetoothDevice bluetoothDevice;
    BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic bluetoothGattCharacteristicWrite;

    public boolean isConnectUpdateTime() {
        return isConnectUpdateTime;
    }

    public void setConnectUpdateTime(boolean connectUpdateTime) {
        isConnectUpdateTime = connectUpdateTime;
    }

    boolean isConnectUpdateTime = false;

    public BluetoothGattCharacteristic getBluetoothGattCharacteristicWrite() {
        return bluetoothGattCharacteristicWrite;
    }

    public void setBluetoothGattCharacteristicWrite(BluetoothGattCharacteristic bluetoothGattCharacteristicWrite) {
        this.bluetoothGattCharacteristicWrite = bluetoothGattCharacteristicWrite;
    }
}