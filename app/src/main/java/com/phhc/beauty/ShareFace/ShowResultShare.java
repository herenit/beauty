package com.phhc.beauty.ShareFace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.SaveCallback;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.radar.RadarNearbyResult;
import com.baidu.mapapi.radar.RadarNearbySearchOption;
import com.baidu.mapapi.radar.RadarSearchError;
import com.baidu.mapapi.radar.RadarSearchListener;
import com.baidu.mapapi.radar.RadarSearchManager;
import com.baidu.mapapi.radar.RadarUploadInfo;
import com.baidu.mapapi.radar.RadarUploadInfoCallback;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.phhc.beauty.R;
import com.phhc.beauty.interfaces.LoadDataListener;
import com.phhc.beauty.utils.StatusUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Objects;


public class ShowResultShare extends Activity implements View.OnClickListener, RadarUploadInfoCallback, RadarSearchListener, BDLocationListener, BaiduMap.OnMarkerClickListener, BaiduMap.OnMapClickListener, LoadDataListener {

    private RelativeLayout uploadServer;
    private ImageView pic;
    private Drawable d;
    private Bitmap bitmap;
    byte[] bytes;
    private RelativeLayout showOff, perfectSuggest;
    private ImageView help;
    private float Age, Skin, Total;
    private int ExpressionLabel, FlawLabel;
    long timeSwapBuff = 0L;
    private LatLng pt = null;
    private Intent intent;
    private MapView mMapView = null;
    // 定位相关
    LocationClient mLocClient;
    BDLocation bdLocation = new BDLocation();
    public MyLocationListenner myListener = new MyLocationListenner();
    BaiduMap mBaiduMap;
    private int uploadFlag = 1;
    // UI相关
    boolean isFirstLoc = true;// 是否首次定位
    private ProgressDialog progressDialog;
    private List<LatLng> pts;
    // 周边雷达相关
    RadarNearbyResult listResult = null;
    private double longitude, latitude;
    //用于显示本人的自定义头像
    private BitmapDescriptor mCurrentMarker;
    private MyLocationConfiguration.LocationMode mCurrentMode;
    private TextView back, score, face, age, skin, flaw;
    private ImageView camera;
    private RelativeLayout faceMap;
    private CustomDialog cdd;

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        return false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public void onGetNearbyInfoList(RadarNearbyResult radarNearbyResult, RadarSearchError radarSearchError) {

    }

    @Override
    public void onGetUploadState(RadarSearchError radarSearchError) {

    }

    @Override
    public void onGetClearInfoState(RadarSearchError radarSearchError) {

    }

    @Override
    public RadarUploadInfo onUploadInfoCallback() {
        return null;
    }

    @Override
    public void upload() {
        progressDialog = ProgressDialog.show(this, "", "上传中,请稍后...", false);
        cdd.dismiss();
        uploadLeanCloud();
    }

    /**
     * 定位SDK监听函数
     */
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null)
                return;
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                            // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(0).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            ImageView imageView = new ImageView(ShowResultShare.this);
            ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(ShowResultShare.this));
            if (AVUser.getCurrentUser().getAVFile("portrait") == null) {

            } else {
                ImageLoader.getInstance().displayImage(AVUser.getCurrentUser().getAVFile("portrait").getUrl(), imageView, StatusUtils.normalImageOptions);
            }
            RelativeLayout relativeLayout = new RelativeLayout(ShowResultShare.this);
            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(70, 70);
            relativeLayout.addView(imageView, rlp);
            // 修改为自定义marker
