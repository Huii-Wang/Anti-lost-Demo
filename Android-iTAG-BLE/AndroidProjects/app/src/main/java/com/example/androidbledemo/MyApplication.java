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
import com.example.androidbledemo.EventBus.BLEMessageEvent;
import com.example.androidbledemo.EventBus.EventType;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyApplication extends Application {





    //单例模式·
    private static MyApplication myApplication = null;
    public static MyApplication getInstance()
    {

        return myApplication;
    }

    /**
     * 当前连接的/准备连接的设备
     */
    MyDevice myDevice = new MyDevice();

    /**
     *手机蓝牙适配器
     */
    private BluetoothListenerReceiver receiver;

    @Override
    public void onCreate() {

        super.onCreate();
        myApplication = this;


        //初始化蓝牙设备
        initBle();
        //注册监听手机蓝牙状态
        receiver = new BluetoothListenerReceiver();
        getApplicationContext().registerReceiver(receiver,makeFilter());



        /**
         *
         * 开始循环发送消息
         * 安卓由于手机设备的不同，消息会放到一个List中去定时发送，避免蓝牙拥堵导致丢包
         * 延时100毫秒
         */
        mHandler.postDelayed(r, 2000);

    }


    /**
     * 蓝牙消息队列
     */
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
                        Log.e( "向设备发送消息","消息内容"+writeMsgItem.getWriteInfo()[i] );
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


    /**
     * 注册监听蓝牙消息
     * 监听用户打开/关闭蓝牙
     * @return
     */
    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }


    BluetoothAdapter mBluetoothAdapter;
    /**
     * //api 5.0版本以上新的低功耗蓝牙搜索类
     */
    private BluetoothLeScanner mLeScanner = null;
    /**
     * //api 5.0版本以上搜索回调接口
     */
    private ScanCallback mBleScanCallback = null;

    /**
     * 蓝牙设备
     */
    private BluetoothDevice mBluetoothDevice = null;
    /**
     *蓝牙 写入服务的Gatt
     */
    private BluetoothGattCharacteristic writeGatt;
    /**
     *蓝牙 Gatt
     */
    private BluetoothGatt mGatt;

    /**
     * 方法名：initBle
     * 功能：初始化手机蓝牙适配器
     */
    public void initBle(){
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


    /**
     * 方法：startDiscovery
     *功能：开始搜索蓝牙
     */
    public void startDiscovery() {


        /**
         * 在初始化中未获取到手机蓝牙适配器
         * 蓝牙适配器为空
         */
        if (mBluetoothAdapter == null) {
            Log.e("蓝牙","蓝牙搜索失败");
            return;
        }

        /**
         * 蓝牙正在搜索中不做处理
         */
        if (mBluetoothAdapter.isDiscovering()) {
            Log.e("蓝牙","蓝牙搜索失败");
            return;
        };

        /**
         * 判断版本号,如果api版本号大于5.0则使用最新的方法搜素
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.startLeScan( (BluetoothAdapter.LeScanCallback) finalCallback );
        } else {
            if (!mBluetoothAdapter.isEnabled())
            {
                return;
            }
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

            /**
             * 对扫描条件进行设置
             * 1.指定目标的UDID  0000ffe0-0000-1000-8000-00805f9b34fb
             * 2.设置扫描频率
             */
            List<ScanFilter> scanFilterList = new ArrayList<>();
            ScanFilter.Builder builder = new ScanFilter.Builder();
            builder.setServiceUuid(ParcelUuid.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"));
            ScanFilter scanFilter = builder.build();
            scanFilterList.add(scanFilter);
            //指定蓝牙的方式，这里设置的ScanSettings.SCAN_MODE_LOW_LATENCY是比较高频率的扫描方式
            ScanSettings.Builder settingBuilder = new ScanSettings.Builder();

            //settingBuilder.setScanMode(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT);
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

            Log.e( "开始扫描","开始进行扫描" );
            mLeScanner.startScan(Collections.singletonList( scanFilter ),settings, finalCallback);
            //mLeScanner.startScan( finalCallback );

        }
    }


    /**
     * 方法：扫描结果
     * 功能：蓝牙扫描设备的回调
     * 提示：可以做在这里对蓝牙进行条件过滤
     */
    final ScanCallback    finalCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            /**
             *过滤设备名为空的设备
             */
            if ("".equals(result.getDevice().getName()))
            {
                return;
            }
            /**
             * 以设备名的方式进行过滤设备 如果设备名以XXiTag开头,说明找到目标设备
             * 找到目标设备
             * 1.停止扫描
             * 2.一般在这里可以直接进行设备连接
             */
            if (!result.getDevice().getName().startsWith("iTAG"))
            {
                /**
                 * 停止扫描
                 */
               mLeScanner.stopScan(finalCallback);

                /**
                 * 保存到MyDevice
                 */
                myDevice.setBluetoothDevice(result.getDevice());
                /**
                 * 发送发现设备的消息
                 */
                BLEMessageEvent bleMessageEvent = new BLEMessageEvent();
                bleMessageEvent.setEventType(EventType.BLE_DEVICEFOUND);
                EventBus.getDefault().post(bleMessageEvent);

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


    /**
     * 方法名：connectBle
     * 功能：连接到设备
     *
     * @param bluetoothDevice
     */
    public void connectBle( BluetoothDevice bluetoothDevice){


        Log.e("开始连接到设备","开始连接到设备");


        if (bluetoothDevice == null){
            return;
        }

        bluetoothDevice.connectGatt(getApplicationContext(), false, new BluetoothGattCallback() {
            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status);
                Log.e("蓝牙消息","更新");
            }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyRead(gatt, txPhy, rxPhy, status);
                Log.e("蓝牙消息","阅读");
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);


                switch (newState){
                    //已经连接
                    case BluetoothGatt.STATE_CONNECTED:
                        /**
                         * 和目标BLE设备连接成功
                         * 1.发起请求设备服务，并获取每一条服务下的特征
                         *  根据设备不同的特征值，确定对设备进行读写或者接受设备消息的操作
                         * 2.记录设备与状态
                         */
                        Log.e("蓝牙消息","已经连接 开始发现服务"+newState);
                        gatt.discoverServices();
                        /**
                         * 发送设备连接成功的消息
                         */
                        BLEMessageEvent bleMessageEvent = new BLEMessageEvent();
                        bleMessageEvent.setEventType(EventType.BLE_CONNECT_SUCCEED);
                        EventBus.getDefault().post(bleMessageEvent);

                        /**
                         * 保存到MyDevice
                         */
                        myDevice.setBluetoothGatt(gatt);



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
                        /**
                         * 发送蓝牙断开的消息
                         */
                        BLEMessageEvent bleMessageEvent2 = new BLEMessageEvent();
                        bleMessageEvent2.setEventType(EventType.BLE_DISONNECT);
                        EventBus.getDefault().post(bleMessageEvent2);
                        break;

                        default:
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

                            if ("00002a06-0000-1000-8000-00805f9b34fb".equals(gattCharacteristic.getUuid().toString())) {

                                /**
                                 * 记录写入服务
                                 */
                                myDevice.setBluetoothDevice(gatt.getDevice());
                                myDevice.setBluetoothGatt(gatt);
                                myDevice.setBluetoothGattCharacteristicWrite(gattCharacteristic);

                                Log.e("蓝牙", "写入服务");






                                Log.e("同步时间","设备连接成功");


                            }else  if ("0000ffe1-0000-1000-8000-00805f9b34fb".equals(gattCharacteristic.getUuid().toString())) {
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


                /**
                 * 设备的通知消息会在这里进行获取
                 */
                if ("0000ffe2-0000-1000-8000-00805f9b34fb".equals(characteristic.getUuid().toString())) {

                    /**
                     * 通过EventBus发送设备发来的消息
                     */
                    BLEMessageEvent bleMessageEvent2 = new BLEMessageEvent();
                    bleMessageEvent2.setEventType(EventType.BLE_MESSAGE);
                    bleMessageEvent2.setMessageInfo(characteristic.getValue());
                    EventBus.getDefault().post(bleMessageEvent2);



//                    byte[] batteryInfo = characteristic.getValue();
//                    for (int i = 0; i < batteryInfo.length; i++) {
//                        Log.e("蓝牙" + i, "获取设备回复消息" + batteryInfo[i]);
//                    }

                }
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


    /**
     * 给BLE发送消息
     */
    public void writeMsgToBLe(byte[] bytes)
    {
        WriteMsgItem writeMsgItem1 = new WriteMsgItem();
        writeMsgItem1.setMyDevice(myDevice);
        //new byte[]{(byte)0x55,(byte) 0x81,(byte)0x06,(byte)0x01,(byte)0x02,(byte)0x03,(byte)0x04,(byte)0x05,(byte)0x06,(byte)crc1}
        writeMsgItem1.setWriteInfo(bytes);
        msgList.add(writeMsgItem1);
    }


    /**
     * 监听设备的蓝牙状态
     * 手机蓝牙关闭/打开后的一些操作逻辑写在这里
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



                            break;
                            default:
                                break;
                    }
                    break;
                    default:
                        break;
            }

        }
    }





}
