package com.phhc.beauty.user;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.SaveCallback;
import com.phhc.beauty.R;
import com.phhc.beauty.main.MainActivity;
import com.phhc.beauty.utils.RoundedImageView;
import com.phhc.beauty.utils.StatusNetAsyncTask;
import com.phhc.beauty.utils.StatusUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.ByteArrayOutputStream;


/**
 * Created by nijian on 2015/5/30.
 */
public class PersonalInfoActivity extends Activity implements View.OnClickListener {

    private ImageView back;
    private RelativeLayout portrait;
    private RoundedImageView photo;
    private TextView honeyName, cellphone;
    private Activity activity = PersonalInfoActivity.this;
    private String picturePath;
    private Intent dataIntent, intent;
    private RelativeLayout exit, modifyPassword;
    private Dialog progressDialog;
    //表示选择图片
    private static final int IMAGE_PICK_REQUEST = 0;
    //表示裁剪图片
    private static final int CROP_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.personal_info);
        back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(this);
        portrait = (RelativeLayout) findViewById(R.id.portrait);
        portrait.setOnClickListener(this);
        photo = (RoundedImageView) findViewById(R.id.photo);
        exit = (RelativeLayout) findViewById(R.id.exit);
        exit.setOnClickListener(this);
        honeyName = (TextView) findViewById(R.id.honeyName);
        honeyName.setText(AVUser.getCurrentUser().getUsername());
        modifyPassword = (RelativeLayout) findViewById(R.id.modifyPassword);
        modifyPassword.setOnClickListener(this);
        cellphone = (TextView) findViewById(R.id.cellphone);
        cellphone.setText(AVUser.getCurrentUser().getMobilePhoneNumber());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                //新的activity进入动画，旧的activity退出的动画
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
                break;
            case R.id.portrait:
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(i, IMAGE_PICK_REQUEST);
                break;
            case R.id.exit:
                finish();
                AVUser.logOut();
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                //新的activity进入动画，旧的activity退出的动画
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
                break;
            case R.id.modifyPassword:
                intent = new Intent();
                intent.setAction("android.intent.action.modifyPasswordActivity");
                startActivity(intent);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_REQUEST) {
                dataIntent = data;
                Uri uri = data.getData();
                StatusUtils.startAvatarCrop(activity, uri, 200, 200, CROP_REQUEST, getCachePath());
            } else if (requestCode == CROP_REQUEST) {
                final Bitmap bitmap = data.getExtras().getParcelable("data");
                progressDialog = ProgressDialog.show(this, "", "头像上传中，请稍后...", true);
                progressDialog.setCancelable(false);
                new StatusNetAsyncTask(this) {
                    @Override
                    protected void doInBack() throws Exception {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                        byte[] bytes = stream.toByteArray();
                        final AVFile file = new AVFile("photo", bytes);
                        AVUser currentUser = AVUser.getCurrentUser();
                        currentUser.put("portrait", file);
                        currentUser.saveInBackground(new SaveCallback() {
                            public void done(AVException e) {
                                if (e == null) {
                                    progressDialog.dismiss();
                                    Toast toast = Toast.makeText(PersonalInfoActivity.this, "上传成功", Toast.LENGTH_LONG);
                                    toast.show();
                                } else {
                                    // 保存失败，输出错误信息
                                    Toast toast = Toast.makeText(PersonalInfoActivity.this, "上传失败", Toast.LENGTH_LONG);
                                    toast.show();
                                    Log.e("aaa", e + "");
                                }
                            }
                        });
                    }

                    @Override
                    protected void onPost(Exception e) {
                        if (StatusUtils.filterException(activity, e)) {
                            Uri selectedImage = dataIntent.getData();
                            String[] filePathColumn = {MediaStore.Images.Media.DATA};
                            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                            cursor.moveToFirst();
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            picturePath = cursor.getString(columnIndex);
                            cursor.close();
                            photo.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                        }
                    }
                }.execute();
            }
        }
    }

    public String getCachePath() {
        return getCacheDir() + "tmp";
    }

    @Override
    protected void onResume() {
        super.onResume();
        AVUser currentUser = AVUser.getCurrentUser();
        if (currentUser.getAVFile("portrait") != null) {
            ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
            ImageLoader.getInstance().displayImage(currentUser.getAVFile("portrait").getUrl(), photo, StatusUtils.normalImageOptions);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        finish();
        intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        return super.onKeyDown(keyCode, event);
    }

}
