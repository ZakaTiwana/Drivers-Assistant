package com.example.fyp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customutilities.SharedPreferencesUtils;
import com.example.fyp.customutilities.SharedValues;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.example.fyp.customview.OverlayView;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;

public class ImageProcessor extends CameraCaptureActivity {

    // thread handling
//    private static int coreCount = Runtime.getRuntime().availableProcessors();
    private static ScheduledExecutorService threadExecutor = Executors.newScheduledThreadPool(10);

    private static final int delayForCurrentLocation = 1000; // ms

    private static final String TAG = "ImageProcessor";
    private static final Size[] DESIRED_PREVIEW_SIZES = SharedValues.DESIRED_PREVIEW_SIZES;
    private static final Size CROP_SIZE = SharedValues.CROP_SIZE;
    private static PointF[] pts_resized = null; // for lane
    private static PointF[] pts = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private Bitmap rgbFrameBitmap = null;
    private Boolean isRgbFrameCreated = false;

    //-- direction nav - steps
    private static volatile ArrayList<String> navigationSteps = null;
    private static volatile boolean hasNavSteps = false;
    private static int navStepPassed = 0;
    private static TextToSpeech tts;
    private static LatLng fromPosition = new LatLng(0,0);
    private static String maneuverDirection = null;
    private static volatile boolean turnOffManeuverDirectionIcon = true;
    private static boolean isDarkModeEnabled = false;
    private Paint bitmapFilterPaint = null;

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
    private Paint carLinePaint = null;
    private Paint offsetLinePaint = null;
    private float maskWidth;
    private float maskHeight;
    private Matrix maneuverMatrix;

    private Snackbar initSnackbar = null;
    private volatile boolean initialized = false;

    private boolean drawDebugInfo = false;
    private int counterForVolumeDown = 0;

    private static OverlayView draw = null;

    private static List<RecognizedObject>  mappedRecognitions = null;
    private static List<RecognizedObject> mappedSignRecognitions = null;

    private Paint borderBoxPaint = null;
    private Paint borderTextPaint = null;

    // background threads
    private static ScheduledFuture<?> directionsTask = null;
    private static ScheduledFuture<?> optionCheckTask = null;

    private static String carSpeed = null;

    // checks for features
    private static boolean isLaneDetectionAllowed = false;
    private static boolean isSignDetectionAllowed = false;
    private static boolean isObjDetectionAllowed = false;
    private static boolean isVoiceWarningAllowed = false;
    private static boolean isDistanceCalculatorAllowed = false;
    private static boolean isVoiceCommandsAllowed = false;

    // distance calculator
    private static DistanceCalculator distanceCalculator = null;

