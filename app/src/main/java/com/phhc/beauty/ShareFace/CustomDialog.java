package com.phhc.beauty.ShareFace;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.RelativeLayout;

import com.phhc.beauty.R;
import com.phhc.beauty.interfaces.LoadDataListener;


public class CustomDialog extends Dialog implements
        View.OnClickListener {

    private RelativeLayout faceMap;
    int i = 15;
    public ShowResultShare c;
    String param;
    private LoadDataListener loadDataListener;

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public CustomDialog(ShowResultShare a, String param) {
        super(a);
        // TODO Auto-generated constructor stub
        this.c = a;
        this.param = param;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog);

        faceMap = (RelativeLayout) findViewById(R.id.faceMap);
        faceMap.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.faceMap:
                c.upload();
                break;
            default:
                break;
        }
    }

}