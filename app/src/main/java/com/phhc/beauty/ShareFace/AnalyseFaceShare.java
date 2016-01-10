package com.phhc.beauty.ShareFace;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.phhc.beauty.utils.LooperExecutor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;


public class AnalyseFaceShare extends Activity implements View.OnClickListener {

    private LooperExecutor executor;
    private Handler mHandler = new Handler();
    private Bitmap mFaceBitmap;
    private JNILib mJniLib = new JNILib();
    private int mRet;

    private Handler handlerOut;
    private ImageView photo;
    private TextView tips;
    private Timer timer;
    private static final int CAMERA_REQUEST = 1888;
    int i = 5;
    private RelativeLayout reCamera;
    private static final int TAKE_PICTURE = 1;
    private Uri imageUri;
    private TextView back, bottom, changeSecond;
    private MaterialProgressBar progressBar;
    private RelativeLayout faceAnalyse, camera, album;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyse_face_share);

        executor = new LooperExecutor();
        executor.requestStart();
        back = (TextView) findViewById(R.id.back);
        loading();
        back.setOnClickListener(this);
        progressBar = (MaterialProgressBar) findViewById(R.id.progress);
        tips = (TextView) findViewById(R.id.tips);
        bottom = (TextView) findViewById(R.id.bottom);
        photo = (ImageView) findViewById(R.id.photo);
        faceAnalyse = (RelativeLayout) findViewById(R.id.faceAnalyse);
        camera = (RelativeLayout) findViewById(R.id.camera);
        album = (RelativeLayout) findViewById(R.id.album);
        faceAnalyse.setOnClickListener(this);
        handlerOut = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //"0"代表检测到人脸
                if (msg.what == 0) {
                    progressBar.setVisibility(View.GONE);
                    bottom.setVisibility(View.GONE);
                    camera.setVisibility(View.VISIBLE);
                    faceAnalyse.setVisibility(View.VISIBLE);
                    album.setVisibility(View.VISIBLE);
                }
                //“1”代表没有检测到人脸
                if (msg.what == 1) {
                    progressBar.setVisibility(View.INVISIBLE);
                    bottom.setVisibility(View.GONE);
                    tips.setVisibility(View.VISIBLE);
                    camera.setVisibility(View.VISIBLE);
                    album.setVisibility(View.VISIBLE);
                    photo.setImageResource(R.mipmap.white);
                }
                //"2"代表
                if (msg.what == 2) {

                }
            }
        };
        mJniLib.InitFaceBeauty(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.faceAnalyse:
                camera.setVisibility(View.GONE);
                faceAnalyse.setVisibility(View.GONE);
                album.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                bottom.setText("颜值分析中");
                bottom.setVisibility(View.VISIBLE);
                executor.execute(
                        new Runnable() {
                            @Override
                            public void run() {
//                                mRet = mJniLib.Calculate(Environment.getExternalStorageDirectory() + "/beautytestScale.jpg");
                                mRet = mJniLib.Calculate(Bimp.drr.get(0));
                                mHandler.post(new Runnable() {  //update UI
                                    @Override
                                    public void run() {
                                        if (0 == mRet) {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            bottom.setVisibility(View.GONE);
                                            Intent intent = new Intent();
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            Bimp.bmp.get(0).compress(Bitmap.CompressFormat.JPEG, 100, baos);
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
                                            intent.putExtra("picname", fileName);
                                            intent.putExtra("Total", mJniLib.GetTotalScore());
                                            intent.putExtra("FlawLabel", mJniLib.GetFlawLabel());
                                            intent.putExtra("ExpressionLabel", mJniLib.GetExpressionLabel());
                                            intent.putExtra("Age", mJniLib.GetAge());
                                            intent.putExtra("Skin", mJniLib.GetSkin());
                                            intent.setAction("android.intent.action.ShowResultShare");
                                            startActivity(intent);
                                        } else {
                                            progressBar.setVisibility(View.INVISIBLE);
                                            bottom.setVisibility(View.GONE);
                                            tips.setVisibility(View.VISIBLE);
                                            camera.setVisibility(View.VISIBLE);
                                            album.setVisibility(View.VISIBLE);
                                            photo.setImageResource(R.mipmap.white);
                                        }
                                    }
                                });
                            }
                        }
                );
                break;
        }
    }

    public void takePhoto() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "Pic.jpg");
        imageUri = Uri.fromFile(photo);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {

        }
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (Bimp.bmp.size() == 0) {

                    } else {
                        photo.setImageBitmap(Bimp.bmp.get(0));
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                Bitmap tmpBmp = Bimp.bmp.get(0).copy(Bitmap.Config.RGB_565, true);
                                FaceDetector faceDet = new FaceDetector(tmpBmp.getWidth(), tmpBmp.getHeight(), 1); //only 1 face //虽然速度快， 但是是检测的眼睛之间的距离，不符合我们的需求
                                FaceDetector.Face[] faceList = new FaceDetector.Face[1];
                                faceDet.findFaces(tmpBmp, faceList);
                                FaceDetector.Face face = faceList[0];
                                if (face != null) {
                                    handlerOut.sendEmptyMessage(0);
                                } else {
                                    handlerOut.sendEmptyMessage(1);
                                }
                            }
                        });
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void loading() {
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    if (Bimp.max == Bimp.drr.size()) {
                        Message message = new Message();
                        message.what = 1;
                        handler.sendMessage(message);
                        break;
                    } else {
                        try {
                            String path = Bimp.drr.get(Bimp.max);
                            System.out.println(path);
                            Bitmap bm = Bimp.revitionImageSize(path);
                            Bimp.bmp.add(bm);
                            String newStr = path.substring(
                                    path.lastIndexOf("/") + 1,
                                    path.lastIndexOf("."));
                            FileUtils.saveBitmap(bm, "" + newStr);
                            Bimp.max += 1;
                            Message message = new Message();
                            message.what = 1;
                            handler.sendMessage(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

}
