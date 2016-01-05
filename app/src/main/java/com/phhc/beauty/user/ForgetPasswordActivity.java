package com.phhc.beauty.user;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.RequestPasswordResetCallback;
import com.phhc.beauty.R;


/**
 * Created by nijian on 2015/6/1.
 */
public class ForgetPasswordActivity extends Activity implements View.OnClickListener {

    private ImageButton back;
    private Intent intent;
    private TextView complete;
    private EditText mailbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forget_password);
        back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(this);
        complete = (TextView) findViewById(R.id.complete);
        complete.setOnClickListener(this);
        mailbox = (EditText) findViewById(R.id.mailbox);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                intent = new Intent();
                intent.setAction("android.intent.action.Login");
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
                break;
            case R.id.complete:
                AVUser.requestPasswordResetInBackground(mailbox.getText().toString(), new RequestPasswordResetCallback() {
                    public void done(AVException e) {
                        if (e == null) {
                            intent = new Intent();
                            intent.setAction("android.intent.action.Initial");
                            startActivity(intent);
                            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
                            // 已发送一份重置密码的指令到用户的邮箱
                            Toast toast = Toast.makeText(ForgetPasswordActivity.this, "重置密码邮件已经发送到你的注册邮箱了哦~", Toast.LENGTH_SHORT);
                            toast.show();
                        } else {
                            // 隐藏输入法
                            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(ForgetPasswordActivity.this.INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(
                                    mailbox.getWindowToken(), 0);
                            // 重置密码出错。
                            Toast toast = Toast.makeText(ForgetPasswordActivity.this, "哦，糟糕，重置密码出错了哦~", Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });
                break;
        }
    }
}
