package com.phhc.beauty.ShareFace;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.RatingBar;
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


public class ScoreDetail extends Activity implements AbsListView.OnScrollListener, View.OnClickListener {

    private Intent intent;
    private List<AVObject> list;
    private ListView listView;
    private MyAdapter myAdapter;
    private Dialog progressDialog;
    private String time, picID;
    private SwipeRefreshLayout swipeRefreshLayout;
    private static int showNum;
    private int preLast;
    private TextView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_detail);

        back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(this);
        intent = getIntent();
        picID = intent.getStringExtra("picID");
        list = new ArrayList<>();
        myAdapter = new MyAdapter();
        progressDialog = ProgressDialog.show(this, "", "数据加载中，请稍后...", true);
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
        listView = (ListView) findViewById(R.id.listView);
        myAdapter.setList(list);
        listView.setAdapter(myAdapter);
        listView.setOnScrollListener(this);
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
            progressDialog.dismiss();
            swipeRefreshLayout.setRefreshing(false);
            myAdapter.notifyDataSetChanged();
        }
    }

    public List<AVObject> findAVObjects() {
        // 查询当前AVObject列表
        AVQuery<AVObject> query = new AVQuery<>("Score");
        // 按照更新时间降序排序
        query.orderByDescending("createdAt");
        query.whereEqualTo("picID", picID);
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

            view = LayoutInflater.from(ScoreDetail.this).inflate(R.layout.score_detail_item, null);
            final ImageView imageView = (ImageView) view.findViewById(R.id.pic);
            final TextView honeyName = (TextView) view.findViewById(R.id.honeyName);
            final TextView scoreText = (TextView) view.findViewById(R.id.scoreText);
            String commenterID = this.list.get(position).getString("commenterID");
            scoreText.setText(this.list.get(position).getInt("score") + "（分）");
            AVQuery<AVUser> avUserAVQuery = AVUser.getQuery();
            avUserAVQuery.whereEqualTo("objectId", commenterID);
            avUserAVQuery.findInBackground(new FindCallback<AVUser>() {
                @Override
                public void done(List<AVUser> list, AVException e) {
                    if (e == null) {
                        AVUser avUser = list.get(0);
                        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(ScoreDetail.this));
                        if (avUser.getAVFile("portrait") != null) {
                            ImageLoader.getInstance().displayImage(avUser.getAVFile("portrait").getUrl(), imageView, StatusUtils.normalImageOptions);
                        }
                        honeyName.setText(avUser.getUsername());
                    } else {
                        Toast.makeText(ScoreDetail.this, "aaaaaa", Toast.LENGTH_SHORT).show();
                    }
                }
            });

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
