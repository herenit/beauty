package com.phhc.beauty.RealtimeFace;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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
import com.phhc.beauty.R;
import com.phhc.beauty.main.MainActivity;

import java.io.File;


public class ShowResult extends Activity implements View.OnClickListener {

    private RelativeLayout uploadServer;
    private Intent intent;
    private TextView back, score, face, age, skin, flaw;
    private ImageView pic;
    private Drawable d;
    private Bitmap bitmap;
    byte[] bytes;
    private float Age, Skin, Total;
    private int ExpressionLabel, FlawLabel;
    private Dialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_result);

        skin = (TextView) findViewById(R.id.skin);
        flaw = (TextView) findViewById(R.id.flaw);
        face = (TextView) findViewById(R.id.face);
        score = (TextView) findViewById(R.id.score);
        age = (TextView) findViewById(R.id.age);
        uploadServer = (RelativeLayout) findViewById(R.id.uploadServer);
        uploadServer.setOnClickListener(this);
        Bundle extras = getIntent().getExtras();
        String fileName = extras.getString("picname");
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
        age.setText(Age + "");
        File filePath = getFileStreamPath(fileName);
        d = Drawable.createFromPath(filePath.toString());
        bytes = (byte[]) extras.getByteArray("bitmap");
        pic = (ImageView) findViewById(R.id.pic);
        pic.setBackgroundDrawable(d);
        back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.uploadServer:
                progressDialog = ProgressDialog.show(this, "", "正在发布到秀场，请稍后...", true);
                progressDialog.setCancelable(false);
                AVObject post = new AVObject("ShareFace");
                final AVFile file = new AVFile("photo", bytes);
                post.put("pic", file);
                post.put("honeyName", AVUser.getCurrentUser().getUsername());
                post.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(AVException e) {
                        if (e == null) {
                            progressDialog.dismiss();
                            intent = new Intent(ShowResult.this, MainActivity.class);
                            startActivity(intent);
                            Toast toast = Toast.makeText(ShowResult.this, "发布成功!", Toast.LENGTH_LONG);
                            toast.show();
                        } else {
                            // 保存失败，输出错误信息
                            Toast toast = Toast.makeText(ShowResult.this, "发布失败！", Toast.LENGTH_LONG);
                            toast.show();
                            Log.e("aaa", e + "");
                        }
                    }
                });
                break;
        }
    }

}