//            mCurrentMarker = BitmapDescriptorFactory
//                    .fromView(relativeLayout);
            if (mBaiduMap != null) {
                mBaiduMap.setMyLocationData(locData);
            }
            pt = new LatLng(location.getLatitude(), location.getLongitude());
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            if (pt != null && uploadFlag == 1) {
                //上传自己的位置信息
                RadarUploadInfo info = new RadarUploadInfo();
                info.pt = pt;
//                info.comments = AVUser.getCurrentUser().getObjectId();
                pts.add(0, pt);
                RadarSearchManager.getInstance().uploadInfoRequest(info);
                new Upload().execute();
                uploadFlag = 0;
                //发起检索周边的请求
                RadarNearbySearchOption option = new RadarNearbySearchOption()
                        .centerPt(pt).pageNum(0).radius(20000);
                RadarSearchManager.getInstance().nearbyInfoRequest(option);
            }
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(u);
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {

        }
    }

    // 在工作线程中上传当前位置信息 尼见 2014-02-28
    class Upload extends AsyncTask<Object, Object, Object> {

        @Override
        protected Objects doInBackground(Object... params) {
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_result_share);

        skin = (TextView) findViewById(R.id.skin);
        flaw = (TextView) findViewById(R.id.flaw);
        face = (TextView) findViewById(R.id.face);
        score = (TextView) findViewById(R.id.score);
        age = (TextView) findViewById(R.id.age);
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);// 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        // TODO
        //这里设置的是雷达搜索的范围，如果想增大搜索范围可以自己定义
        option.setScanSpan(20000);
        mLocClient.setLocOption(option);
        mLocClient.start();
        //周边雷达设置监听
        RadarSearchManager.getInstance().addNearbyInfoListener(this);
        //周边雷达设置用户，id为空默认是设备标识
        //TODO
        //这里的setUserID是作为周边雷达的唯一标志，可以用用户ID来唯一标志，这里我是用设置的UUID来设置的，你改的时候可以将当前用户的ID来设置这个参数。
        RadarSearchManager.getInstance().setUserID(AVUser.getCurrentUser().getObjectId());
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;

        help = (ImageView) findViewById(R.id.help);
        help.setOnClickListener(this);
        showOff = (RelativeLayout) findViewById(R.id.showOff);
        showOff.setOnClickListener(this);
        perfectSuggest = (RelativeLayout) findViewById(R.id.perfectSuggest);
        perfectSuggest.setOnClickListener(this);
        Bundle extras = getIntent().getExtras();
        String fileName = extras.getString("picname");
        File filePath = getFileStreamPath(fileName);
        d = Drawable.createFromPath(filePath.toString());
        bytes = (byte[]) extras.getByteArray("bitmap");
        pic = (ImageView) findViewById(R.id.pic);
        pic.setBackgroundDrawable(d);
        Total = extras.getFloat("Total");
        FlawLabel = extras.getInt("FlawLabel");
        ExpressionLabel = extras.getInt("ExpressionLabel");
        Age = extras.getFloat("Age");
        Skin = extras.getFloat("Skin");
        score.setText("" + Total);
        switch (ExpressionLabel) {
            case 0:
                face.setText("非常不高兴");
                break;
            case 1:
                face.setText("不高兴");
                break;
            case 2:
                face.setText("严肃");
                break;
            case 3:
                face.setText("高兴");
                break;
            case 4:
                face.setText("非常高兴");
                break;
        }
        switch (FlawLabel) {
            case 0:
                flaw.setText("完美无瑕");
                break;
            case 1:
                flaw.setText("非常微小");
                break;
            case 2:
                flaw.setText("影响美观");
                break;
            case 3:
                flaw.setText("严重影响美观");
                break;
            case 4:
                flaw.setText("毁容");
                break;
        }
        skin.setText(Skin + "");
        if (Age < 18 && Age >= 0) {
            age.setText("花样年华");
        }
        if (Age >= 18 && Age < 35) {
            age.setText("奋斗时光");
        }
        if (Age >= 35 && Age < 55) {
            age.setText("不惑岁月");
        }
        if (Age >= 55) {
            age.setText("流光岁月");
        }
        back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.showOff:
                cdd = new CustomDialog(ShowResultShare.this, "");
                cdd.setCancelable(true);
                cdd.show();
                break;
            case R.id.perfectSuggest:
                AlertDialog dialog = getAlertDialogWithSuggest();
                dialog.show();
                break;
            case R.id.help:
                AlertDialog dialog2 = getAlertDialogWithCellphoneWrong();
                dialog2.show();
                break;
        }
    }

    // 手机号不正确警告框 尼见 2015-02-28
    AlertDialog getAlertDialogWithCellphoneWrong() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);

        builder.setTitle("提示");
        builder.setMessage("该评分纯属娱乐...");
        builder.setPositiveButton("确定", null);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        return dialog;
    }

    // 手机号不正确警告框 尼见 2015-02-28
    AlertDialog getAlertDialogWithSuggest() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);

        builder.setTitle("提示");
        builder.setMessage("您...能使您变得更...");
        builder.setPositiveButton("确定", null);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        return dialog;
    }

    public void uploadLeanCloud() {
        AVObject post = new AVObject("FaceMap");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        bitmap.recycle();
        bytes = baos.toByteArray();
        final AVFile file = new AVFile("photo", bytes);
        post.put("portrait", file);
        post.put("userID", AVUser.getCurrentUser().getObjectId());
        post.saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                if (e == null) {
                    progressDialog.dismiss();
                    Toast toast = Toast.makeText(ShowResultShare.this, "发布成功!", Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    progressDialog.dismiss();
                    // 保存失败，输出错误信息
                    Toast toast = Toast.makeText(ShowResultShare.this, "发布失败！", Toast.LENGTH_LONG);
                    toast.show();
                    Log.e("aaa", e + "");
                }
            }
        });
    }

}
