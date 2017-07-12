package com.example.ggu.demo_v1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.GridView;

import org.opencv.core.Mat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Created by GGu on 7/3/2017.
 */

public class view_gallery extends AppCompatActivity {
    GridView grid;
    GridViewAdapter gridAdapter;
    ArrayList<ImageItem> imageItems = new ArrayList<>();
    ArrayList<Mat> stuffing = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState) {
        Log.d("myTag", "Part 8: Loading fragments");

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.view_stuff);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            ArrayList<String> paths = (ArrayList<String>) extras.getSerializable("objects");
            for(int i = 0; i < paths.size(); i++) {
                Bitmap temp = null;
                FileInputStream herro = null;

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                try {
                    herro = new FileInputStream(paths.get(i));
                    temp = BitmapFactory.decodeStream(herro, null, options);
                    imageItems.add(new ImageItem(temp));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        grid = (GridView) findViewById(R.id.gallery);
        gridAdapter = new GridViewAdapter(this, R.layout.galleryitem_layout, imageItems);
        grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();

        Log.d("myTag", "Part 8: Done");
    }
}
