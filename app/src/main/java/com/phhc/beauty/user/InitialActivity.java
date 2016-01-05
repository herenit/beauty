package com.phhc.beauty.user;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.phhc.beauty.R;


/**
 * Created by nijian on 2015/6/2.
 */
public class InitialActivity extends Activity implements View.OnClickListener {

    private RelativeLayout login, register;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial);
        login = (RelativeLayout) findViewById(R.id.login);
        login.setOnClickListener(this);
        register = (RelativeLayout) findViewById(R.id.register);
        register.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.login:
                intent = new Intent();
                intent.setAction("android.intent.action.Login");
                startActivity(intent);
                break;
            case R.id.register:
                intent = new Intent();
                intent.setAction("android.intent.action.Register");
                startActivity(intent);
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        finish();
        return super.onKeyDown(keyCode, event);
    }
}
