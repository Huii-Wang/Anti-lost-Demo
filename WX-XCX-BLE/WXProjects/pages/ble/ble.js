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
        if ( device.name.startsWith("tongche"))
        {
          /*连接到设备*/
          connectDeviceByDeviceId(device.deviceId);
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
          name,
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
  /*获取蓝牙所有的服务Services*/
  getBLEDeviceServices(deviceId) {
    wx.getBLEDeviceServices({
      deviceId,
      success: (res) => {
        for (let i = 0; i < res.services.length; i++) {
          if (res.services[i].isPrimary) {
            this.getBLEDeviceCharacteristics(deviceId, res.services[i].uuid)
            return
          }
        }
      }
    })
  },
  /*获取服务下所有的特征值Characteristics*/
  getBLEDeviceCharacteristics(deviceId, serviceId) {
    wx.getBLEDeviceCharacteristics({
      deviceId,
      serviceId,
      success: (res) => {
        console.log('getBLEDeviceCharacteristics success', res.characteristics)
        for (let i = 0; i < res.characteristics.length; i++) {
          let item = res.characteristics[i]
          if (item.properties.read) {
            wx.readBLECharacteristicValue({
              deviceId,
              serviceId,
              characteristicId: item.uuid,
            })
          }
          if (item.properties.write) {
            this.setData({
              canWrite: true
            })
            this._deviceId = deviceId
            this._serviceId = serviceId
            this._characteristicId = item.uuid
            this.writeBLECharacteristicValue()
          }
          if (item.properties.notify || item.properties.indicate) {
            wx.notifyBLECharacteristicValueChange({
              deviceId,
              serviceId,
              characteristicId: item.uuid,
              state: true,
            })
          }
        }
      },
      fail(res) {
        console.error('getBLEDeviceCharacteristics', res)
      }
    })
    // 操作之前先监听，保证第一时间获取数据
    wx.onBLECharacteristicValueChange((characteristic) => {
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
  writeBLECharacteristicValue() {
    // 向蓝牙设备发送一个0x00的16进制数据
    let buffer = new ArrayBuffer(1)
    let dataView = new DataView(buffer)
    dataView.setUint8(0, Math.random() * 255 | 0)
    wx.writeBLECharacteristicValue({
      deviceId: this._deviceId,
      serviceId: this._serviceId,
      characteristicId: this._characteristicId,
      value: buffer,
    })
  },
  closeBluetoothAdapter() {
    wx.closeBluetoothAdapter()
    this._discoveryStarted = false
  },
})