    //voice commands
    private  VoiceCommandRecognizer voiceCommandRecognizer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        draw = (OverlayView) findViewById(R.id.overlay);
        //voice commands
        Button voiceButton = findViewById(R.id.btn_mic);
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: isvoice = "+isVoiceCommandsAllowed + " voiceComRec = "+voiceCommandRecognizer);
                if (isVoiceCommandsAllowed && voiceCommandRecognizer != null){
                    Toast.makeText(getApplicationContext(),"Speak",Toast.LENGTH_LONG).show();
                    boolean success = voiceCommandRecognizer.run();
                    if (success){
                        voiceCommandRecognizer.setOnResultCallback(new VoiceCommandRecognizer.OnResultCallback() {
                            @Override
                            public void performTask(String msg) {
                                speak(msg);
                            }
                        });
                    }
                    else {
                        speak("Voice command unsuccessful");
                    }

                }
            }
        });

        borderBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderBoxPaint.setColor(Color.RED);
        borderBoxPaint.setStrokeWidth(8);
        borderBoxPaint.setStyle(Paint.Style.STROKE);

        borderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderTextPaint.setColor(Color.BLUE);
        borderTextPaint.setTextSize(23);

        carLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        carLinePaint.setColor(Color.GREEN);
        carLinePaint.setStrokeWidth(5);
        carLinePaint.setTextSize(23);

        offsetLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        offsetLinePaint.setColor(Color.RED);
        offsetLinePaint.setStrokeWidth(5);
        offsetLinePaint.setTextSize(23);


        bitmapFilterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapFilterPaint.setFilterBitmap(true);

        SharedPreferences sp_hs = getSharedPreferences(
                getString(R.string.sp_homeSettings),0);
        String sp_hs_dark_mod = getString(R.string.sp_hs_key_darkMode);
        isDarkModeEnabled = SharedPreferencesUtils.loadBool(sp_hs,sp_hs_dark_mod);

        // change icon of mic
        if (isDarkModeEnabled){
            voiceButton.setBackgroundResource(R.drawable.ic_mic_black);
        }

        SharedPreferences sp_ld = getSharedPreferences(
                getString(R.string.sp_laneDetection),0);
        String sp_ld_key_tp = getString(R.string.sp_ld_key_transformed_mask_pts);
        String sp_ld_key_op = getString(R.string.sp_ld_key_original_mask_pts);
        pts_resized = (PointF[]) SharedPreferencesUtils.loadObject(
                sp_ld,sp_ld_key_tp,PointF[].class);

        pts = (PointF[]) SharedPreferencesUtils.loadObject(
                sp_ld,sp_ld_key_op,PointF[].class
        );

        maskHeight = pts[3].y - pts[0].y;
        float mid_x_1 = (pts[0].x + pts[3].x ) / 2f;
        float mid_x_2 = (pts[1].x + pts[2].x ) / 2f;
        maskWidth = mid_x_2 - mid_x_1;

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

        final String hs_voice = getString(R.string.sp_hs_key_isVoiceCommandAllowed);
        isVoiceCommandsAllowed = SharedPreferencesUtils.loadBool(sp_hs,hs_voice);
        if(!isVoiceCommandsAllowed) {
            voiceButton.setVisibility(Button.INVISIBLE);
        }else {
            voiceCommandRecognizer = new VoiceCommandRecognizer(getApplicationContext());
            voiceCommandRecognizer.initializeSpeechRecognizer();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @Override
    public void onPreviewSizeSelected(int width, int height) {

        Log.d(TAG, String.format("onPreviewSizeSelected: width = %d & height = %d", width, height));

        mWidth = width;
        mHeight = height;

        //object Detection.
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if (mappedRecognitions != null && isObjDetectionAllowed){
                    for (RecognizedObject object: mappedRecognitions){
                        if(object.getScore() >= 0.6f &&
                            object.getLabel().matches("car|motorcycle|person|bicycle|truck|stop sign|laptop|bottle")) {
                            RectF  location = object.getLocation();

                            canvas.drawRect(location,borderBoxPaint);
                            canvas.drawText(
                                    String.format("%s , %.1f %%",object.getLabel(),object.getScore()*100  ),
                                    location.left,location.top < 50? location.top+60:location.top-10,borderTextPaint);
                            if (distanceCalculator != null && object.getLabel().matches("(?i)^truck|motorcycle|person|car|bottle$")
                                && isDistanceCalculatorAllowed){
                                float dist = distanceCalculator.calculateDistance(location,object.getLabel());
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

                                    // voice warning logic
                                    speak("A "+object.getLabel()+" is approaching beware");
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
                if (mappedSignRecognitions != null && isSignDetectionAllowed){
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

        //LaneDetection - deprecated
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if(lanePoints !=null && isLaneDetectionAllowed){
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
                if (!isLaneDetectionAllowed) return;
                Log.d(TAG, "drawCallback: lane detection off center = "+laneDetectorAdvance.getOff_center());
                if(lft_lane_pts != null && lft_lane_pts.size() > 3){
                    canvas.drawPath(SharedValues.getPathFromPointF(lft_lane_pts,false),lanePointsPaint);
                }
                if(rht_lane_pts != null && rht_lane_pts.size() > 3){
                    canvas.drawPath(SharedValues.getPathFromPointF(rht_lane_pts,false),lanePointsPaint);
                }
                float x1 = (pts[3].x +pts[2].x)/2;
                float y1 = mHeight;
                float x2 = x1;
                float y2 = mHeight - (maskHeight - maskHeight/4f) ;
                canvas.drawLine(x1,y1,
                        x2,y2,carLinePaint);

                float x2_n = x2 + laneDetectorAdvance.getPixOffcenter();
                float offset = Math.abs(laneDetectorAdvance.getOff_center());
                if ( offset> 0.6) {
                    canvas.drawCircle(x2,y2,10f,offsetLinePaint);
                    canvas.drawLine(x2 ,y2,
                            x2_n,y2,offsetLinePaint);
                    canvas.drawText(String.format("offset : %.3f", offset),x2,y2+20,offsetLinePaint);
                }else {
                    canvas.drawCircle(x2,y2,10f,carLinePaint);
                    canvas.drawLine(x2 ,y2,
                            x2_n,y2,carLinePaint);
                    canvas.drawText(String.format("offset : %.3f", offset),x2,y2+20,carLinePaint);
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

                    if (carSpeed != null)
                        canvas.drawText(
                                "Speed of Car: "+carSpeed,10,200,borderTextPaint
                        );
                }
            }
        });

        // direction maneuver
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if(hasNavSteps  && maneuverDirection !=null){
                    Bitmap bmp = null;
                    //turn-slight-left, turn-sharp-left, uturn-left, turn-left, turn-slight-right,
                    // turn-sharp-right, uturn-right, turn-right, straight, ramp-left, ramp-right,
                    // merge, fork-left, fork-right, ferry, ferry-train, roundabout-left, roundabout-right
                    switch (maneuverDirection){
                        case "turn-right":
                            if(isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_turn_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_turn_right);
                            break;
                        case "turn-slight-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_turn_slight_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_turn_slight_right);
                            break;
                        case "turn-sharp-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_turn_sharp_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_turn_sharp_right);
                            break;
                        case "uturn-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_uturn_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_uturn_right);
                            break;
                        case "roundabout-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_roundabout_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_roundabout_right);
                            break;
                        case "ramp-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_on_ramp_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_on_ramp_right);
                            break;
                        case "fork-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_fork_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_fork_right);
                            break;
                        case "turn-left":
                            if(isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_turn_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_turn_left);
                            break;
                        case "turn-slight-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_turn_slight_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_turn_slight_left);
                            break;
                        case "turn-sharp-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_turn_sharp_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_turn_sharp_left);
                            break;
                        case "uturn-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_uturn_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_uturn_left);
                            break;
                        case "roundabout-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_roundabout_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_roundabout_left);
                            break;
                        case "ramp-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_on_ramp_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_on_ramp_left);
                            break;
                        case "fork-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_fork_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_fork_left);
                            break;
                        case "merge":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_merge);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_merge);
                            break;
                        case "ferry":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_directions_ferry);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_directions_ferry);
                            break;
                        default:
                            //straight
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.dark_direction_turn_straight);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(),R.drawable.light_direction_turn_straight);
                            break;
                    }
                    if(bmp == null) return;

