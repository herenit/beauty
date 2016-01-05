package com.phhc.beauty.ShareFace;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.phhc.beauty.R;


public class PhotoDetail extends Activity implements View.OnClickListener {

    private TextView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_detail);

        back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
        }
    }

    @Override
    public void onResume() {
        // AVObject Auto-generated method stub
        super.onResume();
    }

}
