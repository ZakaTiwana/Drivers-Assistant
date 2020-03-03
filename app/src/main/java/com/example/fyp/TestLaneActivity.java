package com.example.fyp;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customview.OverlayView;

import org.opencv.android.OpenCVLoader;

public class TestLaneActivity extends AppCompatActivity {
    private static final String TAG = "TestLaneActivity";

    static {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "static initializer: OpenCV Loaded Sucessfully");
        }else{
            Log.e(TAG, "static initializer: OpenCv could not be loaded");
        }
    }

    private static final Size DESIRED_SIZE = new Size(1280,720);

    private OverlayView draw = null;
//    private Button btn_mask;

    private Bitmap image = null;
    private Bitmap copyBitmap = null;
    private Bitmap crropedBitmap = null;
    private Matrix frameToCrop = null;
    private LaneDetector ld = null;

    private int progress = 0;

//    private int threshold,minLineLength,maxLineGap;
//



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_lane);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        draw = findViewById(R.id.lane_test_overlay);
//        Intent i = getIntent();
       image = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.test_img_lane);
       copyBitmap = image.copy(Bitmap.Config.ARGB_8888,true);


    // resize
       frameToCrop = ImageUtilities.getTransformationMatrix(image.getWidth(),image.getHeight(),
                DESIRED_SIZE.getWidth(), DESIRED_SIZE.getHeight(),0,false);

       crropedBitmap = Bitmap.createBitmap(DESIRED_SIZE.getWidth(),DESIRED_SIZE.getHeight(), Bitmap.Config.ARGB_8888);
       Canvas canvas = new Canvas(crropedBitmap);
       canvas.drawBitmap(copyBitmap,frameToCrop,null);

       ld = new LaneDetector(crropedBitmap);

       draw.addCallback(new OverlayView.DrawCallback() {
           @Override
           public void drawCallback(Canvas canvas) {
               switch (progress){
                   case 0:
                       canvas.drawBitmap(crropedBitmap,0,0,null);
                       break;
                   case 1:
                       canvas.drawBitmap(ld.getGray(),0,0,null);
                       break;
                   case 2:
                       canvas.drawBitmap(ld.getEdge(),0,0,null);
                       break;
                   case 3:
                       canvas.drawBitmap(ld.getMask(),0,0,null);
                       break;
                   case 4:
                       canvas.drawBitmap(ld.getRoi(),0,0,null);
                       break;
                   case 5:
                       canvas.drawBitmap(ld.getResult(null),0,0,null);
//                       ld.getResult2();
                       break;
               }
           }
       });

       draw.postInvalidate();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            progress++;
            progress %= 6;
            draw.postInvalidate();
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

            if (progress == 0) return true;
            progress--;
            Math.abs(progress);
            progress %= 6;
            draw.postInvalidate();
            return true;
        }

        return super.onKeyDown(keyCode,event);
    }
}
