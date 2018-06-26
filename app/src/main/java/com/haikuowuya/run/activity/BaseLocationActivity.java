package com.haikuowuya.run.activity;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Window;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.haikuowuya.run.listener.OnLocationChangeListener;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;


public abstract class BaseLocationActivity extends BaseActivity {
    private OnLocationChangeListener mOnLocationChangeListener;
    //定位客户端
    private LocationClient mLocationClient = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        new RxPermissions(this).requestEach(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe((Permission permission) -> { // will emit 2 Permission objects
                    if (permission.granted) {
                        if (null == mLocationClient) {
                            initLocationClient();
                        }
                    } else if (permission.shouldShowRequestPermissionRationale) {
                        // Denied permission without ask never again
                    } else {
                    }
                });
        if (isAutoLocation()) {
            startLocation();
        }
    }

    public void setOnLocationChangeListener(OnLocationChangeListener onLocationChangeListener) {
        mOnLocationChangeListener = onLocationChangeListener;
    }

    /**
     * 初始化定位
     */
    private void initLocationClient() {
        mLocationClient = new LocationClient(this);
        initLocationParams();
        mLocationClient.registerLocationListener(bdLocation -> {
            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation || bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) { // GPS或网络定位
                System.out.println("Latitude = " + bdLocation.getLatitude() + " Longitude = " + bdLocation.getLongitude() + " addStr = " + bdLocation.getAddrStr());
                if (null != mOnLocationChangeListener) {
                    mOnLocationChangeListener.onLocationChange(bdLocation);
                } else {
                    Toast.makeText(BaseLocationActivity.this, bdLocation.getAddrStr(), Toast.LENGTH_SHORT).show();
                    mLocationClient.stop();
                }
            }
        });
    }

    public void startLocation() {
        if (null != mLocationClient) {
            mLocationClient.start();
        }
    }

    public void stopLocation() {
        if (null != mLocationClient) {
            mLocationClient.stop();
        }
    }

    /**
     * 初始化定位，设置定位参数
     */
    private void initLocationParams() {
        //用来设置定位sdk的定位方式
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span = 2500;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(false);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
    }

    public abstract boolean isAutoLocation();
}
