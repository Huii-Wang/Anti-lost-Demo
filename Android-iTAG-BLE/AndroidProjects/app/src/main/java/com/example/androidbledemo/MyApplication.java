package com.example.androidbledemo;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.androidbledemo.Devices.MyDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MyApplication extends Application {


    HashMap<String,MyDevice> bleList = new HashMap<>();
    //单例模式·
    private static MyApplication myApplication = null;
    public static MyApplication getInstance()
    {

        return myApplication;
    }

    private BluetoothListenerReceiver receiver;

    @Override
    public void onCreate() {

        super.onCreate();
        myApplication = this;


        //初始化蓝牙设备
        InitBle();
        //注册监听手机蓝牙状态
        receiver = new BluetoothListenerReceiver();
        getApplicationContext().registerReceiver(receiver,makeFilter());


        //开始循环发送消息
        //安卓由于手机设备的不同，消息会放到一个List中去定时发送，避免蓝牙拥堵导致丢包
        mHandler.postDelayed(r, 2000);//延时100毫秒

    }


    ArrayList<WriteMsgItem> msgList = new ArrayList<>();
    Handler mHandler = new Handler();
    Runnable r = new Runnable() {

        @Override
        public void run() {


            if (msgList.size()>0)
            {

                WriteMsgItem writeMsgItem = msgList.get(0);
                msgList.remove(0);

                if (writeMsgItem!=null)
                {
                    for (int i =0;i<writeMsgItem.getWriteInfo().length;i++)
                    {
                        Log.e( "蓝牙","发送消息"+writeMsgItem.getWriteInfo()[i] );
                    }

                    MyDevice myDevice = writeMsgItem.getMyDevice();
                    if (myDevice!=null)
                    {
                        writeMsgItem.getMyDevice().getBluetoothGattCharacteristicWrite().setValue( writeMsgItem.getWriteInfo());
                        writeMsgItem.getMyDevice().getBluetoothGatt().writeCharacteristic( writeMsgItem.getMyDevice().getBluetoothGattCharacteristicWrite() );
                    }

                }

            }



            //每隔1s循环执行run方法
            mHandler.postDelayed(this, 450);
        }
    };



    //注册监听手机蓝牙
    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }


    BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLeScanner = null;//api 5.0版本以上新的低功耗蓝牙搜索类
    private ScanCallback mBleScanCallback = null;//api 5.0版本以上搜索回调接口

    //蓝牙设备
    private BluetoothDevice mBluetoothDevice = null;
    //Gatt
    private BluetoothGattCharacteristic writeGatt;
    private BluetoothGatt mGatt;
    public void InitBle(){
        // 是否支持蓝牙低功耗广播（4.3+）

        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e("蓝牙","设备不支持蓝牙");
        }
        else
        {
            Log.e("蓝牙","设备支持蓝牙");

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();



            //判断设备是否开启
            if (mBluetoothAdapter.isEnabled())
            {
                Log.e("蓝牙","设备开启蓝牙,开始进行扫描");
//                startDiscovery();
            }
            else
            {
                Log.e("蓝牙","设备没有开启蓝牙");
                //主动开启蓝牙
                mBluetoothAdapter.enable();
                Toast.makeText(getApplicationContext(),"请在设置中打开蓝牙",Toast.LENGTH_SHORT).show();
            }
        }
    }





    //开始搜索蓝牙
    public void startDiscovery() {


        if (mBluetoothAdapter == null) {
            Log.e("蓝牙","蓝牙搜索失败");
            return;
        }


//        Log.e("蓝牙","开始搜索蓝牙");
        //正在查找中，不做处理
        if (mBluetoothAdapter.isDiscovering()) {
            Log.e("蓝牙","蓝牙搜索失败");
            return;
        };

        //判断版本号,如果api版本号大于5.0则使用最新的方法搜素
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.startLeScan( (BluetoothAdapter.LeScanCallback) finalCallback );
        } else {
            if (!mBluetoothAdapter.isEnabled()) return;
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
//            Log.e("蓝牙","开始扫描");

//            //指定需要识别到的蓝牙设备
            List<ScanFilter> scanFilterList = new ArrayList<>();
            ScanFilter.Builder builder = new ScanFilter.Builder();
            builder.setServiceUuid(ParcelUuid.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
            ScanFilter scanFilter = builder.build();
            scanFilterList.add(scanFilter);
            //指定蓝牙的方式，这里设置的ScanSettings.SCAN_MODE_LOW_LATENCY是比较高频率的扫描方式
            ScanSettings.Builder settingBuilder = new ScanSettings.Builder();

//            settingBuilder.setScanMode(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                settingBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                settingBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settingBuilder.setLegacy(true);
            }
            ScanSettings settings = settingBuilder.build();
//

            Log.e( "开始扫描","开始进行扫描" );
            mLeScanner.startScan(Collections.singletonList( scanFilter ),settings, finalCallback);
//            mLeScanner.startScan( finalCallback );

        }
    }



    //扫描结果
    final ScanCallback    finalCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            /*过滤空设备名*/
            if (result.getDevice().getName() == null)
            {
                return;
            }
            /*过滤非目标设备数据*/
            if (!result.getDevice().getName().startsWith("BSM"))
            {
                return;
            }





        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };













    //根据id
    //只能使用一个，一个为空或者另一个不为空
    public void ConnectBle( BluetoothDevice bluetoothDevice){

        if (bleList.containsKey(bluetoothDevice.getAddress()))
        {
            return;
        }

        Log.e("开始连接到设备","开始连接到设备");


        if (bluetoothDevice == null){
            return;
        }

        bluetoothDevice.connectGatt(getApplicationContext(), false, new BluetoothGattCallback() {
            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status);
                Log.e("蓝牙","更新");
            }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyRead(gatt, txPhy, rxPhy, status);
                Log.e("蓝牙","阅读");
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);


                switch (newState){
                    //已经连接
                    case BluetoothGatt.STATE_CONNECTED:
                        Log.e("蓝牙","已经连接 开始发现服务"+newState);
                        gatt.discoverServices();


//                        mLeScanner.stopScan(finalCallback );
                        //发送蓝牙连接成功的事件
//                        MessageEvent messageEvent = new MessageEvent();
//                        messageEvent.setEventType( EventType.BLE_CONN );
//                        EventBus.getDefault().post( messageEvent );

                        //保存设备

//                        if (mBluetoothDevice == null)
//                        {
//                            mBluetoothDevice = gatt.getDevice();
//                            mGatt = gatt;
//                        }

                        //保存连接设备到列表
                        if (bleList.containsKey(gatt.getDevice().getAddress()))
                        {
                            MyDevice myDevice = bleList.get(gatt.getDevice().getAddress());
                            myDevice.setBluetoothDevice(gatt.getDevice());
                            myDevice.setBluetoothGatt(gatt);
                            bleList.put(gatt.getDevice().getAddress(),myDevice);
                        }
                        else
                        {
                            MyDevice myDevice = new MyDevice();
                            myDevice.setBluetoothDevice(gatt.getDevice());
                            myDevice.setBluetoothGatt(gatt);
                            bleList.put(gatt.getDevice().getAddress(),myDevice);
                        }

                        break;
                    //正在连接
                    case BluetoothGatt.STATE_CONNECTING:

                        break;
                    //正在断连
                    case BluetoothGatt.STATE_DISCONNECTING:
                        break;
                    //已经断开
                    case BluetoothGatt.STATE_DISCONNECTED:
                        Log.e("蓝牙","断开连接");
                        //发送蓝牙断开的事件
//                        MessageEvent messageEvent2 = new MessageEvent();
//                        messageEvent2.setEventType( EventType.BLE_DISCONN );
//                        EventBus.getDefault().post( messageEvent2 );
//                        mBluetoothDevice = null;
//                        startDiscovery();


                        break;
                }

            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.e("蓝牙","发现服务完毕:" +status);


                if(status == BluetoothGatt.GATT_SUCCESS){



                    //发现成功后，则可以通过下面方法获取service 列表。
//                    gatt.getServices();
                    Log.e("蓝牙", "蓝牙连接到服务");
                    //得到所有Service
                    List<BluetoothGattService> supportedGattServices = gatt.getServices();
                    for (BluetoothGattService gattService : supportedGattServices) {
                        Log.e("蓝牙", "服务uuid"+gattService.getUuid().toString()); //打印出UUID
                        //得到每个Service下面的Characteristics(特征值)
                        List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {


                            Log.e("蓝牙", "特征值uuid"+gattCharacteristic.getUuid().toString()); //打印出UUID
                            if(gattCharacteristic.getUuid().toString().equals("0000ffe1-0000-1000-8000-00805f9b34fb")){
                                Log.e("蓝牙", "写入服务");


                                if (bleList.containsKey(gatt.getDevice().getAddress()))
                                {
                                    MyDevice myDevice = bleList.get(gatt.getDevice().getAddress());
                                    myDevice.setBluetoothGattCharacteristicWrite(gattCharacteristic);
                                    bleList.put(gatt.getDevice().getAddress(),myDevice);
                                }
                                //ArrayList<WriteMsgItem> msgList = new ArrayList<>();
                                //保持连接
                                int crc1 = (0x55 + 0x81 + 0x06 + 0x01 + 0x02 +0x03 + 0x04 + 0x05 +0x06  )%255;
                                //gattCharacteristic.setValue(new byte[]{(byte)0x55,(byte) 0x81,(byte)0x06,(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x04,(byte)0x05,(byte)0x06,(byte)crc1});
                                //gatt.writeCharacteristic(gattCharacteristic);
                                WriteMsgItem writeMsgItem1 = new WriteMsgItem();
                                writeMsgItem1.setMyDevice(bleList.get(gatt.getDevice().getAddress()));
                                writeMsgItem1.setWriteInfo(new byte[]{(byte)0x55,(byte) 0x81,(byte)0x06,(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x04,(byte)0x05,(byte)0x06,(byte)crc1});
                                msgList.add(writeMsgItem1);
                                //同步时间
                                int now = TimeUtils.getSecondTimestamp();

                                int second3  = now/16777216;
                                int second2  = ((now)%16777216)/65536;
                                int second1  = ((now)%65536)/256;
                                int second0  = (now)%256;
                                int crc2 = (second3+second2+second1+second0+85+131+4)%256;
                                int my = second0+second1*256+second2*256*256 + second3*256*256*256;
                                Log.e("当前时间戳是1",now+"：同步到设备");
                                Log.e("当前时间戳是2",my+"：同步到设备");
                                //gattCharacteristic.setValue(new byte[]{(byte)85,(byte)130,(byte)4,(byte)second3,(byte)second2,(byte)second1,(byte)second0,(byte)crc2});
                                //gatt.writeCharacteristic(gattCharacteristic);
                                WriteMsgItem writeMsgItem2 = new WriteMsgItem();
                                writeMsgItem2.setMyDevice(bleList.get(gatt.getDevice().getAddress()));
                                writeMsgItem2.setWriteInfo(new byte[]{(byte)85,(byte)131,(byte)4,(byte)second3,(byte)second2,(byte)second1,(byte)second0,(byte)crc2});
                                msgList.add(writeMsgItem2);
                                //请求历史数据
                                int crc3 = (0x55 + 0x85 +0x01 + 0x00)%255;
                                //gattCharacteristic.setValue(new byte[]{(byte)0x55,(byte) 0x85,(byte)0x01,(byte)0x00,(byte)crc3});
                                //gatt.writeCharacteristic(gattCharacteristic);

                                WriteMsgItem writeMsgItem3 = new WriteMsgItem();
                                writeMsgItem3.setMyDevice(bleList.get(gatt.getDevice().getAddress()));
                                writeMsgItem3.setWriteInfo(new byte[]{(byte)0x55,(byte) 0x85,(byte)0x01,(byte)0x00,(byte)crc3});
                                msgList.add(writeMsgItem3);

                                Log.e("同步时间","设备连接成功");


                            }else if(gattCharacteristic.getUuid().toString().equals("0000ffe2-0000-1000-8000-00805f9b34fb")){
                                Log.e("蓝牙", "监听数据成功");
                                //打开手机报警通知
                                gatt.setCharacteristicNotification(gattCharacteristic,true);
                            }


                        }
                    }


                }
                else
                {
                    Log.e("发现服务失败","服务失败");
                }

            }



            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);


                if (characteristic.getValue()!=null && characteristic.getValue().length>=1)
                {
                    int a =  characteristic.getValue()[0];

                    Log.e("蓝牙","设置电量"+a);
                }

            }



            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.e("蓝牙","写入");


            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);



                if(characteristic.getUuid().toString().equals("0000ffe2-0000-1000-8000-00805f9b34fb"))
                {


                    byte[] batteryInfo = characteristic.getValue();
                    for (int i = 0;i<batteryInfo.length;i++)
                    {
                        Log.e("蓝牙"+i,"获取设备回复消息"+batteryInfo[i]);
                    }



                    byte[] deviceInfo = characteristic.getValue();


                    if (deviceInfo==null || deviceInfo.length<=0)
                    {
                        return;
                    }

                    //判断是否同步时间完毕
                    if (deviceInfo[1]==-126&&deviceInfo[2]==4)
                    {
                        Log.e("时间","同步时间完成 断开连接");




                    }


                    if (( deviceInfo[1] == -123 )&& ( deviceInfo[3]== 68))
                    {
                        Log.e("接受到设备数据信","开始传输数据");
                    }
                    else   if ((deviceInfo[1] == -123 )&& (deviceInfo[2]== 16))
                    {
                        Log.e("接受到设备数据信","传输数据中");
                        //解析数据 处理数据
                        DeviceManager.getDeviceManager().OnDeviceGetmsg(gatt.getDevice().getAddress(),deviceInfo);
                    }
                    else   if ((deviceInfo[1] == -123 )&& (deviceInfo[3]== 119))
                    {
                        Log.e("接受到设备数据信","数据传输结束 清除数据");
                        //清空消息
                        //0x55、0x86、0x01、0x00、0xcrc
                        WriteMsgItem writeMsgItem3 = new WriteMsgItem();
                        writeMsgItem3.setMyDevice(bleList.get(gatt.getDevice().getAddress()));
                        int crc3 = ( 0x55 +0x86 + 0x01 )%256;
                        writeMsgItem3.setWriteInfo(new byte[]{(byte)0x55,(byte) 0x86,(byte)0x01,(byte)0x00,(byte)crc3});
                        msgList.add(writeMsgItem3);


//                        if (bleList.containsKey(gatt.getDevice().getAddress()))
//                        {
//
//
//
//                            MyDevice myDevice = bleList.get(gatt.getDevice().getAddress());
//
//
//                                Log.e("同步数据完毕，断开连接","断开连接");
//                                gatt.disconnect();
//
//                                bleList.remove(gatt.getDevice().getAddress());
//
//                        }
                    }
                    else if (deviceInfo[1] == -122 && deviceInfo[0]==-86)
                    {



                        Log.e("接受到设备数据信","数据传输结束 断开连接r");
                        gatt.disconnect();

                        if (bleList.containsKey(gatt.getDevice().getAddress()))
                        {
                            bleList.remove(gatt.getDevice().getAddress());
                        }
                    }




                    //开始传输数据
                    //传输数据中
                    //数据处理完毕，清空数据，断开连接

                }



