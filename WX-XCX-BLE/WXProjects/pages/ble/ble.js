/*获取APP实例*/
const app = getApp()

function inArray(arr, key, val) {
  for (let i = 0; i < arr.length; i++) {
    if (arr[i][key] === val) {
      return i;
    }
  }
  return -1;
}

// ArrayBuffer转16进度字符串示例
function ab2hex(buffer) {
  var hexArr = Array.prototype.map.call(
    new Uint8Array(buffer),
    function (bit) {
      return ('00' + bit.toString(16)).slice(-2)
    }
  )
  return hexArr.join('');
}

Page({
  data: {
    devices: [],
    connected: false,
    chs: [],
    writeUUid:"",
    writeCharacteristic:"",
    deviceId:""
  },
  /*1.打开蓝牙适配器 判断用户的蓝牙设备是否可用*/
  openBluetoothAdapter() {
    wx.openBluetoothAdapter({
      success: (res) => {
        console.log('打开蓝牙成功，可以进行扫描设备', res)
        this.startBluetoothDevicesDiscovery()
      },
      fail: (res) => {
        if (res.errCode === 10001) {
          wx.onBluetoothAdapterStateChange(function (res) {
            /*用户的蓝牙状态可能是开，也可能是关闭*/
            console.log('蓝牙状态发生改变', res)
            /*如果蓝牙打开*/
            if (res.available) {
              /*2.开始进行设备扫描 蓝牙可用，直接进行扫描*/
              this.startBluetoothDevicesDiscovery()
            }
            /*如果用户手机蓝牙不可用*/
            else
            {
              console.log("请检查蓝牙状态，或者打开定位权限重试")
            }
          })
        }
      }
    })
  },
  /*获取手机蓝牙适配器状态*/
  getBluetoothAdapterState() {
    wx.getBluetoothAdapterState({
      success: (res) => {
        console.log('getBluetoothAdapterState', res)
        if (res.discovering) {
          this.onBluetoothDeviceFound()
        } else if (res.available) {
          this.startBluetoothDevicesDiscovery()
        }
      }
    })
  },
  /*开始进行蓝牙扫描程序*/
  startBluetoothDevicesDiscovery() {
    if (this._discoveryStarted) {
      console.log("蓝牙扫描已开始");
      return
    }
    this._discoveryStarted = true
    wx.startBluetoothDevicesDiscovery({
      allowDuplicatesKey: true,
      success: (res) => {
        console.log('startBluetoothDevicesDiscovery success', res)
        this.onBluetoothDeviceFound()
      },
    })
  },
  /*停止蓝牙扫描，发现到目标设备后就需要停止扫描*/
  stopBluetoothDevicesDiscovery() {
    wx.stopBluetoothDevicesDiscovery()
  },
  /*调用startBluetoothDevicesDiscovery后，用户手机发现的蓝牙设备消息会在这里展示 */
  /*可以根据用户设备名、MAC地址等进行过滤*/
  onBluetoothDeviceFound() {
    wx.onBluetoothDeviceFound((res) => {
      res.devices.forEach(device => {
        if (!device.name && !device.localName) {
          return
        }
        const foundDevices = this.data.devices
        const idx = inArray(foundDevices, 'deviceId', device.deviceId)
        const data = {}
        if (idx === -1) {
          data[`devices[${foundDevices.length}]`] = device
        } else {
          data[`devices[${idx}]`] = device
        }
        this.setData(data)
        /*如果发现设备为目标设备，直接进行连接*/
        /*3.对扫描结果进行过滤，符合条件的设备进行连接*/
        /*这里我们通过设备名进行过滤 如果名字以 iTag进行开头或者tongche开头，进行连接*/
        if (device.name.startsWith("iTAG") || device.name.startsWith("tongche"))
        {
          /*连接到设备*/
          this.stopBluetoothDevicesDiscovery();
          // connectDeviceByDeviceId(device.deviceId);
          this.connectDeviceByDeviceId(device.deviceId)

         
        }

        // if (device.name.toString.startsWith("iTag") || device.name.toString.startsWith("tongche")) {
        //   /*连接到设备*/
        //   connectDeviceByDeviceId(device.deviceId);
        // }


      })
    })
  },
  // createBLEConnection(e) {
  //   const ds = e.currentTarget.dataset
  //   const deviceId = ds.deviceId
  //   const name = ds.name
  //   wx.createBLEConnection({
  //     deviceId,
  //     success: (res) => {
  //       this.setData({
  //         connected: true,
  //         name,
  //         deviceId,
  //       })
  //       this.getBLEDeviceServices(deviceId)
  //     }
  //   })
  //   this.stopBluetoothDevicesDiscovery()
  // },
  /*3.连接到目标设备 通过bleId进行连接*/
  connectDeviceByDeviceId(deviceId){
    wx.createBLEConnection({
      deviceId,
      success: (res) => {
        this.setData({
          connected: true,
          deviceId,
        })
        /**/
        this.getBLEDeviceServices(deviceId)
      }
    })
    /*连接成功后要对扫描进行停止*/
    this.stopBluetoothDevicesDiscovery()
  },
  closeBLEConnection() {
    wx.closeBLEConnection({
      deviceId: this.data.deviceId
    })
    this.setData({
      connected: false,
      chs: [],
      canWrite: false,
    })
  },
  /*4.获取蓝牙的服务和特征*/
  /*获取蓝牙所有的服务Services*/
  getBLEDeviceServices(deviceId) {
    wx.getBLEDeviceServices({
      deviceId,
      success: (res) => {
        for (let i = 0; i < res.services.length; i++) {
          /*根据协议，用到两个服务*/
          /*服务和特征的关系：一个蓝牙可以有多个服务，一个服务下可以有多个特征*/
          /*小程序和蓝牙设备进行通讯，就是在确定的设备id,确定的服务，确定的特征下进行首发消息*/
          /*响铃 服务1802 特征值：2A06 属性：写 设备向蓝牙设备发送消息需要使用这个服务*/
          /*通知服务FFE0 特征值：FFE1 属性：通知 设备接受蓝牙设备发送的按键消息需要使用这个服务*/
          /*其他功能请根据自己需要进行扩展*/
          if (res.services[i].uuid == "00001802-0000-1000-8000-00805F9B34FB") {
            console.log("发现服务 响铃服务 1802");
             /*开始获取特征值*/
            this.getBLEDeviceCharacteristics(deviceId, res.services[i].uuid)
          }
          else if (res.services[i].uuid == "0000FFE0-0000-1000-8000-00805F9B34FB") {
            console.log("发现服务 设备按键通知服务 FFE0");
            /*开始获取特征值*/
            this.getBLEDeviceCharacteristics(deviceId, res.services[i].uuid)
          }
        }
      }
    })
  },
  /*4.获取蓝牙的服务和特征*/
  /*获取服务下所有的特征值Characteristics*/
  getBLEDeviceCharacteristics(deviceId, serviceId) {
    wx.getBLEDeviceCharacteristics({
      deviceId,
      serviceId,
      success: (res) => {
        console.log('getBLEDeviceCharacteristics success', res.characteristics)
        for (let i = 0; i < res.characteristics.length; i++) {

          // let item = res.characteristics[i]
          // if (item.properties.read) {
          //   wx.readBLECharacteristicValue({
          //     deviceId,
          //     serviceId,
          //     characteristicId: item.uuid,
          //   })
          // }
          // if (item.properties.write) {
          //   this.setData({
          //     canWrite: true
          //   })
          //   this._deviceId = deviceId
          //   this._serviceId = serviceId
          //   this._characteristicId = item.uuid
          //   this.writeBLECharacteristicValue()
          // }
          // if (item.properties.notify || item.properties.indicate) {
          //   wx.notifyBLECharacteristicValueChange({
          //     deviceId,
          //     serviceId,
          //     characteristicId: item.uuid,
          //     state: true,
          //   })
          // }


          /*由于我们已经先前得到蓝牙的协议，可以直接进行判断，对改特征值进行修改*/

          /*4.监听防丢器设备发来的按键消息*/
          if (res.characteristics[i].uuid =="0000FFE1-0000-1000-8000-00805F9B34FB")
          {
            console.log("开始监听设备按键消息");

            wx.notifyBLECharacteristicValueChange({
              deviceId,
              serviceId,
              characteristicId: res.characteristics[i].uuid,
              state: true,
            })
          }
           /*5.向防丢器发送消息，控制防丢器的响铃与不响铃*/
           /*记录可以写入的服务*/
          else if (res.characteristics[i].uuid == "00002A06-0000-1000-8000-00805F9B34FB"){
            console.log("获取到写入特征");
            /*把可以写入的服务和特征进行记录*/
            this.setData(
              {
                deviceId:deviceId,
                writeUUid:serviceId,
                writeCharacteristic: res.characteristics[i].uuid
              }
            );
            // this._deviceId = deviceId
            // this._serviceId = serviceId
            // this._characteristicId = res.characteristics[i].uuid
            // this.writeBLECharacteristicValue()
          }



        }
      },
      fail(res) {
        console.error('getBLEDeviceCharacteristics', res)
      }
    })
    // 操作之前先监听，保证第一时间获取数据
    wx.onBLECharacteristicValueChange((characteristic) => {
      console.log("接受到按键消息:" + characteristic.characteristicId + ":" + ab2hex(characteristic.value));
      const idx = inArray(this.data.chs, 'uuid', characteristic.characteristicId)
      const data = {}
      if (idx === -1) {
        data[`chs[${this.data.chs.length}]`] = {
          uuid: characteristic.characteristicId,
          value: ab2hex(characteristic.value)
        }
      } else {
        data[`chs[${idx}]`] = {
          uuid: characteristic.characteristicId,
          value: ab2hex(characteristic.value)
        }
      }
      // data[`chs[${this.data.chs.length}]`] = {
      //   uuid: characteristic.characteristicId,
      //   value: ab2hex(characteristic.value)
      // }
      this.setData(data)
    })
  },
  /*写开启报警消息 01 到设备 */
  writeBLECharacteristicValue01() {


    // deviceId: deviceId,
    //   writeUUid: serviceId,
    //     writeCharacteristic: res.characteristics[i].uuid


    var hexString = "01";
    // 向蓝牙设备发送一个0x00的16进制数据
    var typedArray = new Uint8Array(hexString.match(/[\da-f]{2}/gi).map(function (h) {
      return parseInt(h, 16)
    }));
    var buffer = typedArray.buffer;
    // let dataView = new DataView(buffer)
    // dataView.setUint8(0, Math.random() * 255 | 0)
    // dataView.setUint8(0,1);
    /*使用记录的特征和服务进行发送*/
    wx.writeBLECharacteristicValue({
      deviceId: this.data.deviceId,
      serviceId: this.data.writeUUid,
      characteristicId: this.data.writeCharacteristic,
      value: buffer,
    })
  },
  /*写关闭报警消息 00 到设备 */
  writeBLECharacteristicValue00() {


    // deviceId: deviceId,
    //   writeUUid: serviceId,
    //     writeCharacteristic: res.characteristics[i].uuid


    var hexString = "00";
    // 向蓝牙设备发送一个0x00的16进制数据
    var typedArray = new Uint8Array(hexString.match(/[\da-f]{2}/gi).map(function (h) {
      return parseInt(h, 16)
    }));
    var buffer = typedArray.buffer;
    // let dataView = new DataView(buffer)
    // dataView.setUint8(0, Math.random() * 255 | 0)
    // dataView.setUint8(0, 1);
    /*使用记录的特征和服务进行发送*/
    wx.writeBLECharacteristicValue({
      deviceId: this.data.deviceId,
      serviceId: this.data.writeUUid,
      characteristicId: this.data.writeCharacteristic,
      value: buffer,
    })
  },
  closeBluetoothAdapter() {
    wx.closeBluetoothAdapter()
    this._discoveryStarted = false
  },
})
