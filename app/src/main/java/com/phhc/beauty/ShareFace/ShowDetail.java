package com.phhc.beauty.ShareFace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.SaveCallback;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.phhc.beauty.R;
import com.phhc.beauty.utils.StatusUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class ShowDetail extends Activity implements View.OnClickListener, AbsListView.OnScrollListener {

    private List<AVObject> list;
    private MyAdapter myAdapter;
    private TextView back, honeyName, submit, likeNum, scoreNum, averageScore;
    private ImageView pic, more;
    private Intent intent;
    private RatingBar scoreBar;
    private String url, picID;
    private ProgressDialog progressDialog;
    private int score;
    private EditText comment;
    private RelativeLayout newComment, likeRL;
    private static int showNum;
    private ListView listView;
    private int preLast;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_detail);

        scrollView = (ScrollView) findViewById(R.id.scrollView);
        scrollView.fullScroll(ScrollView.FOCUS_UP);
        listView = (ListView) findViewById(R.id.listView);
        setListViewHeightBasedOnItems(listView);
        list = new ArrayList<>();
        myAdapter = new MyAdapter();
        scoreNum = (TextView) findViewById(R.id.scoreNum);
        averageScore = (TextView) findViewById(R.id.averageScore);
        likeNum = (TextView) findViewById(R.id.likeNum);
        likeRL = (RelativeLayout) findViewById(R.id.likeRL);
        likeRL.setOnClickListener(this);
        intent = getIntent();
        url = intent.getStringExtra("url");
        picID = intent.getStringExtra("picID");
        newComment = (RelativeLayout) findViewById(R.id.newComment);
        progressDialog = ProgressDialog.show(ShowDetail.this, "", "数据加载中，请稍后...", true);
        progressDialog.setCancelable(false);
        new RemoteDataTask().execute();
        myAdapter.setList(list);
        listView.setAdapter(myAdapter);
        listView.setOnScrollListener(this);
        listView.setOnTouchListener(new View.OnTouchListener() {
            // Setting on Touch Listener for handling the touch inside ScrollView
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Disallow the touch request for parent scroll on touch of child view
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        AVQuery<AVObject> avObjectAVQuery = new AVQuery<>("ShareFace");
        avObjectAVQuery.whereEqualTo("objectId", picID);
        avObjectAVQuery.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                if (e == null) {
                    likeNum.setText("有" + list.get(0).getInt("likeNum") + "人喜欢");
                }
            }
        });
        AVQuery<AVObject> avObjectAVQuery2 = new AVQuery<>("Score");
        avObjectAVQuery2.whereEqualTo("picID", picID);
        avObjectAVQuery2.setLimit(1000);
        avObjectAVQuery2.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> list, AVException e) {
                int size = list.size();
                long scoreAll = 0;
                long averageScoreLong = 0;
                for (int i = 0; i < size; i++) {
                    scoreAll += list.get(i).getInt("score");
                }
                if (size != 0) {
                    averageScoreLong = scoreAll / size;
                }
                scoreNum.setText(size + "人评分");
                averageScore.setText("平均分值(" + averageScoreLong + "分)");
            }
        });
        comment = (EditText) findViewById(R.id.comment);
        submit = (TextView) findViewById(R.id.submit);
        submit.setOnClickListener(this);
        honeyName = (TextView) findViewById(R.id.honeyName);
        more = (ImageView) findViewById(R.id.more);
        more.setOnClickListener(this);
        back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(this);
        scoreBar = (RatingBar) findViewById(R.id.scoreBar);
        scoreBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                score = (int) v;
                if (score <= 1) {
                    AlertDialog dialog = getAlertDialogWithLowScore();
                    dialog.show();
                } else if (score >= 5) {
                    AlertDialog dialog = getAlertDialogWithHighScore();
                    dialog.show();
                } else {
                    progressDialog = ProgressDialog.show(ShowDetail.this, "", "正在为该照片评分，请稍后...", true);
                    progressDialog.setCancelable(false);
                    AVObject post = new AVObject("Score");
                    post.put("picID", picID);
                    post.put("score", score * 20);
                    post.put("commenterID", AVUser.getCurrentUser().getObjectId());
                    post.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(AVException e) {
                            if (e == null) {
                                AVQuery<AVObject> query = new AVQuery<AVObject>("ShareFace");
                                query.whereEqualTo("objectId", picID);
                                query.findInBackground(new FindCallback<AVObject>() {
                                    @Override
                                    public void done(List<AVObject> list, AVException e) {
                                        if (e == null) {
                                            list.get(0).increment("scoreNum");
                                            list.get(0).saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(AVException e) {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(ShowDetail.this, "评分成功了哦~", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                });
                            } else {
                                Toast.makeText(ShowDetail.this, "出错了~", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
        pic = (ImageView) findViewById(R.id.pic);
        honeyName.setText(intent.getStringExtra("honeyName"));
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
        ImageLoader.getInstance().displayImage(url, pic, StatusUtils.normalImageOptions);
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

    public static boolean setListViewHeightBasedOnItems(ListView listView) {

        Adapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {

            int numberOfItems = listAdapter.getCount();

            // Get total height of all items.
            int totalItemsHeight = 0;
            for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
                View item = listAdapter.getView(itemPos, null, listView);
                item.measure(0, 0);
                totalItemsHeight += item.getMeasuredHeight();
            }

            // Get total height of all item dividers.
            int totalDividersHeight = listView.getDividerHeight() *
                    (numberOfItems - 1);

            // Set list height.
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalItemsHeight + totalDividersHeight;
            listView.setLayoutParams(params);
            listView.requestLayout();

            return true;

        } else {
            return false;
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
            if (list.size() == 0) {
                listView.setVisibility(View.GONE);
            }
            progressDialog.dismiss();
            myAdapter.notifyDataSetChanged();
        }
    }

    public List<AVObject> findAVObjects() {
        // 查询当前AVObject列表
        AVQuery<AVObject> query = new AVQuery<>("Comment");
        query.whereEqualTo("picID", picID);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.more:
                intent = new Intent();
                intent.putExtra("picID", picID);
                intent.setAction("android.intent.action.ScoreDetail");
                startActivity(intent);
                break;
            case R.id.submit:
                if (comment.getText().toString().length() == 0) {
                    Toast.makeText(this, "不要提交空内容哦~", Toast.LENGTH_SHORT).show();
                } else {
                    progressDialog = ProgressDialog.show(ShowDetail.this, "", "正在为该照片评分，请稍后...", true);
                    progressDialog.setCancelable(false);
                    final AVObject post = new AVObject("Comment");
                    post.put("content", comment.getText().toString());
                    post.put("picID", picID);
                    post.put("commentHoneyName", AVUser.getCurrentUser().getUsername());
                    post.put("commenterID", AVUser.getCurrentUser().getObjectId());
                    post.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(AVException e) {
                            if (e == null) {
                                progressDialog.dismiss();
                                Toast.makeText(ShowDetail.this, "评价成功了~", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ShowDetail.this, "出错了~", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                break;
            case R.id.likeRL:
                AVQuery<AVObject> avObjectAVQuery = new AVQuery<>("ShareFace");
                avObjectAVQuery.whereEqualTo("objectId", picID);
                avObjectAVQuery.findInBackground(new FindCallback<AVObject>() {
                    @Override
                    public void done(List<AVObject> list, AVException e) {
                        if (e == null) {
                            list.get(0).increment("likeNum");
                            list.get(0).saveInBackground();
                            likeNum.setText("有" + list.get(0).getInt("likeNum") + "人喜欢");
                            Toast.makeText(ShowDetail.this, "标记喜欢成功了~", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
        }
    }

    @Override
    public void onResume() {
        // AVObject Auto-generated method stub
        super.onResume();
    }

    // 手机号不正确警告框 尼见 2015-02-28
    AlertDialog getAlertDialogWithLowScore() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);

        builder.setTitle("提示");
        builder.setMessage("20分，看来TA不是你的菜，分数不够高哦");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                progressDialog = ProgressDialog.show(ShowDetail.this, "", "正在为该照片评分，请稍后...", true);
                progressDialog.setCancelable(false);
                AVObject post = new AVObject("Score");
                post.put("picID", picID);
                post.put("score", score * 20);
                post.put("commenterID", AVUser.getCurrentUser().getObjectId());
                post.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(AVException e) {
                        if (e == null) {
                            AVQuery<AVObject> query = new AVQuery<AVObject>("ShareFace");
                            query.whereEqualTo("objectId", picID);
                            query.findInBackground(new FindCallback<AVObject>() {
                                @Override
                                public void done(List<AVObject> list, AVException e) {
                                    if (e == null) {
                                        list.get(0).increment("scoreNum");
                                        list.get(0).saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(AVException e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(ShowDetail.this, "评分成功了哦~", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(ShowDetail.this, "出错了~", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        builder.setNegativeButton("打错了，重新打分", null);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        return dialog;
    }

    // 手机号不正确警告框 尼见 2015-02-28
    AlertDialog getAlertDialogWithHighScore() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);

        builder.setTitle("提示");
        builder.setMessage("20分，看来TA不是你的菜，分数不够高哦");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                progressDialog = ProgressDialog.show(ShowDetail.this, "", "正在为该照片评分，请稍后...", true);
                progressDialog.setCancelable(false);
                AVObject post = new AVObject("Score");
                post.put("picID", picID);
                post.put("score", score * 20);
                post.put("commenterID", AVUser.getCurrentUser().getObjectId());
                post.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(AVException e) {
                        if (e == null) {
                            AVQuery<AVObject> query = new AVQuery<AVObject>("ShareFace");
                            query.whereEqualTo("objectId", picID);
                            query.findInBackground(new FindCallback<AVObject>() {
                                @Override
                                public void done(List<AVObject> list, AVException e) {
                                    if (e == null) {
                                        list.get(0).increment("scoreNum");
                                        list.get(0).saveInBackground(new SaveCallback() {
                                            @Override
                                            public void done(AVException e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(ShowDetail.this, "评分成功了哦~", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(ShowDetail.this, "出错了~", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        builder.setNegativeButton("打错了，重新打分", null);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        return dialog;
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
        public View getView(final int position, View view, ViewGroup arg2) {

            view = LayoutInflater.from(ShowDetail.this).inflate(R.layout.comment_item, null);
            ImageView picComment = (ImageView) view.findViewById(R.id.picComment);
            TextView honeyName = (TextView) view.findViewById(R.id.honeyNameComment);
            TextView time = (TextView) view.findViewById(R.id.timeComment);
            TextView commentText = (TextView) view.findViewById(R.id.commentText);
            final TextView praiseNum = (TextView) view.findViewById(R.id.praiseNum);
            picComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    list.get(position).increment("likeNum");
                    list.get(position).saveInBackground();
                    Toast.makeText(ShowDetail.this, "点赞成功了~", Toast.LENGTH_SHORT).show();
                    praiseNum.setText("(" + list.get(0).getInt("likeNum") + ")");
                    notifyDataSetChanged();
                }
            });
            if (list.size() == 0) {
                newComment.setVisibility(View.GONE);
            } else {
                honeyName.setText(list.get(position).getString("commentHoneyName"));
                Date date = new Date(list.get(position).getCreatedAt().getTime());
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String time_param = format.format(date);
                time.setText(time_param);
                commentText.setText(list.get(position).getString("content"));
                praiseNum.setText("(" + list.get(position).getInt("likeNum") + ")");
            }

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
