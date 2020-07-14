package com.example.fyp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;

import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customutilities.SharedPreferencesUtils;
import com.example.fyp.customutilities.SharedValues;
import com.google.android.material.snackbar.Snackbar;
import com.example.fyp.customview.OverlayView;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;

public class ImageProcessor extends CameraCaptureActivity {

    private static final String TAG = "ImageProcessor";
    private static final Size[] DESIRED_PREVIEW_SIZES = SharedValues.DESIRED_PREVIEW_SIZES;
    private static final Size CROP_SIZE = SharedValues.CROP_SIZE;
    private static PointF[] pts_resized = null; // for lane

    private int mWidth = 0;
    private int mHeight = 0;

    private Bitmap rgbFrameBitmap = null;
    private Boolean isrgbFrameCreated = false;

    private static Detector detector = null;
    private static float timeTakeByObjDetector = 0;
    private static SignDetector signDetector = null;
    private static float timeTakeBySignDetector = 0;
    private static volatile boolean isComputingSignDetection = false;
    private static volatile boolean isComputingDetection = false;
    private static volatile boolean isComputingLaneDetection = false;

    private static float[][] lanePoints = null;
    private static ArrayList<PointF> lft_lane_pts = null;
    private static ArrayList<PointF> rht_lane_pts = null;
    private static float timeTakeByLaneDetector = 0;
    private Paint lanePointsPaint = null;

    private LaneDetector laneDetector = null;
    private  static  LaneDetectorAdvance laneDetectorAdvance = null;
    private static boolean laneGuidLines = false;
    private Path laneGuidPath = null;
    private Paint laneGuidPathPaint = null;

    private Snackbar initSnackbar = null;
    private volatile boolean initialized = false;

    private boolean drawDebugInfo = false;
    private int counterForVolumeDown = 0;

    private static OverlayView draw = null;

    private static List<RecognizedObject>  mappedRecognitions = null;
    private static List<RecognizedObject> mappedSignRecognitions = null;

    private Paint borderBoxPaint = null;
    private Paint borderTextPaint = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        draw = (OverlayView) findViewById(R.id.overlay);

        borderBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderBoxPaint.setColor(Color.RED);
        borderBoxPaint.setStrokeWidth(8);
        borderBoxPaint.setStyle(Paint.Style.STROKE);

        borderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderTextPaint.setColor(Color.BLUE);
        borderTextPaint.setTextSize(23);

        SharedPreferences sp_ld = getSharedPreferences(
                getString(R.string.sp_laneDetection),0);
        String sp_ld_key_tp = getString(R.string.sp_ld_key_transformed_mask_pts);
        String sp_ld_key_op = getString(R.string.sp_ld_key_original_mask_pts);
        pts_resized = (PointF[]) SharedPreferencesUtils.loadObject(
                sp_ld,sp_ld_key_tp,PointF[].class);

        PointF[] pts = (PointF[]) SharedPreferencesUtils.loadObject(
                sp_ld,sp_ld_key_op,PointF[].class
        );

        laneGuidPath = SharedValues.getPathFromPointF(pts,true);

        laneGuidPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laneGuidPathPaint.setColor(Color.BLUE);
        laneGuidPathPaint.setStrokeWidth(8);
        laneGuidPathPaint.setStyle(Paint.Style.STROKE);

        lanePointsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lanePointsPaint.setColor(Color.argb(255,255,170,0)); // 255,170,0,255 orange
        lanePointsPaint.setStrokeWidth(8);
        lanePointsPaint.setStyle(Paint.Style.STROKE);