//                if(characteristic.getUuid().toString().equals("00002a19-0000-1000-8000-00805f9b34fb")){
//
//                    byte[] batteryInfo = characteristic.getValue();
//                    for (int i = 0;i<batteryInfo.length;i++)
//                    {
//                        Log.e("蓝牙"+i,"获取到电量信息"+batteryInfo[i]);
//                    }
//                }




                Log.e( characteristic.getUuid().toString(),characteristic.getValue().toString() );
                Log.e("蓝牙特征值"+characteristic.getUuid().toString(),"改变");


            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                Log.e("蓝牙","描述");
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
                Log.e("蓝牙","可读");
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);


            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                Log.e("蓝牙","Mtu改变");
            }
        });
    }








    /*
     *  监听手机蓝牙的开、关状态
     */
    public class BluetoothListenerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.e("蓝牙","onReceive---------蓝牙正在打开中");


                            break;
                        case BluetoothAdapter.STATE_ON:
                            Log.e("蓝牙","onReceive---------蓝牙已经打开");
                            //开始发现设备
                            startDiscovery();


                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.e("蓝牙","onReceive---------蓝牙正在关闭中");

                            break;
                        case BluetoothAdapter.STATE_OFF:
                            Log.e("蓝牙","onReceive---------蓝牙已经关闭");
                            //发送蓝牙连接打开的事件


                            break;
                    }
                    break;
            }

        }
    }





}
