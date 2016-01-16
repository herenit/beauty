package com.phhc.beauty.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.avos.avoscloud.AVUser;
import com.baidu.mapapi.radar.RadarSearchManager;
import com.phhc.beauty.JNILib;
import com.phhc.beauty.R;


import java.util.logging.LogRecord;

public class MainActivity extends Activity implements View.OnClickListener {

    private RelativeLayout realTimeBeauty, share, beautyHome;
    private Intent intent;
    private AVUser currentUser;
    private static boolean isExit = false;
    private JNILib mJNILib=new JNILib();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beautyHome = (RelativeLayout) findViewById(R.id.beautyHome);
        beautyHome.setOnClickListener(this);
        realTimeBeauty = (RelativeLayout) findViewById(R.id.realTimeBeauty);
        realTimeBeauty.setOnClickListener(this);
        share = (RelativeLayout) findViewById(R.id.share);
        share.setOnClickListener(this);
        //判断是否有用户登录
        currentUser = AVUser.getCurrentUser();
        mJNILib.InitFaceBeauty(this);  //make sure init first
    }

    @Override
    protected void onResume() {
        //判断是否有用户登录
        currentUser = AVUser.getCurrentUser();
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.realTimeBeauty:
                if (currentUser != null) {
                    intent = new Intent();
                    intent.setAction("android.intent.action.AnalyseFace");
                    startActivity(intent);
                } else {
//                    finish();
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.Login");
                    startActivity(intent);
                }
                break;
            case R.id.share:
                if (currentUser != null) {
                    intent = new Intent();
                    intent.setAction("android.intent.action.ShareFace");
                    startActivity(intent);
                } else {
//                    finish();
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.Login");
                    startActivity(intent);
                }

                break;
            case R.id.beautyHome:
                if (currentUser != null) {
//                    finish();
                    intent = new Intent();
                    intent.setAction("android.intent.action.personalInfoActivity");
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                } else {
//                    finish();
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.Login");
                    startActivity(intent);
                }
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }
    };

    private void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            handler.sendEmptyMessageDelayed(0, 2000);
        } else {
            mJNILib.DeInitFaceBeauty();
            finish();
            System.exit(0);
        }
    }
}