//                    Bitmap bmp_resized = ImageUtilities.getResizedBitmap(bmp,(int)(maskWidth - maskWidth/3),
//                            (int)(maskHeight - maskHeight/10),true);
                    if(maneuverMatrix != null){
                        Bitmap newBitmap = Bitmap.createBitmap((int)maskWidth +50,
                                (int)maskHeight+50, Bitmap.Config.ARGB_8888);
                        Canvas canvas1 = new Canvas(newBitmap);
                        canvas1.drawBitmap(bmp, maneuverMatrix,null);

                    canvas.drawBitmap(newBitmap,pts[0].x - maskWidth/8,
                            pts[0].y - maskHeight/3,bitmapFilterPaint);
                    }

                }
            }
        });

        // -----------------
        new Init().execute();
    }


    @Override
    public void processImage(int aqWidth,int aqHeight) {

        if ( aqWidth == 0 || aqHeight ==0 ) return;

        if(!isRgbFrameCreated) {
            rgbFrameBitmap = Bitmap.createBitmap(
                    aqWidth,
                    aqHeight, Bitmap.Config.ARGB_8888);
            isRgbFrameCreated = true;
        }

        if (!initialized ||
            (!isLaneDetectionAllowed && !isObjDetectionAllowed && !isSignDetectionAllowed) ||
            (isComputingLaneDetection  && isComputingDetection && isComputingSignDetection) ) {

            readyForNextImage();
            return;
        }


        rgbFrameBitmap.setPixels(getRgbBytes(),
                0, aqWidth, 0, 0, aqWidth, aqHeight);

        Bitmap resizedBitmap = ImageUtilities.getResizedBitmap(rgbFrameBitmap,
                Detector.OBJ_DETECTOR_INPUT_SIZE, Detector.OBJ_DETECTOR_INPUT_SIZE,
                false);


        if(isLaneDetectionAllowed && !isComputingLaneDetection){
            threadExecutor.schedule(new LaneTask(resizedBitmap.copy(Bitmap.Config.ARGB_8888,true)),
                    0,TimeUnit.MILLISECONDS);
        }
        if(isSignDetectionAllowed && !isComputingSignDetection){
            threadExecutor.schedule(new SignTask(resizedBitmap.copy(Bitmap.Config.ARGB_8888,true)),
                    10,TimeUnit.MILLISECONDS);
        }
        if(isObjDetectionAllowed && !isComputingDetection) {
            threadExecutor.schedule(new DetectorTask(resizedBitmap),
                    10,TimeUnit.MILLISECONDS);
        }
        readyForNextImage();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
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
                distanceCalculator = new DistanceCalculator();

                detector = Detector.create(getAssets(), Detector.OBJ_DETECTOR_MODEL,mWidth,mHeight);
                Log.d(TAG, "run: detector created");

                signDetector = SignDetector.create(getAssets(),mWidth,mHeight);
                Log.d(TAG, "run: SignDetector created");

//                laneDetector = new LaneDetector(mWidth,mHeight,300,300);
                laneDetectorAdvance = new LaneDetectorAdvance(mWidth,mHeight,
                        SharedValues.CROP_SIZE.getWidth(),SharedValues.CROP_SIZE.getHeight());
                laneDetectorAdvance.setPtsResized(pts_resized);
                laneDetectorAdvance.setCarMidpoint((pts[3].x +pts[2].x)/2);

                //-- get step info --
                Intent intent = getIntent();
                navigationSteps = intent.getStringArrayListExtra(SharedValues.intent_step_info);
                Log.d(TAG, "run: got navigationSteps = "+ navigationSteps);
                if(navigationSteps != null && navigationSteps.size() > 0) hasNavSteps = true;
                if (hasNavSteps) {
                    getDeviceLocation();
                     directionsTask =  threadExecutor.scheduleAtFixedRate(new DirectionsTask(),
                            1000,delayForCurrentLocation, TimeUnit.MILLISECONDS);
//                    float width = maskWidth - maskWidth/3;
//                    float height = maskHeight - maskHeight/10;
                    maneuverMatrix = LaneDetectorAdvance.getFlatPerspectiveMatrix(maskWidth,maskHeight);
                }
                final SharedPreferences sp_bt = getSharedPreferences(getString(R.string.sp_blueTooth),0);
                final String key_bt_conn = getString(R.string.sp_bt_key_isDeviceConnected);
                final String key_bt_speed = getString(R.string.sp_bt_key_car_speed);
                // if obd connected then get speed at intervals
                if (SharedPreferencesUtils.loadBool(sp_bt,key_bt_conn)) {
                    threadExecutor.scheduleWithFixedDelay(new Runnable() {
                        @Override
                        public void run() {
                            carSpeed = sp_bt.getString(key_bt_speed,null);
                        }
                    }, 2, 5, TimeUnit.SECONDS);
                }


                final SharedPreferences sp_fs = getSharedPreferences(getString(R.string.sp_featureSettings),0);
                final String fs_lane = getString(R.string.sp_fs_key_isLaneAllowed);
                final String fs_obj_detect = getString(R.string.sp_fs_key_isObjDetectionAllowed);
                final String fs_sign = getString(R.string.sp_fs_key_isSignAllowed);
                final String fs_dist = getString(R.string.sp_fs_key_isDistCalAllowed);
                final String fs_mute = getString(R.string.sp_fs_key_areWarningsMuted);


                // repeatedly check if changed
                 optionCheckTask =  threadExecutor.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        isLaneDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs,fs_lane);
                        isSignDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs,fs_sign);
                        isObjDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs,fs_obj_detect);
                        isDistanceCalculatorAllowed = SharedPreferencesUtils.loadBool(sp_fs,fs_dist);
                        isVoiceWarningAllowed = ! SharedPreferencesUtils.loadBool(sp_fs,fs_mute);
                    }
                }, 3, 5, TimeUnit.SECONDS);
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


    private static class DetectorTask implements Runnable{
        private Bitmap resizedBmp = null;;
        public DetectorTask(Bitmap resizedBmp){
            this.resizedBmp = resizedBmp;
        }

        @Override
        public void run() {
            if(resizedBmp == null) return;
            if(!isComputingDetection){
                isComputingDetection = true;
                float start = SystemClock.currentThreadTimeMillis();
                mappedRecognitions = detector.run(resizedBmp,true);

                float end = SystemClock.currentThreadTimeMillis();
                timeTakeByObjDetector = end - start;
                draw.postInvalidate();
                isComputingDetection = false;
            }
            if(!resizedBmp.isRecycled()) resizedBmp.recycle();
        }
    }
    private static class SignTask implements Runnable{
        private Bitmap resizedBmp = null;
        public SignTask(Bitmap resizedBmp){
            this.resizedBmp = resizedBmp;
        }
        @Override
        public void run() {
            if(resizedBmp == null) return;
            if (!isComputingSignDetection){
                isComputingSignDetection = true;
                float start  = SystemClock.currentThreadTimeMillis();
                mappedSignRecognitions = signDetector.run(resizedBmp,true);
                float end = SystemClock.currentThreadTimeMillis();
//                Log.d(TAG, String.format("doInBackground in SignLaneTask: sign detection time = %f ms", (end-start)));
                timeTakeBySignDetector = end - start;
                draw.postInvalidate();
                isComputingSignDetection = false;
            }
            if(!resizedBmp.isRecycled()) resizedBmp.recycle();
        }
    }

    private static class LaneTask implements Runnable{
        private Bitmap resizedBmp = null;
        public LaneTask(Bitmap resizedBmp){
            this.resizedBmp = resizedBmp;
        }
        @Override
        public void run() {
            if(resizedBmp == null) return;
            if(!isComputingLaneDetection){
                isComputingLaneDetection = true;
                float start = SystemClock.currentThreadTimeMillis();

                ArrayList<PointF>[] ret = laneDetectorAdvance.processFrame(resizedBmp,false);
                lft_lane_pts = ret[0];
                rht_lane_pts = ret[1];
                float end = SystemClock.currentThreadTimeMillis();
                timeTakeByLaneDetector = end - start;
                draw.postInvalidate();
                isComputingLaneDetection = false;
//                System.gc();
            }
            if(!resizedBmp.isRecycled()) resizedBmp.recycle();
        }
    }

    //------------ Navigation -------------
    private void initializeTextToSpeech() {

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (tts.getEngines().size() == 0) {
                    Toast.makeText(getApplicationContext(), "No TTS engine on your device", Toast.LENGTH_LONG).show();
                    Log.d("check", "No TTS engine on your device");
                } else {
                    tts.setLanguage(Locale.getDefault());
                    tts.setLanguage(Locale.US);
                    //   Toast.makeText(getApplicationContext(),"check"+Locale.getDefault(),Toast.LENGTH_SHORT);
                    Log.d("check", "language: " + Locale.getDefault());

//                    speak("Oye bhai kesa hai");
                }
            }
        });
    }
    @Override
    protected void onPause() {
        readyForNextImage();
        super.onPause();
        if(threadExecutor != null){
            threadExecutor.shutdown();
        }

        if(tts != null) tts.shutdown();
    }

    @Override
    protected void onDestroy() {
        if(threadExecutor != null){
            threadExecutor.shutdown();
        }
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        if (threadExecutor.isShutdown()){
            threadExecutor = Executors.newScheduledThreadPool(10);
        }
        if (directionsTask != null && directionsTask.isCancelled()) {
            directionsTask = threadExecutor.scheduleAtFixedRate(new DirectionsTask(),
                    1000,delayForCurrentLocation, TimeUnit.MILLISECONDS);;//
        }
        if (optionCheckTask != null && optionCheckTask.isCancelled()){
            final SharedPreferences sp_fs = getSharedPreferences(getString(R.string.sp_featureSettings),0);
            final String fs_lane = getString(R.string.sp_fs_key_isLaneAllowed);
            final String fs_obj_detect = getString(R.string.sp_fs_key_isObjDetectionAllowed);
            final String fs_sign = getString(R.string.sp_fs_key_isSignAllowed);
            final String fs_dist = getString(R.string.sp_fs_key_isDistCalAllowed);
            final String fs_mute = getString(R.string.sp_fs_key_areWarningsMuted);

            final SharedPreferences sp_hs = getSharedPreferences(getString(R.string.sp_homeSettings),0);
            final String hs_voice = getString(R.string.sp_hs_key_isVoiceCommandAllowed);
            isVoiceCommandsAllowed = SharedPreferencesUtils.loadBool(sp_hs,hs_voice);

            // repeatedly check if changed
            optionCheckTask =
                     threadExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    isLaneDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs,fs_lane);
                    isSignDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs,fs_sign);
                    isObjDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs,fs_obj_detect);
                    isDistanceCalculatorAllowed = SharedPreferencesUtils.loadBool(sp_fs,fs_dist);
                    isVoiceWarningAllowed = ! SharedPreferencesUtils.loadBool(sp_fs,fs_mute);
                }
            }, 3, 5, TimeUnit.SECONDS);
        }