        FrameLayout container = (FrameLayout) findViewById(R.id.container);
        initSnackbar = Snackbar.make(container, "Initializing...", Snackbar.LENGTH_INDEFINITE);
        Log.d(TAG, "onCreate: snackbar declared");
    }


    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @Override
    public void onPreviewSizeSelected(int width, int height) {

        Log.d(TAG, String.format("onPreviewSizeSelected: width = %d & height = %d", width, height));

        mWidth = width;
        mHeight = height;

//        rgbFrameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        rgbFrameBitmap = Bitmap.createBitmap(
//                DESIRED_PREVIEW_SIZES[0].getWidth(),
//                DESIRED_PREVIEW_SIZES[0].getHeight(), Bitmap.Config.ARGB_8888);
        //object Detection.
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if (mappedRecognitions != null){
                    for (RecognizedObject object: mappedRecognitions){
                        if(object.getScore() >= 0.6f) {
                            RectF  location = object.getLocation();

                            DistanceCalculator dc = new DistanceCalculator(location,object.getLabel());
                            float dist = dc.getDistance();

                            canvas.drawRect(location,borderBoxPaint);
                            canvas.drawText(
                                    String.format("%s , %.1f %%",object.getLabel(),object.getScore()*100  ),
                                    location.left,location.top < 50? location.top+60:location.top-10,borderTextPaint);
                            if(object.getLabel().matches("(?i)^car|bottle$")){
                                canvas.drawText(String.format("%.1f m", dist),location.left,
                                        location.top < 50 ? location.top + 20:location.top - 35,
                                        borderTextPaint);
                                // display warning if car some minimum distance
                                if(dist < 10){
                                    Bitmap bmp = BitmapFactory.decodeResource(getResources(),R.drawable.warning_for_distance);
                                    Bitmap bmp_resized = ImageUtilities.getResizedBitmap(bmp,(int)(location.width() - 5),
                                            (int)(location.height() -5),true);
                                    canvas.drawBitmap(bmp_resized,location.left + 5,
                                            location.top + 5,null);
                                }
                            }
                        }
                    }
                }
            }
        });

        // SignDetection
        draw.addCallback(new OverlayView.DrawCallback() {
            @SuppressLint("DefaultLocale")
            @Override
            public void drawCallback(Canvas canvas) {
                if (mappedSignRecognitions != null){
                    int count = 0;
                    for (RecognizedObject object: mappedSignRecognitions){
                        if(count >=2) break;
                        RectF  location = object.getLocation();

                        canvas.drawRect(location,borderBoxPaint);
                        canvas.drawText(
                                String.format("%s , %.1f %%",object.getLabel(),object.getScore()*100  ),
                                location.left,location.top < 50? location.top+60:location.top-10,borderTextPaint);
                        count++;
                    }
                }
                }
        });

        //LaneDetection
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if(lanePoints !=null){
//                    Log.d(TAG, "drawCallback: lanePoints = "+lanePoints.toString());
                    for(float[] line : lanePoints){
                        canvas.drawLine(line[0],line[1],
                                line[2],line[3],lanePointsPaint);
                    }
                }
            }
        });

        // lane detection advance
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if(lft_lane_pts != null){
                    canvas.drawPath(SharedValues.getPathFromPointF(lft_lane_pts,false),lanePointsPaint);
                }
                if(rht_lane_pts != null){
                    canvas.drawPath(SharedValues.getPathFromPointF(rht_lane_pts,false),lanePointsPaint);
                }
            }
        });

        // lane Mask
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
//                Log.d(TAG, "drawCallback: lanGuidLines = "+laneGuidLines);
                if(laneGuidLines){
                    canvas.drawPath(laneGuidPath, laneGuidPathPaint);
                }
            }
        });
        // debug information
        draw.addCallback(new OverlayView.DrawCallback() {
            @SuppressLint("DefaultLocale")
            @Override
            public void drawCallback(Canvas canvas) {
                if(drawDebugInfo){
                    canvas.drawText(
                            String.format("Time Taken Object Detection: %.0f ms",timeTakeByObjDetector),10,50,borderTextPaint);
                    canvas.drawText(
                            String.format( "Time Taken Sign Detection: %.0f ms",timeTakeBySignDetector),10,100,borderTextPaint);
                    canvas.drawText(
                            String.format("Time Taken Lane Detection: %.0f ms",timeTakeByLaneDetector),10,150,borderTextPaint);
                }
            }
        });
        // -----------------
        new Init().execute();
    }


    @Override
    public void processImage(int aqWidth,int aqHeight) {

        if ( aqWidth == 0 || aqHeight ==0 ) return;

        if( !isrgbFrameCreated ) {
            rgbFrameBitmap = Bitmap.createBitmap(
                    aqWidth,
                    aqHeight, Bitmap.Config.ARGB_8888);
            isrgbFrameCreated = true;
        }

        if (!initialized  ) { //|| isComputingDetection) {
            readyForNextImage();
            return;
        }

        if (isComputingLaneDetection  && isComputingDetection){
            readyForNextImage();
            return;
        }

        rgbFrameBitmap.setPixels(getRgbBytes(),
                0, aqWidth, 0, 0, aqWidth, aqHeight);

        Bitmap resizedBitmap = ImageUtilities.getResizedBitmap(rgbFrameBitmap,
                Detector.OBJ_DETECTOR_INPUT_SIZE, Detector.OBJ_DETECTOR_INPUT_SIZE,
                false);


        if(!isComputingLaneDetection)new LaneTask().execute(resizedBitmap.copy(Bitmap.Config.ARGB_8888,true));
//        if(!isComputingSignDetection)new SignTask().execute(resizedBitmap.copy(Bitmap.Config.ARGB_8888,true));
        if(!isComputingDetection) new detectorTask().execute(resizedBitmap);
        readyForNextImage();
    }

    @Override
    public void onBackPressed() {
        finish();
        Intent i = new Intent(getApplicationContext(),MainActivity.class);
        startActivity(i);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            switch (counterForVolumeDown){
                case 0:
                    laneGuidLines = true;
                    break;
                case 1:
                    drawDebugInfo = true;
                    laneGuidLines = false;
                    break;
                case 2:
                    drawDebugInfo = false;
                    break;
            }
            counterForVolumeDown++ ;
            counterForVolumeDown %=3 ;
            return true;
        }

        return super.onKeyDown(keyCode,event);
    }

    @Override
    public Size getDesiredPreviewSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        Log.d(TAG, String.format("getDesiredPreviewSize: (device resolution) device width = %d :height = %d", width,height));
        int max = Math.max(height, width);
        Log.d(TAG, String.format("getDesiredPreviewSize: (device resolution) max %d", max));
        int selectedSize = 0;
        Range<Integer> check = Range.create(max -20,max+20);
        for (Size choice :
                DESIRED_PREVIEW_SIZES) {
            Log.d(TAG, String.format("getDesiredPreviewSize: (device resolution) selectedSize %d", selectedSize));
            if (check.contains(choice.getWidth())) break;
            selectedSize++;
        }
        selectedSize= selectedSize == 0 ? 0 : selectedSize -1;
        Log.d(TAG, String.format("getDesiredPreviewSize: (device resolution) chosen width = %d :height = %d",
                DESIRED_PREVIEW_SIZES[selectedSize].getWidth(),DESIRED_PREVIEW_SIZES[selectedSize].getHeight()));
        return DESIRED_PREVIEW_SIZES[selectedSize];
    }

    @Override
    public Size getDesiredImageReaderSize() {
        return new Size(
                CROP_SIZE.getWidth(),CROP_SIZE.getWidth());
    }

    private class Init extends AsyncTask<Object,Object,Object>{

        @Override
        protected Object doInBackground(Object[] objects) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initSnackbar.show();
                }
            });

            if(OpenCVLoader.initDebug()){
                Log.d(TAG, "onCreate: Opencv Loaded Successfully");
            }else{
                Log.d(TAG, "onCreate: Opencv Could not load");
            }

            try {
                detector = Detector.create(getAssets(), Detector.OBJ_DETECTOR_MODEL,mWidth,mHeight);
                Log.d(TAG, "run: detector created");

                signDetector = SignDetector.create(getAssets(),mWidth,mHeight);
                Log.d(TAG, "run: SignDetector created");

//                laneDetector = new LaneDetector(mWidth,mHeight,300,300);
                laneDetectorAdvance = new LaneDetectorAdvance(mWidth,mHeight,
                        SharedValues.CROP_SIZE.getWidth(),SharedValues.CROP_SIZE.getHeight());
                laneDetectorAdvance.setPtsResized(pts_resized);
            } catch (Exception e) {
                Log.e(TAG,"run: Exception initializing classifier!", e);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initSnackbar.dismiss();
                }
            });

            initialized = true;
            return null;
        }
    }


    private static class detectorTask extends AsyncTask<Bitmap,Object,Object>{
        @Override
        protected Object doInBackground(Bitmap... params) {
            if(!isComputingDetection){
                isComputingDetection = true;
                float start = SystemClock.currentThreadTimeMillis();
                mappedRecognitions = detector.run(params[0],true);

                float end = SystemClock.currentThreadTimeMillis();
                timeTakeByObjDetector = end - start;
                draw.postInvalidate();
                isComputingDetection = false;
            }
            if(!params[0].isRecycled()) params[0].recycle();
            return null;
        }
    }
    private static class SignTask extends AsyncTask<Bitmap,Object,Object>{

        @Override
        protected Object doInBackground(Bitmap... params) {
            if (!isComputingSignDetection){
                isComputingSignDetection = true;
                float start  = SystemClock.currentThreadTimeMillis();
                mappedSignRecognitions = signDetector.run(params[0],true);
                float end = SystemClock.currentThreadTimeMillis();
//                Log.d(TAG, String.format("doInBackground in SignLaneTask: sign detection time = %f ms", (end-start)));
                timeTakeBySignDetector = end - start;
//                draw.postInvalidate();
                isComputingSignDetection = false;
            }
            if(!params[0].isRecycled()) params[0].recycle();
            return null;
        }
    }

    private static class LaneTask extends AsyncTask<Bitmap,Object,Object>{

        @Override
        protected Object doInBackground(Bitmap... params) {
            if(!isComputingLaneDetection){
                isComputingLaneDetection = true;
                float start = SystemClock.currentThreadTimeMillis();

                ArrayList<PointF>[] ret = laneDetectorAdvance.processFrame(params[0],false);
                lft_lane_pts = ret[0];
                rht_lane_pts = ret[1];
                float end = SystemClock.currentThreadTimeMillis();
                timeTakeByLaneDetector = end - start;
                draw.postInvalidate();
                isComputingLaneDetection = false;
//                System.gc();
            }
            if(!params[0].isRecycled()) params[0].recycle();
            return null;
        }
    }

}
