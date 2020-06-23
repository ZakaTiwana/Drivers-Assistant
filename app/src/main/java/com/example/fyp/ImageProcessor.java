package com.example.fyp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.android.material.snackbar.Snackbar;
import com.example.fyp.customview.OverlayView;
import org.opencv.android.OpenCVLoader;

import java.util.List;


public class ImageProcessor extends CameraCaptureActivity {

    private static final String TAG = "ImageProcessor";
    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280,720);
    private static final int[] pts = {650,360, 750,360, 1280,600, 100,600}; // for lane

    private int mWidth = 0;
    private int mHeight = 0;

    private Bitmap rgbFrameBitmap = null;
//    private Bitmap croppedBitmap = null;

//    private Matrix frameToCropTransform = null;
//    private Matrix cropToFrameTransform = null;

    private Detector detector = null;
    private volatile boolean computingDetection = false;

    private LaneDetector laneDetector = null;
    private Double[][] lanePoints = null;
    private Paint lanePointsPaint = null;
    private volatile boolean computinLane = false;

    private volatile boolean laneGuidLines = false;
    private Path laneGuidPath = null;
    private Paint laneGuidPathPaint = null;

    private Snackbar initSnackbar = null;
    private volatile boolean initialized = false;


    private OverlayView draw = null;

    private List<RecognizedObject> mappedRecognitions = null;

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


        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "onCreate: Opencv Loaded Successfully");
        }else{
            Log.d(TAG, "onCreate: Opencv Could not load");
        }

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
//        rgbFrameBitmap = Bitmap.createBitmap(DESIRED_PREVIEW_SIZE.getWidth(), DESIRED_PREVIEW_SIZE.getHeight(), Bitmap.Config.ARGB_8888);
//        croppedBitmap = Bitmap.createBitmap(CROP_SIZE, CROP_SIZE, Bitmap.Config.ARGB_8888);

//        frameToCropTransform =
//                ImageUtilities.getTransformationMatrix(
//                        width, height,
//                        CROP_SIZE, CROP_SIZE,
//                        0, false);
//
//        cropToFrameTransform = new Matrix();
//        frameToCropTransform.invert(cropToFrameTransform);

        // ------------------

//        draw.addCallback(new OverlayView.DrawCallback() {
//            @Override
//            public void drawCallback(Canvas canvas) {
//                canvas.drawBitmap(croppedBitmap.copy(Bitmap.Config.ARGB_8888,false),0,0,null);
//            }
//        });
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if (mappedRecognitions != null){
                    for (RecognizedObject object: mappedRecognitions){
                        if(object.getScore() >= 0.6f) {
                            RectF  location = object.getLocation();
//                            cropToFrameTransform.mapRect(location);

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
                            }
                        }
                    }
                }

            }
        });

        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if(lanePoints !=null){
//                    Log.d(TAG, "drawCallback: lanePoints = "+lanePoints.toString());
                    for(Double[] line : lanePoints){
                        canvas.drawLine(line[0].floatValue(),line[1].floatValue(),line[2].floatValue(),line[3].floatValue(),lanePointsPaint);
                    }
                }
            }
        });

        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
//                Log.d(TAG, "drawCallback: lanGuidLines = "+laneGuidLines);
                if(laneGuidLines){
                    canvas.drawPath(laneGuidPath, laneGuidPathPaint);
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
        if (computingDetection || !initialized) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, mWidth, 0, 0, mWidth, mHeight);
//        rgbFrameBitmap.setPixels(getRgbBytes(), 0, DESIRED_PREVIEW_SIZE.getWidth(), 0, 0, DESIRED_PREVIEW_SIZE.getWidth(), DESIRED_PREVIEW_SIZE.getHeight());
        readyForNextImage();

        laneDetector = null;
        laneDetector = new LaneDetector(rgbFrameBitmap);
        lanePoints = laneDetector.getResult2();

//        final Canvas canvas = new Canvas(croppedBitmap);
//        canvas.drawBitmap(rgbFrameBitmap,frameToCropTransform,null);

        mappedRecognitions = detector.run(rgbFrameBitmap,true);
        draw.postInvalidate();
        computingDetection = false;

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            laneGuidLines = !laneGuidLines;
            return true;
        }

        return super.onKeyDown(keyCode,event);
    }

    @Override
    public Size getDesiredPreviewSize() {
        return DESIRED_PREVIEW_SIZE;
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

            try {
                detector = Detector.create(getAssets(), Detector.OBJ_DETECTOR_MODEL);
                Log.d(TAG, "run: detector created");
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

}
