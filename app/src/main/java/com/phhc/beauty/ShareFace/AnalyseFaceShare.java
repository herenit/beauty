package com.phhc.beauty.ShareFace;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.phhc.beauty.JNILib;
import com.phhc.beauty.R;
import com.phhc.beauty.RealtimeFace.AnalyseFace;
import com.phhc.beauty.utils.LooperExecutor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.iwf.photopicker.PhotoPickerActivity;
import me.iwf.photopicker.utils.PhotoPickerIntent;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;


public class AnalyseFaceShare extends Activity implements View.OnClickListener {

    private LooperExecutor executor;
    private Handler mHandler = new Handler();
    private Bitmap mFaceBitmap;
    private JNILib mJniLib = new JNILib();
    private int mRet;
    private Handler handlerOut;
    private ImageView photo;
    private Object bWait = new Object();
    private TextView tips;
    private Timer timer;
    private static final int CAMERA_REQUEST = 1888;
    private RelativeLayout reCamera;
    private static final int TAKE_PICTURE = 1;
    private Uri imageUri;
    private TextView back, bottom, changeSecond;
    private MaterialProgressBar progressBar;
    private RelativeLayout faceAnalyse, camera, album;
    private Intent intent;
    private Context mContext;
    List<String> photos = null;
    private Uri uri;
    ArrayList<String> selectedPhotos = new ArrayList<>();
    private static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyse_face_share);

        mContext = this;
        intent = getIntent();
        photos = intent.getStringArrayListExtra(PhotoPickerActivity.KEY_SELECTED_PHOTOS);
        selectedPhotos.clear();
        if (photos != null) {
            selectedPhotos.addAll(photos);
        }
        uri = Uri.fromFile(new File(selectedPhotos.get(0)));
        photo = (ImageView) findViewById(R.id.photo);
        photo.setImageURI(uri);
        executor = new LooperExecutor();
        executor.requestStart();
        back = (TextView) findViewById(R.id.back);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (bWait) {
                    mRet = mJniLib.Calculate(uri.getPath());
                    if (mRet == -5) //#define ERR_SDKNOINIT -5
                    {
                        mJniLib.InitFaceBeauty(mContext);
                        mRet = mJniLib.Calculate(uri.getPath());
                    }
                }

            }
        });
        back.setOnClickListener(this);
        progressBar = (MaterialProgressBar) findViewById(R.id.progress);
        tips = (TextView) findViewById(R.id.tips);
        bottom = (TextView) findViewById(R.id.bottom);
        faceAnalyse = (RelativeLayout) findViewById(R.id.faceAnalyse);
        camera = (RelativeLayout) findViewById(R.id.camera);
        album = (RelativeLayout) findViewById(R.id.album);
        faceAnalyse.setOnClickListener(this);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == JNILib.FACE_DETECTED) {
                    handler.sendEmptyMessage(0);
                    timer = new Timer();
                    TimerTask timerTask;
                    timerTask = new TimerTask() {
                        @Override
                        public void run() {
//                            handler.sendEmptyMessage(2);
                        }
                    };
                    timer.schedule(timerTask, 0, 1000);
                } else if (msg.what == JNILib.FACE_NOTDETECTED) {
                    handler.sendEmptyMessage(1);
                }
                //"0"代表检测到人脸
                else if (msg.what == 0) {
                    progressBar.setVisibility(View.GONE);
                    bottom.setVisibility(View.GONE);
                    camera.setVisibility(View.VISIBLE);
                    faceAnalyse.setVisibility(View.VISIBLE);
                    album.setVisibility(View.VISIBLE);
                }
                //“1”代表没有检测到人脸
                else if (msg.what == 1) {
                    progressBar.setVisibility(View.INVISIBLE);
                    bottom.setVisibility(View.GONE);
                    tips.setVisibility(View.VISIBLE);
                    camera.setVisibility(View.VISIBLE);
                    album.setVisibility(View.VISIBLE);
                    photo.setImageResource(R.mipmap.white);
                }
                //"2"代表开始颜值分析
                else if (msg.what == 2) {
//                    reCamera.setVisibility(View.GONE);
//                    progressBar.setVisibility(View.VISIBLE);
//                    bottom.setText("颜值分析中");
//                    bottom.setVisibility(View.VISIBLE);
//                    timer.cancel();
                    executor.execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (bWait) {
                                        mHandler.post(new Runnable() {  //update UI
                                            @Override
                                            public void run() {
                                                if (0 == mRet) {
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    bottom.setVisibility(View.GONE);
                                                    Intent intent = new Intent();
                                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                    Bitmap bitmap = ((BitmapDrawable) photo.getDrawable()).getBitmap();
                                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                                                    byte[] b = baos.toByteArray();
                                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                                                    // 当前手机时间
                                                    SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm");
                                                    long timeShow = System.currentTimeMillis();
                                                    String fileName = timeShow + ".png";
                                                    try {
                                                        FileOutputStream fileOutStream = openFileOutput(fileName, MODE_PRIVATE);
                                                        fileOutStream.write(b);  //b is byte array
                                                        //(used if you have your picture downloaded
                                                        // from the *Web* or got it from the *devices camera*)
                                                        //otherwise this technique is useless
                                                        fileOutStream.close();
                                                    } catch (IOException ioe) {
                                                        ioe.printStackTrace();
                                                    }
//                                                    intent.putExtra("bitmap", b);
                                                    intent.putExtra("picname", fileName);
                                                    intent.putExtra("Total", mJniLib.GetTotalScore());
                                                    intent.putExtra("FlawLabel", mJniLib.GetFlawLabel());
                                                    intent.putExtra("ExpressionLabel", mJniLib.GetExpressionLabel());
                                                    intent.putExtra("Age", mJniLib.GetAge());
                                                    intent.putExtra("Skin", mJniLib.GetSkin());
                                                    intent.setAction("android.intent.action.ShowResultShare");
                                                    startActivity(intent);
                                                    finish();
                                                } else {
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    bottom.setVisibility(View.GONE);
                                                    reCamera.setVisibility(View.VISIBLE);
                                                    tips.setVisibility(View.VISIBLE);
                                                    photo.setImageResource(R.mipmap.white);
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                    );
                }
            }
        };
        mJniLib.setHandler(handler);
        mJniLib.InitFaceBeauty(this);
    }

    public final static int REQUEST_CODE = 1;

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                PhotoPickerIntent intent = new PhotoPickerIntent(AnalyseFaceShare.this);
                intent.setPhotoCount(1);
                intent.setShowCamera(true);
                startActivityForResult(intent, REQUEST_CODE);
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
                finish();
                break;
            case R.id.faceAnalyse:
                camera.setVisibility(View.GONE);
                faceAnalyse.setVisibility(View.GONE);
                album.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                bottom.setText("颜值分析中");
                bottom.setVisibility(View.VISIBLE);
                handler.sendEmptyMessage(2);
                break;
        }
    }

    public void takePhoto() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "Pic.jpg");
        imageUri = Uri.fromFile(photo);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

}
