package com.phhc.beauty.ShareFace;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.FindCallback;
import com.phhc.beauty.R;
import com.phhc.beauty.utils.StatusUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import me.iwf.photopicker.utils.PhotoPickerIntent;


public class ShareFace extends Activity implements View.OnClickListener, AbsListView.OnScrollListener {

    public final static int REQUEST_CODE = 1;
    private List<AVObject> list;
    private MyAdapter myAdapter;
    private Dialog progressDialog;
    private String time;
    private SwipeRefreshLayout swipeRefreshLayout;
    private static int showNum;
    private int preLast;
    private Intent intent;
    private TextView back;
    private ImageView camera;
    private RelativeLayout faceMap;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_face);

        listView = (ListView) findViewById(R.id.listView);
        list = new ArrayList<>();
        myAdapter = new MyAdapter();
        ShareFace.this.progressDialog =
                ProgressDialog.show(this, "", "数据加载中，请稍后...", true);
        progressDialog.setCancelable(true);
        new RemoteDataTask().execute();
        // 设置ListView
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showNum = 0;
                new RemoteDataTask().execute();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.orange, R.color.green, R.color.blue);
        myAdapter.setList(list);
        listView.setAdapter(myAdapter);
        listView.setOnScrollListener(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                AVObject avObject = (AVObject) adapterView.getItemAtPosition(position);
                Intent intent = new Intent();
                intent.putExtra("url", avObject.getAVFile("pic").getUrl());
                intent.putExtra("honeyName", avObject.getString("honeyName"));
                intent.putExtra("picID", avObject.getObjectId());
                intent.putExtra("stage", avObject.getString("stage"));
                intent.putExtra("expression", avObject.getString("expression"));
                intent.putExtra("skin", avObject.getString("skin"));
                intent.putExtra("score", avObject.getString("score"));
                intent.setAction("android.intent.action.ShowDetail");
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(this);
        camera = (ImageView) findViewById(R.id.camera);
        camera.setOnClickListener(this);
        faceMap = (RelativeLayout) findViewById(R.id.faceMap);
        faceMap.setOnClickListener(this);
    }


    public void previewPhoto(Intent intent) {
        startActivityForResult(intent, REQUEST_CODE);
    }

    enum RequestCode {
        Button(R.id.button),
        ButtonOnePhoto(R.id.camera),
        ;

        @IdRes
        final int mViewId;
        RequestCode(@IdRes int viewId) {
            mViewId = viewId;
        }
    }

    private void checkPermission(@NonNull RequestCode requestCode) {

        int permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionState != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        requestCode.ordinal());

            }
        } else {
            // Permission granted
            onClick(requestCode.mViewId);
        }
    }

    private void onClick(@IdRes int viewId) {

        switch (viewId) {


            case R.id.camera: {
                PhotoPickerIntent intent = new PhotoPickerIntent(ShareFace.this);
                intent.setPhotoCount(1);
                intent.setShowCamera(true);
                startActivityForResult(intent, REQUEST_CODE);
                break;
            }

        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.camera:
//                intent = new Intent();
//                intent.setAction("android.intent.action.ImageGrid");
//                startActivity(intent);
                checkPermission(RequestCode.ButtonOnePhoto);
                break;
            case R.id.faceMap:
                intent = new Intent();
                intent.setAction("android.intent.action.FaceMap");
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onResume() {
        // AVObject Auto-generated method stub
        super.onResume();
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {

    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        switch (absListView.getId()) {
            case R.id.listView:
                final int lastItem = firstVisibleItem + visibleItemCount;
                if (lastItem == totalItemCount) {
                    if (preLast != lastItem) {
                        if (lastItem % 10 == 0) {
                            new RemoteDataTask().execute();
                            progressDialog = ProgressDialog.show(this, "", "数据加载中，请稍后...", true);
                            progressDialog.setCancelable(true);
                        } else {
                            Toast toast = Toast.makeText(this, "没有更多了哦~", Toast.LENGTH_LONG);
                            toast.show();
                        }
                        preLast = lastItem;
                    }
                }
        }
    }

    private class RemoteDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            list = findAVObjects();
            myAdapter.setList(list);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void result) {
            // 设置初期数据
            ShareFace.this.progressDialog.dismiss();
            swipeRefreshLayout.setRefreshing(false);
            myAdapter.notifyDataSetChanged();
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    AVObject avObject = (AVObject) adapterView.getItemAtPosition(position);
                    Intent intent = new Intent();
                    intent.putExtra("url", avObject.getAVFile("pic").getUrl());
                    intent.putExtra("honeyName", avObject.getString("honeyName"));
                    intent.putExtra("picID", avObject.getObjectId());
                    intent.putExtra("stage", avObject.getString("stage"));
                    intent.putExtra("expression", avObject.getString("expression"));
                    intent.putExtra("skin", avObject.getString("skin"));
                    intent.putExtra("score", avObject.getString("score"));
                    intent.setAction("android.intent.action.ShowDetail");
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            });
        }
    }

    public static List<AVObject> findAVObjects() {
        // 查询当前AVObject列表
        AVQuery<AVObject> query = new AVQuery<>("ShareFace");
        // 按照更新时间降序排序
        query.orderByDescending("createdAt");
        // 最大返回1000条
        showNum = showNum + 10;
        query.limit(showNum);
        try {
            return query.find();
        } catch (AVException exception) {
            Log.e("tag", "Query AVObjects failed.", exception);
            return Collections.emptyList();
        }
    }

    public class MyAdapter extends BaseAdapter {

        List<AVObject> list;
        private Context context;

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int index) {
            return list.get(index);
        }

        @Override
        public long getItemId(int index) {
            return index;
        }

        @Override
        public View getView(int position, View view, ViewGroup arg2) {

            view = LayoutInflater.from(ShareFace.this).inflate(R.layout.share_face_item, null);
            ImageView imageView = (ImageView) view.findViewById(R.id.pic);
            final ImageView portrait = (ImageView)view.findViewById(R.id.portrait);
            TextView honeyName = (TextView) view.findViewById(R.id.honeyName);
            TextView time = (TextView) view.findViewById(R.id.time);
            TextView scoreNum = (TextView) view.findViewById(R.id.scoreNum);
            final TextView averageScore = (TextView) view.findViewById(R.id.averageScore);
            honeyName.setText(this.list.get(position).getString("honeyName"));
            Date date = new Date(this.list.get(position).getCreatedAt().getTime());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String time_param = format.format(date);
            time.setText(time_param);
            AVQuery<AVObject> avObjectAVQuery = new AVQuery<>("Score");
            avObjectAVQuery.whereEqualTo("picID", this.list.get(position).getObjectId());
            avObjectAVQuery.setLimit(1000);
            avObjectAVQuery.findInBackground(new FindCallback<AVObject>() {
                @Override
                public void done(List<AVObject> list, AVException e) {
                    if (list != null) {
                        int size = list.size();
                        long scoreAll = 0;
                        long averageScoreLong = 0;
                        for (int i = 0; i < size; i++) {
                            scoreAll += list.get(i).getInt("score");
                        }
                        if (size != 0) {
                            averageScoreLong = scoreAll / size;
                        }
                        averageScore.setText("(" + averageScoreLong + "分)");
                    }
                }
            });
            scoreNum.setText(this.list.get(position).getInt("scoreNum") + "");
            ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(ShareFace.this));
            ImageLoader.getInstance().displayImage(this.list.get(position).getAVFile("pic").getUrl(), imageView, StatusUtils.normalImageOptions);
            AVQuery<AVUser> query = AVUser.getQuery();
            query.whereEqualTo("objectId",this.list.get(position).getString("distributeID"));
            query.findInBackground(new FindCallback<AVUser>() {
                @Override
                public void done(List<AVUser> list, AVException e) {
                    if(e == null){
                        if(list.get(0).getAVFile("portrait") != null){
                            ImageLoader.getInstance().displayImage(list.get(0).getAVFile("portrait").getUrl(), portrait, StatusUtils.normalImageOptions);
                        }
                    }
                }
            });
            ImageLoader.getInstance().displayImage(this.list.get(position).getAVFile("pic").getUrl(), imageView, StatusUtils.normalImageOptions);

            return view;
        }

        public void setContext(Context context) {
            this.context = context;
        }

        public void setList(List<AVObject> list) {
            this.list = list;
        }
    }

}
