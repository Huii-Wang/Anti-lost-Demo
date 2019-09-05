package com.example.androidbledemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }



    /**
     * 1.检查用户是否开启定位权限
     *   关于使用蓝牙需要定位权限的问题，请点击这里查看
     */
    private void checkPermission() {
        //定位权限
        final String permission5 = Manifest.permission.ACCESS_FINE_LOCATION;


        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission5) != PackageManager.PERMISSION_GRANTED) {
            Log.e("判断是否有位置权限","判断权限");
            //先判断是否被赋予权限，没有则申请权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission5)) {
                //用户拒绝了定位权限，无法使用BLE蓝牙，给用户提示打开定位权限
                Log.e("判断是否有位置权限","不应该去权限，但是没有权限,提示用户打开权限");
                Toast.makeText(this,"请在设置开启定位权限，以使用蓝牙",Toast.LENGTH_SHORT).show();
            } else { //直接申请权限
                Log.e("判断是否有位置权限","可以进行申请权限，开始申请权限");
                ActivityCompat.requestPermissions(this,
                        new String[]{permission5},
                        101);
            }
        } else {  //赋予过权限，则直接调用相机拍照
            Log.e("判断是否有位置权限","已经拥有权限");
            MyApplication.getInstance().startDiscovery();
        }
    }


}
