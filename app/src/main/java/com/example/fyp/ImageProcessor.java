package com.example.fyp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.example.fyp.customutilities.ImageUtilities;
import com.google.android.material.snackbar.Snackbar;
import com.example.fyp.customview.OverlayView;
import org.opencv.android.OpenCVLoader;

import java.util.List;

public class ImageProcessor extends CameraCaptureActivity {

    private static final String TAG = "ImageProcessor";
    private static final Size[] DESIRED_PREVIEW_SIZES =
            {
                    new Size(1280,720),
                    new Size(640,480),
                    new Size(720,480),
                    new Size(960,720),
                    new Size(1440,1080),
                    new Size(1920,1080),
                    new Size(2048,1152),
                    new Size(3264,1836),
                    new Size(4128,2322)
            };
    private static final int[] pts = {650,360, 750,360, 1280,600, 100,600}; // for lane

    private int mWidth = 0;
    private int mHeight = 0;

    private Bitmap rgbFrameBitmap = null;

    private static Detector detector = null;
    private static float timeTakeByObjDetector = 0;
    private static SignDetector signDetector = null;
    private static float timeTakeBySignDetector = 0;
    private static volatile boolean isComputingSignDetection = false;
    private static volatile boolean isComputingDetection = false;
    private static volatile boolean isComputingLaneDetection = false;

    private static Double[][] lanePoints = null;
    private static float timeTakeByLaneDetector = 0;
    private Paint lanePointsPaint = null;

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

        laneGuidPath = new Path();
        laneGuidPath.moveTo(pts[0],pts[1]);
        laneGuidPath.lineTo(pts[2],pts[3]);
        laneGuidPath.lineTo(pts[4],pts[5]);
        laneGuidPath.lineTo(pts[6],pts[7]);
        laneGuidPath.lineTo(pts[0],pts[1]);
        laneGuidPath.close();

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


    @Override
    public void onPreviewSizeSelected(int width, int height) {

        Log.d(TAG, String.format("onPreviewSizeSelected: width = %d & height = %d", width, height));

        mWidth = width;
        mHeight = height;


        rgbFrameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

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
                    for(Double[] line : lanePoints){
                        canvas.drawLine(line[0].floatValue(),line[1].floatValue(),
                                line[2].floatValue(),line[3].floatValue(),lanePointsPaint);
                    }
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
    public void processImage() {
//        Log.d(TAG, "processImage: computingDetection = "+ computingDetection + " initilazed = "+ initialized );

        // No mutex needed as this method is not reentrant.
        if (isComputingDetection || !initialized) {
            if (initialized && (!isComputingLaneDetection || !isComputingSignDetection) ) {
                rgbFrameBitmap.setPixels(getRgbBytes(), 0, mWidth, 0, 0, mWidth, mHeight);
                new SignLaneTask().execute(rgbFrameBitmap.copy(Bitmap.Config.ARGB_8888, true));
            }
            readyForNextImage();
            return;
        }

        isComputingDetection = true;
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, mWidth, 0, 0, mWidth, mHeight);
        new SignLaneTask().execute(rgbFrameBitmap.copy(Bitmap.Config.ARGB_8888,true));
        readyForNextImage();

//        LaneDetector laneDetector = null;
//        laneDetector = new LaneDetector(rgbFrameBitmap);
//        lanePoints = laneDetector.getResult2();

//        final Canvas canvas = new Canvas(croppedBitmap);
//        canvas.drawBitmap(rgbFrameBitmap,frameToCropTransform,null);

        float start = SystemClock.currentThreadTimeMillis();
        mappedRecognitions = detector.run(rgbFrameBitmap,false);
//        float signStart = SystemClock.currentThreadTimeMillis();
//        Log.d(TAG, String.format("processImage: Time take for only Object Detection = %f ms", (signStart-start)));
//        List<RecognizedObject> signRecognitions = signDetector.run(rgbFrameBitmap,false);
//        float signEnd = SystemClock.currentThreadTimeMillis();
//        Log.d(TAG, String.format("processImage: Time take for only Sign Detection = %f ms", (signEnd-signStart)));
        float end = SystemClock.currentThreadTimeMillis();
        timeTakeByObjDetector = end - start;
//        Log.d(TAG, String.format("processImage: Total time for Object Detection = %f ms", (end-start)));
//        mappedRecognitions.addAll(signRecognitions);
        draw.postInvalidate();
        isComputingDetection = false;

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
        Log.d(TAG, String.format("getDesiredPreviewSize: device width = %d :height = %d", width,height));
        int max = Math.max(height, width);
        int min = Math.min(height,width);
        int selectedSize = 0;
        for (Size choice :
                DESIRED_PREVIEW_SIZES) {
            if (choice.getWidth() >= max || choice.getHeight() >= min) break;
            selectedSize++;
        }
        return DESIRED_PREVIEW_SIZES[selectedSize];
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
                detector = Detector.create(getAssets(), Detector.OBJ_DETECTOR_MODEL);
                Log.d(TAG, "run: detector created");

                signDetector = SignDetector.create(getAssets());
                Log.d(TAG, "run: SignDetector created");

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

    private static class SignLaneTask extends AsyncTask<Bitmap,Object,Object>{

        @Override
        protected Object doInBackground(Bitmap... params) {
            if(!isComputingLaneDetection){
                isComputingLaneDetection = true;
                LaneDetector laneDetector = null;
                float start = SystemClock.currentThreadTimeMillis();
                laneDetector = new LaneDetector(params[0]);
                lanePoints = laneDetector.getResult2();
                float end = SystemClock.currentThreadTimeMillis();
                timeTakeByLaneDetector = end - start;
//                Log.d(TAG, String.format("doInBackground in SignLaneTask : lane detection time = %f ms", (end-start)));
                draw.postInvalidate();
                isComputingLaneDetection = false;
            }

            if (!isComputingSignDetection){
                isComputingSignDetection = true;
                float start  = SystemClock.currentThreadTimeMillis();
                mappedSignRecognitions = signDetector.run(params[0],true);
                float end = SystemClock.currentThreadTimeMillis();
//                Log.d(TAG, String.format("doInBackground in SignLaneTask: sign detection time = %f ms", (end-start)));
                timeTakeBySignDetector = end - start;
                draw.postInvalidate();
                isComputingSignDetection = false;
            }
            return null;
        }
    }

}
