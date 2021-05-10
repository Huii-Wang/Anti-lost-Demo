import Foundation
import CoreBluetooth
import UIKit

//用于看发送数据是否成功!
class LLBlueTooth:NSObject {
    
    //单例对象
    internal static let instance = LLBlueTooth()
    
    //中心对象
    var central : CBCentralManager?
    
    
    // 当前连接的设备
    var currentPeripheral:CBPeripheral?
    //发送数据特征(连接到设备之后可以把需要用到的特征保存起来，方便使用)
    var sendCharacteristic:CBCharacteristic?
    
    
    
    override init() {
        
        super.init()
        
        if central == nil {
            print("蓝牙初始化成功")
            //1.初始化蓝牙为中心设备(Master)
            self.central = CBCentralManager.init(delegate:self, queue:nil, options:[CBCentralManagerOptionShowPowerAlertKey:true])
        }
        
    }
    
    
    
    
    // MARK: 扫描设备的方法
    func scanForPeripheralsWithServices(_ serviceUUIDS:[CBUUID]?, options:[String: AnyObject]?){

        
        if ((self.central?.isScanning) == true) {
            
        }
        else
        {
            print("开始蓝牙扫描")
            self.central?.scanForPeripherals(withServices: nil, options: options)
        }
    }
    
    
    // MARK: 停止扫描
    func stopScan() {
        print("停止蓝牙扫描扫描")
        self.central?.stopScan()
        
    }
    
  

    
    func writeCurrentDevice(data:Data)  {
        if(self.currentPeripheral == nil){
            return;
        }
  
        let per = self.currentPeripheral
        
    
        currentPeripheral!.writeValue(data, for: sendCharacteristic!,type: CBCharacteristicWriteType.withoutResponse)
    }
    
    func setModeByIndex(modeInde:UInt8,liangdu:Int)  {
        print("setModeByIndex \(modeInde) \(liangdu)")
        // 0x03 0xde为亮度的两个字节 详情参考文档，后续要可以修改亮度
        let date = Data.init(bytes:[0xaa,0x03,0x04,0x02,modeInde, UInt8( liangdu/255),   UInt8( liangdu%255),0xbb])
        self.writeCurrentDevice(data: date)
    }
    
    func setHSVColor(h:Int,s:Int,v:Int)  {
        print("setHSVColor H\(h)")
        print("setHSVColor S\(s)")
        print("setHSVColor V\(v)")
        // 0x03 0xde为亮度的两个字节 详情参考文档，后续要可以修改亮度
        let date = Data.init(bytes:[0xaa,0x03,0x07,0x01, UInt8(h/255),UInt8(h%255),UInt8(s/255),UInt8(s%255),UInt8(v/255),UInt8(v%255),0xbb])
        self.writeCurrentDevice(data: date)

    }
    
    


    
    //
    func setBuzzerAlarm(isOn:Bool)  {
        var onOffValue:UInt8 = 0;
        if isOn {
            onOffValue = 1;
        }
        let date = Data.init(bytes:[onOffValue])
        self.writeCurrentDevice(data: date)
    }
    


 
    
    

    
    
    
    
}


//MARK: -- 中心管理器的代理
extension LLBlueTooth : CBCentralManagerDelegate{
    
    // MARK: 检查运行这个App的设备是不是支持BLE。
    func centralManagerDidUpdateState(_ central: CBCentralManager){
        switch central.state {
        
        case CBManagerState.poweredOn:
            print("蓝牙打开")
            scanForPeripheralsWithServices(nil, options: nil)
            
        case CBManagerState.unauthorized:
            print("没有蓝牙功能")
            break;
        case CBManagerState.poweredOff:
            print("蓝牙关闭")
            break;
            
        default:
            print("未知状态")
            break;
        }
 
        
    }
    
    
    // 开始扫描之后会扫描到蓝牙设备，扫描到之后走到这个代理方法
    // MARK: 中心管理器扫描到了设备
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        if peripheral.name == nil {
            return;
        }
        

        

        print("发现设备%s%s",peripheral.name ?? "1",peripheral.identifier);
        
        //2.这里通过设备名进行过滤
        if ((peripheral.name!.starts(with: "iTAG"))) {
            //发现目标设备 停止扫描 保存设备 停止连接
            print("发现目标设备")
            self.central?.stopScan();
            self.currentPeripheral = peripheral;
            self.central?.connect(self.currentPeripheral!, options: nil);
        }

    }
    
    
    // MARK: 连接外设成功，开始发现服务
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral){
        // 设置代理
        peripheral.delegate = self
        // 开始发现服务
        peripheral.discoverServices(nil)
        // 保存当前连接设备
        self.currentPeripheral = peripheral
        // 这里可以发通知出去告诉设备连接界面连接成功
    }
    
    // MARK: 连接外设失败
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        // 这里可以发通知出去告诉设备连接界面连接失败
    }
    
    // MARK: 连接丢失
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        //这里是断开的设备
        //可以在这里发起重连
        self.central?.connect(peripheral, options: nil);

    }
}


// 外设的代理
extension LLBlueTooth : CBPeripheralDelegate {
    
    //MARK: - 匹配对应服务UUID
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?){
        
        if error != nil {
            return
        }
        
        for service in peripheral.services! {
            peripheral.discoverCharacteristics(nil, for: service )
        }
        
    }
    
    //MARK: - 服务下的特征
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?){
        
        if (error != nil){
            return
        }
        
        for  characteristic in service.characteristics! {
            switch characteristic.uuid.description {
            
            case "2A06":
                print("记录写的特征")
                self.sendCharacteristic = characteristic;
            case "FFE1":
                print("订阅特征值")
                peripheral.setNotifyValue(true, for: characteristic)
                break
            // 读区特征值，只能读到一次
            //连接成功
            // print("连接成功4")
            default:
                print("扫描到其他特征")
            }
            
        }
        
    }
    
    //MARK: - 特征的订阅状体发生变化
    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?){
        
        guard error == nil  else {
            return
        }
        
    }
    
    // MARK: - 获取外设发来的数据
    // 注意，所有的，不管是 read , notify 的特征的值都是在这里读取
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?)-> (){
        
        if(error != nil){
            return
        }
        
        switch characteristic.uuid.uuidString {

            
        case "FFE1":
            //这里可以接受到按键消息
            print("收到设备发送来的消息: \(characteristic.uuid.uuidString) \(characteristic.value!)")
            for i in 0 ..< characteristic.value!.count{
    
//                print( "\(i): \(characteristic.value![i])" )
                print(String(format:"收到设备发送来的消息HEX %2X", characteristic.value![i]))
            }
            break
        default:
            print("收到了其他数据特征数据: \(characteristic.uuid.uuidString) \(characteristic.value!)")
            for i in 0 ..< characteristic.value!.count{
//                print( "\(i): \(characteristic.value![i])" )
                print( "\(i): \(String.init(format: String(characteristic.value![i]), "%2x"))" )
            }
        }
        
    }
    
    
    //MARK: - 检测中心向外设写数据是否成功
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        
        if(error != nil){
            print("发送数据失败!error信息:\(String(describing: error))")
        }
        
    }
    
    
    
}

