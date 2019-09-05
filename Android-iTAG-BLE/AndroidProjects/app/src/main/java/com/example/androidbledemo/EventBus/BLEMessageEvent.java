package com.example.androidbledemo.EventBus;

public class BLEMessageEvent {

    /**
     * 蓝牙消息事件类型
     */
    private EventType eventType;
    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }


    /**
     * 蓝牙消息内容
     */
    private byte[] messageInfo;
    public byte[] getMessageInfo() {
        return messageInfo;
    }

    public void setMessageInfo(byte[] messageInfo) {
        this.messageInfo = messageInfo;
    }






}