//        Reinitialize the tts engines upon resuming from background such as after opening the browser
        initializeTextToSpeech();
        if(voiceCommandRecognizer != null) voiceCommandRecognizer.initializeSpeechRecognizer();
    }

    private void getDeviceLocation() {
        Log.d("TAG", "getDeviceLocation: getting the devices current location");
//        LatLng fromPosition = new LatLng(0,0);
        try {
            SmartLocation smartLocation = null;
            LocationParams.Builder builder;
            smartLocation = new SmartLocation.Builder(getApplicationContext()).logging(true).build();
            builder = new LocationParams.Builder()
                    .setAccuracy(LocationAccuracy.HIGH)
                    .setDistance(0)
                    .setInterval(delayForCurrentLocation);
            try {
                smartLocation.with(getApplicationContext())
                        .location()
                        .config(LocationParams.BEST_EFFORT)
                        .continuous()
                        .config(builder.build())
                        .start(new OnLocationUpdatedListener() {
                            @Override
                            public void onLocationUpdated(Location location) {
                                fromPosition = new LatLng(location.getLatitude(), location.getLongitude());
                            }
                        });
            } catch (SecurityException se) {
                se.printStackTrace();
            }

        } catch (SecurityException e) {
            Log.e("TAG", "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }
    public class DirectionsTask implements Runnable {
        @Override
        public void run() {
            if (navigationSteps == null) {
                Log.d(TAG, "doInBackground: navigationSteps is null");
                return;
            }
            Log.d(TAG, "run: in directionsTask");
            String[] step;
            String distance;
            String instructions;
            double lat;
            double lng;
            String maneuver;

            for (int i = navStepPassed; i < navigationSteps.size(); i++) {
                step = navigationSteps.get(i).split("::");
                distance = step[0];
                instructions = step[1];
                instructions = instructions.replaceAll("\\<.*?\\>", "");
                String lat1 = step[2];
                lat = Double.parseDouble(step[2]);
                lng = Double.parseDouble(step[3]);
                maneuver = step[4];
                Log.d(TAG, "doInBackground: current longLat = "+fromPosition.latitude +", "+ fromPosition.latitude);
                if (Math.abs(lat - fromPosition.latitude) < 0.001) {
                    if (Math.abs(lng - fromPosition.longitude) < 0.001) {
                        navStepPassed++;
                        speak(instructions);
                       if (maneuver != null) {
                            maneuverDirection = maneuver;
                       } else {
                           maneuverDirection = "straight";
                       }
                        draw.postInvalidate();
                       break;
                    }
                }
            }
            // return to straight
            threadExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    if( navStepPassed == 0 || navStepPassed >= navigationSteps.size() ||
                    maneuverDirection == null) return;
                    String[] step = navigationSteps
                            .get(navStepPassed -1 ).split("::"); // one previous
                    String lat1 = step[2];
                    double lat = Double.parseDouble(step[2]);
                    double lng = Double.parseDouble(step[3]);
                    // if passed current point
                    Log.d(TAG, "run: in straight check diff lat= "+ (lat - fromPosition.latitude));
                    Log.d(TAG, "run: in straight check diff long= "+ (lng - fromPosition.longitude));
                    if (Math.abs(lat - fromPosition.latitude) > 0.001) {
                        if (Math.abs(lng - fromPosition.longitude) > 0.001) {
                            maneuverDirection = "Straight";
                            draw.postInvalidate();
                        }
                    }
                }
            }, 5, TimeUnit.SECONDS);


            if(navStepPassed >= navigationSteps.size()){
                // end navigation
                // need to check langitude longitude
                maneuverDirection = null;
                speak("You have reached Your destination");
                if (directionsTask !=null){
                    directionsTask.cancel(false);
                }
            }
        }
    }

    private  void speak(String msg){
        if(isVoiceWarningAllowed){
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
        }else {
            Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
        }
    }


}
