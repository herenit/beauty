package com.phhc.beauty.ShareFace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.phhc.beauty.R;


public class ImageGridActivity extends Activity implements View.OnClickListener {
    public static final String EXTRA_IMAGE_LIST = "imagelist";

    private Spinner spinner;
    List<ImageItem> dataList;
    List<ImageBucket> dataListBucket;
    GridView gridView;
    ImageGridAdapter adapter;
    AlbumHelper helper;
    Button bt;
    private TextView back;
    List<String> list;
    private RelativeLayout selectCategory;
    private ArrayAdapter<String> adapterSpinner;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Toast.makeText(ImageGridActivity.this, "最多选择1张图片", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_grid);

        spinner = (Spinner) findViewById(R.id.spinner);
        list = new ArrayList<>();

        helper = AlbumHelper.getHelper();
        helper.init(getApplicationContext());
        selectCategory = (RelativeLayout) findViewById(R.id.selectCategory);
        selectCategory.setOnClickListener(this);
        helper = AlbumHelper.getHelper();
        helper.init(getApplicationContext());
        back = (TextView) findViewById(R.id.back);
        back.setOnClickListener(this);
        dataListBucket = helper.getImagesBucketList(false);
        for (int i = 0; i < dataListBucket.size(); i++) {
            if (dataListBucket.get(i).bucketName.equals("Camera")) {
                dataList = dataListBucket.get(i).imageList;
            }
            list.add(dataListBucket.get(i).bucketName);
        }
        adapterSpinner = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterSpinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                for (int j = 0; j < dataListBucket.size(); j++) {
                    if (dataListBucket.get(j).bucketName.equals(((TextView) view).getText().toString())) {
                        dataList = dataListBucket.get(i).imageList;
                        initView();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        initView();
        bt = (Button) findViewById(R.id.bt);
        bt.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                ArrayList<String> list = new ArrayList<String>();
                Collection<String> c = adapter.map.values();
                Iterator<String> it = c.iterator();
                for (; it.hasNext(); ) {
                    list.add(it.next());
                }

                if (Bimp.act_bool) {
//                    Intent intent = new Intent(ImageGridActivity.this,
//                            PublishedActivity.class);
//                    startActivity(intent);
//                    Bimp.act_bool = false;
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.AnalyseFaceShare");
                    startActivity(intent);
                    Bimp.act_bool = false;
                }
                for (int i = 0; i < list.size(); i++) {
                    if (Bimp.drr.size() < 9) {
                        Bimp.drr.add(list.get(i));
                    }
                }
//                finish();
            }

        });
    }

    private void initView() {
        gridView = (GridView) findViewById(R.id.gridview);
        gridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        adapter = new ImageGridAdapter(ImageGridActivity.this, dataList,
                mHandler);
        gridView.setAdapter(adapter);
        adapter.setTextCallback(new ImageGridAdapter.TextCallback() {
            public void onListen(int count) {
                bt.setText("完成" + "(" + count + ")");
            }
        });

        gridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                adapter.notifyDataSetChanged();
            }

        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.selectCategory:
                Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
