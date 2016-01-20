package com.phhc.beauty.ShareFace;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.PushService;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
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
import com.phhc.beauty.utils.App;
import com.phhc.beauty.utils.StatusUtils;

import java.util.ArrayList;
import java.util.List;


public class FaceMap extends Activity implements View.OnClickListener, RadarUploadInfoCallback, RadarSearchListener, BDLocationListener, BaiduMap.OnMarkerClickListener, BaiduMap.OnMapClickListener {

    private LatLng pt = null;
    private Intent intent;
    private MapView mMapView = null;
    // 定位相关
    LocationClient mLocClient;
    BDLocation bdLocation = new BDLocation();
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
    private TextView back;
    private ImageView camera;
    private RelativeLayout faceMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_map);

        back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(this);
        App.getInstance().addActivity(this);
        // 设置默认打开的 Activity
        PushService.setDefaultPushCallback(this, FaceMap.class);
        pts = new ArrayList<>();
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        // 开启定位图层
        mBaiduMap.setOnMarkerClickListener(this);
        mBaiduMap.setOnMapClickListener(this);
        mBaiduMap.setMyLocationEnabled(true);
        //周边雷达设置监听
        RadarSearchManager.getInstance().addNearbyInfoListener(this);
        //周边雷达设置用户，id为空默认是设备标识
        //这里的setUserID是作为周边雷达的唯一标志，可以用用户ID来唯一标志，这里我是用设置的UUID来设置的，你改的时候可以将当前用户的ID来设置这个参数。
        RadarSearchManager.getInstance().setUserID(AVUser.getCurrentUser().getObjectId());
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(this);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(false);// 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(20000);
        mLocClient.setLocOption(option);
        mLocClient.start();
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
        }
    }

    @Override
    protected void onResume() {
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // 退出时销毁定位
        mLocClient.stop();
        // 释放周边雷达相关
        RadarSearchManager.getInstance().removeNearbyInfoListener(this);
        RadarSearchManager.getInstance().clearUserInfo();
        RadarSearchManager.getInstance().destroy();
        // 释放地图
        if (mCurrentMarker != null) {
            mCurrentMarker.recycle();
        }
        mMapView.onDestroy();
        mBaiduMap = null;
        super.onDestroy();
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//
//        // 退出时销毁定位
//        mLocClient.stop();
//        // 释放周边雷达相关
//        RadarSearchManager.getInstance().removeNearbyInfoListener(this);
//        RadarSearchManager.getInstance().clearUserInfo();
//        RadarSearchManager.getInstance().destroy();
//        // 释放地图
//        mCurrentMarker.recycle();
//        mMapView.onDestroy();
//        mBaiduMap = null;
//        return super.onKeyDown(keyCode, event);
//    }

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
        ImageView imageView = new ImageView(FaceMap.this);
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(FaceMap.this));
        if (AVUser.getCurrentUser().getAVFile("portrait") == null) {

        } else {
            ImageLoader.getInstance().displayImage(AVUser.getCurrentUser().getAVFile("portrait").getUrl(), imageView, StatusUtils.normalImageOptions);
        }
        RelativeLayout relativeLayout = new RelativeLayout(FaceMap.this);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(70, 70);
        relativeLayout.addView(imageView, rlp);
        // 修改为自定义marker
