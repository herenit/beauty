package com.phhc.beauty.ShareFace;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;

import com.phhc.beauty.R;


public class FaceDescriptionDialog extends Dialog implements
        View.OnClickListener {

    private RelativeLayout seeDetail;
    int i = 15;
    public Activity c;
    String param;

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public FaceDescriptionDialog(Activity a, String param) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
        this.param = param;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.face_description_dialog);

        seeDetail = (RelativeLayout) findViewById(R.id.seeDetail);
        seeDetail.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.seeDetail:
                Intent intent = new Intent();
                intent.setAction("android.intent.action.PhotoDetail");
                c.startActivity(intent);
                break;
        }
    }

}