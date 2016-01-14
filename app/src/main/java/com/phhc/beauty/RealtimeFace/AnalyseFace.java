package com.phhc.beauty.RealtimeFace;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
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


import com.phhc.beauty.R;
import com.phhc.beauty.JNILib;
import com.phhc.beauty.utils.LooperExecutor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;


public class AnalyseFace extends Activity implements View.OnClickListener {

    private LooperExecutor executor;
    private Handler mHandler = new Handler();
    private Bitmap mFaceBitmap;
    private JNILib mJniLib = new JNILib();
    private int mRet;
    private static final int CAMERA_REQUEST = 1888;
    int i = 5;
    private RelativeLayout reCamera;
    private TextView back, bottom, changeSecond;
    private MaterialProgressBar progressBar;
    private Handler handler;
    private ImageView photo;
    private TextView tips;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.analyse_face);

        executor = new LooperExecutor();
        executor.requestStart();
        back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(this);
        progressBar = (MaterialProgressBar) findViewById(R.id.progress);
        bottom = (TextView) findViewById(R.id.bottom);
        reCamera = (RelativeLayout) findViewById(R.id.reCamera);
        reCamera.setOnClickListener(this);
        changeSecond = (TextView) findViewById(R.id.changeSecond);
        photo = (ImageView) findViewById(R.id.photo);
        tips = (TextView) findViewById(R.id.tips);
        takePhoto();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //"0"代表检测到人脸
                if (msg.what == 0) {
                    progressBar.setVisibility(View.INVISIBLE);
                    bottom.setVisibility(View.GONE);
                    tips.setVisibility(View.GONE);
                    reCamera.setVisibility(View.VISIBLE);
                    changeSecond.setVisibility(View.VISIBLE);
                }
                //“1”代表没有检测到人脸
                if (msg.what == 1) {
                    progressBar.setVisibility(View.INVISIBLE);
                    bottom.setVisibility(View.GONE);
                    reCamera.setVisibility(View.VISIBLE);
                    tips.setVisibility(View.VISIBLE);
                    photo.setImageResource(R.mipmap.white);
                }
                //"2"代表
                if (msg.what == 2) {
                    changeSecond.setText(i-- + "秒后开始颜值分析");
                    if (i >= 0) {

                    } else {
                        changeSecond.setVisibility(View.GONE);
                        reCamera.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        bottom.setText("颜值分析中");
                        bottom.setVisibility(View.VISIBLE);
                        timer.cancel();
                        executor.execute(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        mRet = mJniLib.Calculate(Environment.getExternalStorageDirectory() + "/beautytestScale.jpg");
                                        mHandler.post(new Runnable() {  //update UI
                                            @Override
                                            public void run() {
                                                if (0 == mRet) {
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    bottom.setVisibility(View.GONE);
                                                    Intent intent = new Intent();
                                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                    mFaceBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                                                    mFaceBitmap.recycle();
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
                                                    intent.setAction("android.intent.action.ShowResult");
                                                    startActivity(intent);
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
                        );
                    }
                }
            }
        };
        mJniLib.InitFaceBeauty(this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
//        savedInstanceState.putString("message", text.getText().toString());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
//        message = savedInstanceState.getString("message");
    }

    @Override
    protected void onDestroy() {
        executor.requestStop();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.reCamera:
                i = 5;
                timer.cancel();
                takePhoto();
                break;
        }
    }

    public void takePhoto() {
        mFaceBitmap = null;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //参考   http://blog.csdn.net/csqingchen/article/details/45502813
        if (intent.resolveActivity(getPackageManager()) != null) {
            Uri u = Uri.fromFile(new File(getPhotopath()));
            //指定了生成文件的名称，则返回onActivityForResult时，data为null
            intent.putExtra(MediaStore.EXTRA_OUTPUT, u);
            startActivityForResult(intent, 1234);
        }
    }

    private Bitmap getBitmapFromUrl(String url, double width) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 设置了此属性一定要记得将值设置为false
        Bitmap bitmap = BitmapFactory.decodeFile(url);
        // 防止OOM发生
        options.inJustDecodeBounds = false;
        int mWidth = bitmap.getWidth();
        int mHeight = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = 1;
        float scaleHeight = 1;
        int ori = -1;
        try {
            ExifInterface exif = new ExifInterface(url);
            ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ori != -1) {
            switch (ori) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }
        }
        // 按照固定宽高进行缩放
        // 这里希望知道照片是横屏拍摄还是竖屏拍摄
        // 因为两种方式宽高不同，缩放效果就会不同
        // 这里用了比较笨的方式
        if (mWidth <= mHeight) {
            scaleWidth = (float) (width / mWidth);
            scaleHeight = scaleWidth;
        } else {
            scaleHeight = (float) (width / mHeight);
            scaleWidth = scaleHeight;
            if (ori == -1)
                matrix.postRotate(90);
        }
//        matrix.postRotate(90); /* 翻转90度 */
        // 按照固定大小对图片进行缩放
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, mWidth, mHeight, matrix, true);
        // 用完了记得回收
        bitmap.recycle();
        return newBitmap;
    }

    /**
     * 存储缩放的图片
     *
     * @param
     */
    private void saveScalePhoto(Bitmap bitmap) {
        // 照片全路径
        String fileName = "";
        // 文件夹路径
        String pathUrl = Environment.getExternalStorageDirectory().getPath() + "/";
        String imageName = "beautytestScale.jpg";
        FileOutputStream fos = null;
        File file = new File(pathUrl);
        file.mkdirs();// 创建文件夹
        fileName = pathUrl + imageName;
        try {
            fos = new FileOutputStream(fileName);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 98, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getPhotopath() {
        return Environment.getExternalStorageDirectory() + "/beautytest.jpg";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234 && resultCode == RESULT_OK) {
            if (null != data)
                mFaceBitmap = data.getParcelableExtra("data");
            else {
                mFaceBitmap = getBitmapFromUrl(getPhotopath(), 480);
            }

            if (mFaceBitmap != null) {
                // 显示图片
                photo.setScaleType(ImageView.ScaleType.CENTER);
                photo.setImageBitmap(mFaceBitmap);
                // 检测是否是人脸,耗时操作不要放在UI线程！！！
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        saveScalePhoto(mFaceBitmap);
                        Bitmap tmpBmp = mFaceBitmap.copy(Bitmap.Config.RGB_565, true);
                        FaceDetector faceDet = new FaceDetector(tmpBmp.getWidth(), tmpBmp.getHeight(), 1); //only 1 face //虽然速度快， 但是是检测的眼睛之间的距离，不符合我们的需求
                        FaceDetector.Face[] faceList = new FaceDetector.Face[1];
                        faceDet.findFaces(tmpBmp, faceList);
                        FaceDetector.Face face = faceList[0];
                        if (face != null) {
                            handler.sendEmptyMessage(0);
                            timer = new Timer();
                            TimerTask timerTask;
                            timerTask = new TimerTask() {
                                @Override
                                public void run() {
                                    handler.sendEmptyMessage(2);
                                }
                            };
                            timer.schedule(timerTask, 0, 1000);
                        } else {
                            handler.sendEmptyMessage(1);
                        }
                        tmpBmp.recycle();
//                        mFaceBitmap.recycle();
                    }
                });
            }
        } else {
            finish();
        }
    }

}
