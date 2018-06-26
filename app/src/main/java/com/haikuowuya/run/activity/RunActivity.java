package com.haikuowuya.run.activity;


import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.utils.DistanceUtil;
import com.haikuowuya.run.R;
import com.haikuowuya.run.db.DBManager;
import com.haikuowuya.run.listener.OnLocationChangeListener;
import com.haikuowuya.run.model.bean.RunRecord;
import com.haikuowuya.run.utils.FileUtil;
import com.haikuowuya.run.utils.GeneralUtil;
import com.haikuowuya.run.utils.IdentiferUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import cn.bmob.v3.listener.SaveListener;

public class RunActivity extends BaseLocationActivity implements View.OnClickListener {

    private MapView mapView;
    private TextView timeText;
    private TextView distanceText;
    private ImageView startOrPauseImg;
    private ImageView continueImg;
    private ImageView stopImg;
    private RelativeLayout startRelative;
    private LinearLayout pauseLinear;
    private TextView stateText;

    private TextView dialogContinue;
    private TextView dialogEnd;

    /***
     * 是否是模拟状态
     */
    private CheckBox mCheckBox;

    private Dialog dialog; // 弹窗提示


    private RunRecord runRecord = null; // 跑步记录

    private String picPath = null; // 截屏路径

    private boolean isStart = false; // 标示 是否开始运动，默认false，未开始

    private double distance = 0.0; // 跑步总距离
    /****
     * 记录的最小距离
     */
    private static final double MIN_DISTANCE = 3.0;
    private int time = 0; //跑步用时，单位秒
    private Timer timer = null;
    private TimerTask timerTask = null;

    private List<LatLng> pointList = new ArrayList<>(); //坐标点集合
    private List<Float> speedList = new ArrayList<>(); // 速度集合

    private BaiduMap baiduMap;  //地图对象
    /****
     * 默认地图定位成功后的缩放比例
     */
    private float mDefaultZoom = 17.f;
    /**
     * 图标
     */
    private static BitmapDescriptor realtimeBitmap;

    /**
     * 地图状态更新
     */
    private MapStatusUpdate update = null;

    /**
     * 实时点覆盖物
     */
    private OverlayOptions realtimeOptions = null;

    /**
     * 开始点覆盖物
     */
    private OverlayOptions startOptions = null;

    /**
     * 结束点覆盖物
     */
    private OverlayOptions endOptions = null;