//            mCurrentMarker = BitmapDescriptorFactory
//                    .fromView(relativeLayout);
        mBaiduMap
                .setMyLocationConfigeration(new MyLocationConfiguration(
                        mCurrentMode, true, mCurrentMarker));
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
            info.comments = AVUser.getCurrentUser().getUsername();
            RadarSearchManager.getInstance().uploadInfoRequest(info);
            pts.add(0, pt);
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

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        return false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        final FaceDescriptionDialog cdd = new FaceDescriptionDialog(FaceMap.this, "");
        cdd.setCancelable(true);
        cdd.show();
        return false;
    }

    @Override
    public void onGetNearbyInfoList(RadarNearbyResult result, RadarSearchError error) {
        if (error == RadarSearchError.RADAR_NO_ERROR) {
            // 获取成功
            listResult = result;
            // 处理数据
            parseResultToMap(listResult);
        } else {
            Toast.makeText(this, "您附近暂时没有美颜用户哦~", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGetUploadState(RadarSearchError error) {

        if (error == RadarSearchError.RADAR_NO_ERROR) {
            // 上传成功
            Toast.makeText(FaceMap.this, "单次上传位置成功", Toast.LENGTH_SHORT)
                    .show();
        } else {
//            // 上传失败
            Toast.makeText(FaceMap.this, "单次上传位置失败", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onGetClearInfoState(RadarSearchError radarSearchError) {

    }

    @Override
    public RadarUploadInfo onUploadInfoCallback() {
        return null;
    }

    /**
     * 更新结果地图
     *
     * @param res
     */
    public void parseResultToMap(RadarNearbyResult res) {
        final RadarNearbyResult resParam = res;
        if (mBaiduMap != null) {
            mBaiduMap.clear();
        }
        if (resParam != null && resParam.infoList != null && resParam.infoList.size() > 0) {
            for (int i = 0; i < resParam.infoList.size(); i++) {
                final int j = i;
                final ImageView imageView = new ImageView(FaceMap.this);
                imageView.setImageResource(R.mipmap.portrait_backup);
//                ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(FaceMap.this));
//                ImageLoader.getInstance().displayImage(resParam.infoList.get(i).comments, imageView, StatusUtils.normalImageOptions);
                AVQuery<AVObject> query2 = new AVQuery<>("FaceMap");
                query2.whereEqualTo("userID", resParam.infoList.get(j).userID);
                query2.orderByDescending("createdAT");
                query2.findInBackground(new FindCallback<AVObject>() {
                    @Override
                    public void done(List<AVObject> list, AVException e) {
                        if (e == null) {
                            if (list.size() == 0) {
                                AVQuery<AVUser> query = AVUser.getQuery();
                                query.whereEqualTo("objectId", resParam.infoList.get(j).userID);
                                query.findInBackground(new FindCallback<AVUser>() {
                                    @Override
                                    public void done(List<AVUser> list, AVException e) {
                                        if (list.size() != 0) {
                                            ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(FaceMap.this));
                                            if (list.get(0).getAVFile("portrait") == null) {

                                            } else {
                                                ImageLoader.getInstance().displayImage(list.get(0).getAVFile("portrait").getUrl(), imageView, StatusUtils.normalImageOptions);
                                                RelativeLayout relativeLayout = new RelativeLayout(FaceMap.this);
                                                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(70, 70);
                                                rlp.addRule(RelativeLayout.CENTER_IN_PARENT);
                                                relativeLayout.addView(imageView, rlp);
                                                //设置地图悬浮层
                                                try {
                                                    mCurrentMarker = BitmapDescriptorFactory
                                                            .fromView(relativeLayout);
                                                    MarkerOptions option = new MarkerOptions().icon(mCurrentMarker).position(
                                                            resParam.infoList.get(j).pt);
                                                    Bundle des = new Bundle();
                                                    if (resParam.infoList.get(j).comments == null
                                                            || resParam.infoList.get(j).comments.equals("")) {
                                                        //这里的des是周边雷达唯一标志用户的字段，这里用用户的用户名来标志
                                                        des.putString("des", AVUser.getCurrentUser().getObjectId());
                                                    } else {
                                                        des.putString("des", resParam.infoList.get(j).comments);
                                                    }
                                                    option.extraInfo(des);
                                                    mBaiduMap.addOverlay(option);
                                                } catch (Exception e1) {

                                                }
                                            }
                                        }
                                    }
                                });
                            } else {
                                ImageLoader.getInstance().displayImage(list.get(0).getAVFile("portrait").getUrl(), imageView, StatusUtils.normalImageOptions);
                                RelativeLayout relativeLayout = new RelativeLayout(FaceMap.this);
                                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(70, 70);
                                rlp.addRule(RelativeLayout.CENTER_IN_PARENT);
                                relativeLayout.addView(imageView, rlp);
                                //设置地图悬浮层
                                try {
                                    mCurrentMarker = BitmapDescriptorFactory
                                            .fromView(relativeLayout);
                                    MarkerOptions option = new MarkerOptions().icon(mCurrentMarker).position(
                                            resParam.infoList.get(j).pt);
                                    Bundle des = new Bundle();
                                    if (resParam.infoList.get(j).comments == null
                                            || resParam.infoList.get(j).comments.equals("")) {
                                        //这里的des是周边雷达唯一标志用户的字段，这里用用户的用户名来标志
                                        des.putString("des", AVUser.getCurrentUser().getObjectId());
                                    } else {
                                        des.putString("des", resParam.infoList.get(j).comments);
                                    }
                                    option.extraInfo(des);
                                    mBaiduMap.addOverlay(option);
                                } catch (Exception e1) {

                                }
                            }
                        }
                    }
                });


            }
        }
    }
}
