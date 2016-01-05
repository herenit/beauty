package com.phhc.beauty.user;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.UpdatePasswordCallback;
import com.phhc.beauty.R;


public class ModifyPasswordActivity extends Activity implements View.OnClickListener {

    ProgressDialog progressDialog;
    String newPassword, oldPassword;
    EditText old_password, new_password;
    ImageButton back;
    TextView complete;
    private Intent intent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.modify_password);
        back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(this);
        complete = (TextView) findViewById(R.id.complete);
        complete.setOnClickListener(this);
        old_password = (EditText) findViewById(R.id.old_password);
        new_password = (EditText) findViewById(R.id.new_password);
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    @Override
    public void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                intent = new Intent();
                intent.setAction("android.intent.action.personalInfoActivity");
                startActivity(intent);
                break;
            case R.id.complete:
                oldPassword = old_password.getText().toString();
                newPassword = new_password.getText().toString();
                AVUser userA = AVUser.getCurrentUser();//请确保用户当前的有效登录状态
                userA.updatePasswordInBackground(oldPassword, newPassword, new UpdatePasswordCallback() {

                    @Override
                    public void done(AVException e) {
                        if (e == null) {
                            Toast toast = Toast.makeText(ModifyPasswordActivity.this, "密码修改成功了哦~", Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            Toast toast = Toast.makeText(ModifyPasswordActivity.this, "密码没有修改成功，请重试~", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });
                break;
        }
    }

}