    /**
     * 路径覆盖物
     */
    private PolylineOptions polyLine = null;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case IdentiferUtil.TIME_TASK:
                    timeText.setText(GeneralUtil.secondsToString(msg.arg1));
                    break;
                case IdentiferUtil.SAVE_DATA_TO_BMOB_SUCCESS:
                    runRecord.setIsSync(true);
                    DBManager.getInstance(context).insertRunRecord(runRecord);
                    break;
                case IdentiferUtil.SAVE_DATA_TO_BMOB_FAILURE:
                    runRecord.setIsSync(false);
                    DBManager.getInstance(context).insertRunRecord(runRecord);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    /****
     * 当前定位的经纬度坐标
     */
    private LatLng mLocationLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run);
        initToolBar();
        initView();
        baiduMap = mapView.getMap();
        setOnLocationChangeListener(new OnLocationChangeListener() {
            @Override
            public void onLocationChange(BDLocation bdLocation) {
                double latitude = bdLocation.getLatitude(); //纬度
                double longitude = bdLocation.getLongitude(); // 经度
                double radius = bdLocation.getRadius(); //精度
                if (bdLocation.hasSpeed()) {
                    float speed = bdLocation.getSpeed();
                    speedList.add(speed);
                    Log.i("TAG", "速度" + speed);
                }
                LatLng latLng = new LatLng(latitude, longitude); //坐标点
                if (null == mLocationLatLng) {
                    mLocationLatLng = new LatLng(latitude, longitude);
                    MapStatus mapStatus = new MapStatus.Builder().target(mLocationLatLng).zoom(mDefaultZoom).build();
                    update = MapStatusUpdateFactory.newMapStatus(mapStatus);
                    baiduMap.setMapStatus(update);
                }
                if(mCheckBox.isChecked()) {
                    latLng = mockLatLng();
                }
                if (Math.abs(latitude - 0.0) < 0.000001 && Math.abs(longitude - 0.0) < 0.000001) {
                } else {
                    if (pointList.size() < 1) { //初次定位
                        pointList.add(latLng);
                    } else {
                        LatLng lastPoint = pointList.get(pointList.size() - 1);//上一次定位坐标点
                        double rang = DistanceUtil.getDistance(lastPoint, latLng); // 两次定位的距离
                        if (rang > MIN_DISTANCE) {
                            distance = distance + rang;
                            pointList.add(latLng);
                        }
                    }
                    distanceText.setText(GeneralUtil.doubleToString(distance));
                }
                drawTrace(latLng);
            }
        });
        onStartBtnClick();
    }

    /****
     * 模拟跑步绘制 路线图
     * @return
     */
    private LatLng mockLatLng() {
        if (null != mLocationLatLng) {
            DecimalFormat decimalFormat = new DecimalFormat("#.000000");
            double latitude = mLocationLatLng.latitude;
            latitude += new Random().nextDouble() / 25000;
            double longitude = mLocationLatLng.longitude;
            longitude += new Random().nextDouble() / 15000;
            System.out.println("mock Latitude = " + decimalFormat.format(latitude) + " Longitude = " + decimalFormat.format(longitude));
            LatLng latLng = new LatLng(latitude, longitude); //坐标点
            mLocationLatLng = latLng;
            return latLng;
        }
        return null;
    }

    @Override
    public boolean isAutoLocation() {
        return false;
    }

    private void initToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar_run);
        toolbar.setTitle("跑步");
        toolbar.setNavigationIcon(R.drawable.back);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    /**
     * 初始化组件
     */
    private void initView() {
        mCheckBox = findViewById(R.id.checkbox);
        mapView = (MapView) findViewById(R.id.run_mapview);
        timeText = (TextView) findViewById(R.id.run_time_text);
        distanceText = (TextView) findViewById(R.id.run_distance_text);
        stateText = (TextView) findViewById(R.id.run_state_text);
        startRelative = (RelativeLayout) findViewById(R.id.run_init_run_relative);
        pauseLinear = (LinearLayout) findViewById(R.id.run_pause_run_linear);
        continueImg = (ImageView) findViewById(R.id.run_continue_img);
        startOrPauseImg = (ImageView) findViewById(R.id.run_start_or_pause_img);
        stopImg = (ImageView) findViewById(R.id.run_stop_img);

        continueImg.setOnClickListener(this);
        startOrPauseImg.setOnClickListener(this);
        stopImg.setOnClickListener(this);
        mapView.setOnClickListener(this);

    }

    /**
     * 绘制轨迹
     *
     * @param latLng
     */
    private void drawTrace(LatLng latLng) {
        float zoom = baiduMap.getMapStatus().zoom;
        Log.i("TAG", "绘制实时点 zoom = " + zoom);
        baiduMap.clear(); //清除覆盖物
        MapStatus mapStatus = new MapStatus.Builder().target(latLng).zoom(zoom).build();
        update = MapStatusUpdateFactory.newMapStatus(mapStatus);
        //实时点
        realtimeBitmap = BitmapDescriptorFactory.fromResource(R.drawable.point);
        if (isStart) {
            realtimeOptions = new MarkerOptions().position(latLng).icon(realtimeBitmap)
                    .zIndex(9).draggable(true);
        }
        // 开始点
        BitmapDescriptor startBitmap = BitmapDescriptorFactory.fromResource(R.drawable.startpoint);

        if (pointList.size() > 1) {
            startOptions = new MarkerOptions().position(pointList.get(0)).
                    icon(startBitmap).zIndex(9).draggable(true);
        }

        // 路线
        if (pointList.size() >= 2) {
            polyLine = new PolylineOptions().width(6).color(Color.GREEN).points(pointList);
        }
        addMarker();
    }

    /**
     * 添加地图覆盖物
     */
    private void addMarker() {

        Log.i("TAG", "添加覆盖物");
        if (null != update) {
            baiduMap.setMapStatus(update);
        }
        //开始点覆盖物
        if (null != startOptions) {
            baiduMap.addOverlay(startOptions);
        }
        // 路线覆盖物
        if (null != polyLine) {
            baiduMap.addOverlay(polyLine);
        }
        // 实时点覆盖物
        if (null != realtimeOptions) {
            baiduMap.addOverlay(realtimeOptions);
        }
        //结束点覆盖物
        if (null != endOptions) {
            baiduMap.addOverlay(endOptions);
        }
    }

    /**
     * 绘制最终完成地图
     */
    private void drawFinishMap() {
        baiduMap.clear();
        LatLng startLatLng = pointList.get(0);
        LatLng endLatLng = pointList.get(pointList.size() - 1);
        //地理范围
        LatLngBounds bounds = new LatLngBounds.Builder().include(startLatLng).include(endLatLng).build();
        update = MapStatusUpdateFactory.newLatLngBounds(bounds);
        if (pointList.size() >= 2) {
            // 开始点
            BitmapDescriptor startBitmap = BitmapDescriptorFactory.fromResource(R.drawable.startpoint);
            startOptions = new MarkerOptions().position(startLatLng).
                    icon(startBitmap).zIndex(9).draggable(true);

            // 终点
            BitmapDescriptor endBitmap = BitmapDescriptorFactory.fromResource(R.drawable.endpoint);
            endOptions = new MarkerOptions().position(endLatLng)
                    .icon(endBitmap).zIndex(9).draggable(true);
            polyLine = new PolylineOptions().width(6).color(Color.GREEN).points(pointList);
        } else {
            //实时点
            realtimeBitmap = BitmapDescriptorFactory.fromResource(R.drawable.point);
            realtimeOptions = new MarkerOptions().position(startLatLng).icon(realtimeBitmap)
                    .zIndex(9).draggable(true);
        }
        addMarker();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 开始计时
     */
    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
        }
        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    time++;
                    Message msg = new Message();
                    msg.what = IdentiferUtil.TIME_TASK;
                    msg.arg1 = time;
                    handler.sendMessage(msg);
                }
            };
        }
        if (timer != null && timerTask != null) {
            timer.schedule(timerTask, 1000, 1000);
        }

    }

    /**
     * 结束计时
     */
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.run_start_or_pause_img:
                if (isStart) {//已开始，暂停按钮
                    stopTimer(); //停止计时
                    isStart = false;
                    startRelative.setVisibility(View.GONE);
                    pauseLinear.setVisibility(View.VISIBLE);
                    stopLocation();
                } else {
                    onStartBtnClick();
                }
                break;
            case R.id.run_continue_img:// 继续
                onStartBtnClick();
                break;
            case R.id.run_stop_img: //停止,完成
                stopLocation();
                stopTimer(); // 停止计时
                showDialog();
                break;
            case R.id.dialog_continue_run:
                onStartBtnClick();
                dialog.dismiss();
                break;
            case R.id.dialog_end_run:
                //绘制完成轨迹图
                drawFinishMap();
                //截屏
                mapScreenShot();
                dialog.dismiss();
                break;
        }
    }

    private void onStartBtnClick() {
        //未开始，开始按钮
        isStart = true;
        startTimer();
        startRelative.setVisibility(View.VISIBLE);
        pauseLinear.setVisibility(View.GONE);
        startOrPauseImg.setImageResource(R.drawable.run_stop);
        stateText.setText("暂停");
        startLocation();
    }


    /**
     * 地图截屏
     */

    private void mapScreenShot() {
        baiduMap.snapshot(new BaiduMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                //将bitmap存储到文件中
                Log.i("TAG", "截图成功");
                picPath = FileUtil.saveBitmapToFile(bitmap, "mapshot");
                //保存记录
                saveRunRecord();
            }
        });
    }

    /**
     * 保存跑步记录
     */
    private void saveRunRecord() {
        runRecord = new RunRecord();
        String id = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        Log.i("TAG", "id" + id);
        runRecord.setRecordid(id);
        runRecord.setPoints(pointList);
        runRecord.setDistance(distance);
        runRecord.setTime(time);
        runRecord.setUserId(System.currentTimeMillis() + "");
        runRecord.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        runRecord.setMapShotPath(picPath);
        runRecord.setSpeeds(speedList);
        if (GeneralUtil.isNetworkAvailable(context)) { //网络连接
            runRecord.setIsSync(true);
            //存储到服务器端
            runRecord.save(context, new SaveListener() {
                @Override
                public void onSuccess() {
                    Log.i("TAG", "成功上传到云端");
                    Message msg = new Message();
                    msg.what = IdentiferUtil.SAVE_DATA_TO_BMOB_SUCCESS;
                    handler.sendMessage(msg);
                }

                @Override
                public void onFailure(int i, String s) {
                    Message msg = new Message();
                    msg.what = IdentiferUtil.SAVE_DATA_TO_BMOB_FAILURE;
                    handler.sendMessage(msg);
                }
            });
        } else {
            runRecord.setIsSync(false);
            DBManager.getInstance(context).insertRunRecord(runRecord);
        }
    }

    /**
     * 展示dialog
     */
    private void showDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_run_finish_layout, null);
        dialog = new AlertDialog.Builder(RunActivity.this).create();
        dialog.show();
        dialog.setContentView(view);
        dialogContinue = (TextView) view.findViewById(R.id.dialog_continue_run);
        dialogEnd = (TextView) view.findViewById(R.id.dialog_end_run);
        dialogContinue.setOnClickListener(this);
        dialogEnd.setOnClickListener(this);
    }

}
