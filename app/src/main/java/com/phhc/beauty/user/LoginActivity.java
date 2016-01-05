package com.phhc.beauty.user;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.phhc.beauty.R;
import com.phhc.beauty.main.MainActivity;
import com.phhc.beauty.utils.Constants;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;


/**
 * Created by nijian on 2015/6/1.
 */
public class LoginActivity extends Activity implements View.OnClickListener {

    private IWXAPI api;
    private TextView forget_password;
    private ImageButton back;
    private Intent intent;
    private RelativeLayout complete;
    private EditText username, password;
    private ProgressDialog progressDialog;
    private TextView register;
    private ImageView weixin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(this);
        forget_password = (TextView) findViewById(R.id.forget_password);
        forget_password.setOnClickListener(this);
        complete = (RelativeLayout) findViewById(R.id.complete);
        complete.setOnClickListener(this);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        register = (TextView)findViewById(R.id.register);
        register.setOnClickListener(this);
        weixin = (ImageView)findViewById(R.id.weixin);
        weixin.setOnClickListener(this);
        api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, true);
        api.registerApp(Constants.APP_ID);
//        api = WXAPIFactory.createWXAPI(this, Constants.APP_ID);
        // 隐藏输入法
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(LoginActivity.this.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(username.getWindowToken(), 0);
        inputMethodManager.hideSoftInputFromWindow(password.getWindowToken(), 0);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.complete:
                progressDialog = ProgressDialog.show(this, "", "数据加载中，请稍后...", true);
                AVUser.logInInBackground(username.getText().toString(), password.getText().toString(), new LogInCallback<AVUser>() {
                    public void done(AVUser user, AVException e) {
                        if (user != null) {
                            progressDialog.dismiss();
                            finish();
                            intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                        } else {
                            progressDialog.dismiss();
                            Toast toast = Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                });
                break;
            case R.id.back:
                finish();
                break;
            case R.id.forget_password:
                intent = new Intent();
                intent.setAction("android.intent.action.ForgetPassword");
                startActivity(intent);
                break;
            case R.id.register:
                intent = new Intent();
                intent.setAction("android.intent.action.Register");
                startActivity(intent);
                break;
            case R.id.weixin:
                final SendAuth.Req req = new SendAuth.Req();
                req.scope = "snsapi_userinfo";
                req.state = "com_phhc_beauty";
                api.sendReq(req);
                break;
        }
    }
}
