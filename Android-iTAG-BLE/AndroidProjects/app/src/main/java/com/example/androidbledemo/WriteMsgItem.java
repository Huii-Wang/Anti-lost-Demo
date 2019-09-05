package com.example.androidbledemo;

import com.example.androidbledemo.Devices.MyDevice;

public class WriteMsgItem {
    public MyDevice getMyDevice() {
        return myDevice;
    }

    public void setMyDevice(MyDevice myDevice) {
        this.myDevice = myDevice;
    }



    MyDevice myDevice;

    public byte[] getWriteInfo() {
        return writeInfo;
    }

    public void setWriteInfo(byte[] writeInfo) {
        this.writeInfo = writeInfo;
    }

    byte[] writeInfo;
}
