package com.example.androidbledemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


/**
 * @author Wirhui
 * @date 2019/09/05
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textView1;
    private TextView textView2;
    private TextView textView3;
    private TextView textView4;
    private TextView textView5;

    //用户是否有定位权限
    private boolean hasLocationPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button1).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);
        findViewById(R.id.button3).setOnClickListener(this);
        findViewById(R.id.button4).setOnClickListener(this);
        findViewById(R.id.button5).setOnClickListener(this);
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView5 = (TextView) findViewById(R.id.textView5);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button1:
                //TODO implement
                //1.检查用户权限
                checkPermission();
                break;
            case R.id.button2:
                //TODO implement
                //开始发现设备
                MyApplication.getInstance().startDiscovery();
                //蓝牙设备
                textView2.setText("蓝牙正在扫描中");
                break;
            case R.id.button3:
                //TODO implement
                break;
            case R.id.button4:
                //TODO implement
                break;
            case R.id.button5:
                //TODO implement
                break;
                default:
                    break;
        }
    }



    /**
     * 1.检查用户是否开启定位权限
     *   关于使用蓝牙需要定位权限的问题，请点击这里查看
     */
    private void checkPermission() {
        //定位权限
        final String permission5 = Manifest.permission.ACCESS_FINE_LOCATION;


        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission5) != PackageManager.PERMISSION_GRANTED) {
            Log.e("判断是否有位置权限", "判断权限");
            //先判断是否被赋予权限，没有则申请权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission5)) {
                //用户拒绝了定位权限，无法使用BLE蓝牙，给用户提示打开定位权限
                Log.e("判断是否有位置权限", "不应该去权限，但是没有权限,提示用户打开权限");
                Toast.makeText(this, "请在设置开启定位权限，以使用蓝牙", Toast.LENGTH_SHORT).show();
                //在UI显示
                textView1.setText("用户拒绝了定位权限，无法进行扫描");
                hasLocationPermission = false;
            } else { //直接申请权限
                Log.e("判断是否有位置权限", "可以进行申请权限，开始申请权限");
                ActivityCompat.requestPermissions(this,
                        new String[]{permission5},
                        101);
            }
        } else {  //赋予过权限，则直接调用相机拍照
            Log.e("判断是否有位置权限", "已经拥有权限");
            //在UI显示
            textView1.setText("用户已经同意过位置权限");
            hasLocationPermission = true;

        }
    }

    /*申请权限的结果*/
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            //蓝牙申请权限
            case 101:
                Log.e("判断是否有位置权限","蓝牙权限申请成功");
                //在UI显示
                textView1.setText("获取用户定位权限成成功");
                hasLocationPermission = true;
                break;
        }
    }














}
