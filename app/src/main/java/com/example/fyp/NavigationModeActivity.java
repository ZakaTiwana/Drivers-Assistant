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
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customutilities.SharedPreferencesUtils;
import com.example.fyp.customutilities.SharedValues;
import com.example.fyp.customview.OverlayView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;

public class NavigationModeActivity extends CameraCaptureActivity {


    private  ScheduledThreadPoolExecutor threadExecutor = null;
    private ScheduledFuture<?> flagCheckTask;
    private  ScheduledFuture<?> directionTask;

    private static final String TAG = "NavigationModeActivity";

    private static OverlayView draw = null;
    private static List<RecognizedObject> mappedRecognitions = null;
    private static List<RecognizedObject> mappedSignRecognitions = null;
    private static final Size[] DESIRED_PREVIEW_SIZES = SharedValues.DESIRED_PREVIEW_SIZES;
    private static final Size CROP_SIZE = SharedValues.CROP_SIZE;
    private int mWidth = 0;
    private int mHeight = 0;

    private Bitmap rgbFrameBitmap = null;
    private Boolean isRgbFrameCreated = false;


    private static PointF[] pts = null;
    private static PointF[] pts_resized = null; // for lane

    //-- direction nav - steps
    private static volatile ArrayList<String> navigationSteps = null;
    private static volatile boolean hasNavSteps = false;

    private static int navStepPassed = 0;
    private static LatLng fromPosition = null;
    private static TextToSpeech tts;

    private static String maneuverDirection = null;
    private Paint bitmapFilterPaint = null;

    private static Detector detector = null;
    private static SignDetector signDetector = null;
    private static volatile boolean isComputingSignDetection = false;
    private static volatile boolean isComputingDetection = false;
    private static volatile boolean isComputingLaneDetection = false;
    private static volatile boolean isDirectionTaskCompleted = false;

    private static float[][] lanePoints = null;
    private static ArrayList<PointF> lft_lane_pts = null;
    private static ArrayList<PointF> rht_lane_pts = null;
    private Paint lanePointsPaint = null;

    private LaneDetector laneDetector = null;
    private static LaneDetectorAdvance laneDetectorAdvance = null;
    private float maskWidth;
    private float maskHeight;
    private RectF maskRect = null;
    private Matrix maneuverMatrix;

    private Snackbar initSnackbar = null;
    private volatile boolean initialized = false;

    private Paint borderBoxPaint = null;
    private Paint borderTextPaint = null;

    // checks for features
    private static boolean isLaneDetectionAllowed = false;
    private static boolean isSignDetectionAllowed = false;
    private static boolean isObjDetectionAllowed = false;
    private static boolean isVoiceWarningAllowed = false;
    private static boolean isDistanceCalculatorAllowed = false;
    private static boolean isVoiceCommandsAllowed = false;
    private static boolean isDarkModeEnabled = false;

    // distance calculator
    private static DistanceCalculator distanceCalculator = null;

    //voice commands
    private VoiceCommandRecognizer voiceCommandRecognizer = null;

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
                Log.d(TAG, "onClick: isvoice = " + isVoiceCommandsAllowed + " voiceComRec = " + voiceCommandRecognizer);
                if (isVoiceCommandsAllowed && voiceCommandRecognizer != null) {
                    voiceCommandRecognizer.setOnReadyCallback(new VoiceCommandRecognizer.OnReadyCallback() {
                        @Override
                        public void ready() {
                            showToast("Voice Commands Ready Now.");
                        }
                    });
                    boolean success = voiceCommandRecognizer.run();
                    if (success) {
                        voiceCommandRecognizer.setOnResultCallback(new VoiceCommandRecognizer.OnResultCallback() {
                            @Override
                            public void performTask(String msg) {
                                speak(msg);
                            }
                        });
                    } else {
                        if (isVoiceWarningAllowed)
                            speak("Voice command unsuccessful");
                        else
                            showToast("Voice command unsuccessful");
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

        bitmapFilterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapFilterPaint.setFilterBitmap(true);

        SharedPreferences sp_hs = getSharedPreferences(
                getString(R.string.sp_homeSettings), 0);
        String sp_hs_dark_mod = getString(R.string.sp_hs_key_darkMode);
        isDarkModeEnabled = SharedPreferencesUtils.loadBool(sp_hs, sp_hs_dark_mod);

        // change icon of mic
        if (isDarkModeEnabled) {
            voiceButton.setBackgroundResource(R.drawable.ic_mic_black);
        }

        SharedPreferences sp_ld = getSharedPreferences(
                getString(R.string.sp_laneDetection), 0);
        String sp_ld_key_tp = getString(R.string.sp_ld_key_transformed_mask_pts);
        String sp_ld_key_op = getString(R.string.sp_ld_key_original_mask_pts);
        pts_resized = (PointF[]) SharedPreferencesUtils.loadObject(
                sp_ld, sp_ld_key_tp, PointF[].class);

        pts = (PointF[]) SharedPreferencesUtils.loadObject(
                sp_ld, sp_ld_key_op, PointF[].class
        );

        maskHeight = pts[3].y - pts[0].y;
        float mid_x_1 = (pts[0].x + pts[3].x) / 2f;
        float mid_x_2 = (pts[1].x + pts[2].x) / 2f;
        maskWidth = mid_x_2 - mid_x_1;
        maskRect  = new RectF(pts[3].x,pts[0].y,pts[2].x,pts[2].y);

        lanePointsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lanePointsPaint.setColor(Color.argb(255, 255, 170, 0)); // 255,170,0,255 orange
        lanePointsPaint.setStrokeWidth(8);
        lanePointsPaint.setStyle(Paint.Style.STROKE);

        FrameLayout container = (FrameLayout) findViewById(R.id.container);
        initSnackbar = Snackbar.make(container, "Initializing...", Snackbar.LENGTH_INDEFINITE);
        Log.d(TAG, "onCreate: snackbar declared");

        final String hs_voice = getString(R.string.sp_hs_key_isVoiceCommandAllowed);
        isVoiceCommandsAllowed = SharedPreferencesUtils.loadBool(sp_hs, hs_voice);
        if (!isVoiceCommandsAllowed) {
            voiceButton.setVisibility(Button.INVISIBLE);
        } else {
            voiceCommandRecognizer = new VoiceCommandRecognizer(getApplicationContext());
            voiceCommandRecognizer.initializeSpeechRecognizer();
        }
    }

    @Override
    public void onPreviewSizeSelected(int width, int height) {
        Log.d(TAG, String.format("onPreviewSizeSelected: width = %d & height = %d", width, height));

        mWidth = width;
        mHeight = height;

        //object Detection.
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if (mappedRecognitions != null && isObjDetectionAllowed) {
                    int countVoicedWarns = 0;
                    for (RecognizedObject object : mappedRecognitions) {
                        if (object.getScore() >= 0.6f &&
                                object.getLabel().matches("car|motorcycle|person|bicycle|truck|stop sign|laptop|bottle")) {
                            RectF location = object.getLocation();

                            if (object.getLabel().contentEquals("stop sign"))
                                speak(object.getLabel());

                            canvas.drawRect(location, borderBoxPaint);
                            canvas.drawText(
                                    String.format("%s , %.1f %%", object.getLabel(), object.getScore() * 100),
                                    location.left, location.top < 50 ? location.top + 60 : location.top - 10, borderTextPaint);
                            if (distanceCalculator != null && object.getLabel().matches("(?i)^truck|motorcycle|person|car|bottle$")
                                    && isDistanceCalculatorAllowed) {
                                float dist = distanceCalculator.calculateDistance(location, object.getLabel());
                                canvas.drawText(String.format("%.1f m", dist), location.left,
                                        location.top < 50 ? location.top + 20 : location.top - 35,
                                        borderTextPaint);
                                // display warning if car some minimum distance
                                if (dist < 10 && countVoicedWarns <= 2) {
                                    countVoicedWarns++;
                                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.warning_for_distance);
                                    Bitmap bmp_resized = ImageUtilities.getResizedBitmap(bmp, (int) (location.width() - 5),
                                            (int) (location.height() - 5), true);
                                    canvas.drawBitmap(bmp_resized, location.left + 5,
                                            location.top + 5, bitmapFilterPaint);

                                    // voice warning logic
                                    if (maskRect != null && object.getLocation().intersect(maskRect))
                                        speak("A " + object.getLabel() + " is approaching beware");
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
                if (mappedSignRecognitions != null && isSignDetectionAllowed) {
                    int count = 0;
                    for (RecognizedObject object : mappedSignRecognitions) {
                        if (object.getLabel().contentEquals("yield")) continue;
                        if (count >= 2) break;
                        RectF location = object.getLocation();
                        speak(object.getLabel());
                        canvas.drawRect(location, borderBoxPaint);
                        canvas.drawText(
                                String.format("%s , %.1f %%", object.getLabel(), object.getScore() * 100),
                                location.left, location.top < 50 ? location.top + 60 : location.top - 10, borderTextPaint);
                        count++;
                    }
                }
            }
        });

        //LaneDetection - deprecated
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if (lanePoints != null && isLaneDetectionAllowed) {
//                    Log.d(TAG, "drawCallback: lanePoints = "+lanePoints.toString());
                    for (float[] line : lanePoints) {
                        canvas.drawLine(line[0], line[1],
                                line[2], line[3], lanePointsPaint);
                    }
                }
            }
        });

        // lane detection advance
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                    if (!isLaneDetectionAllowed) return;
                    if (lft_lane_pts != null && lft_lane_pts.size() > 3) {
                        canvas.drawPath(SharedValues.getPathFromPointF(lft_lane_pts, false), lanePointsPaint);
                    }
                    if (rht_lane_pts != null && rht_lane_pts.size() > 3) {
                        canvas.drawPath(SharedValues.getPathFromPointF(rht_lane_pts, false), lanePointsPaint);
                    }

                }
        });

        // direction maneuver
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if (hasNavSteps && maneuverDirection != null) {
                    Bitmap bmp = null;
                    //turn-slight-left, turn-sharp-left, uturn-left, turn-left, turn-slight-right,
                    // turn-sharp-right, uturn-right, turn-right, straight, ramp-left, ramp-right,
                    // merge, fork-left, fork-right, ferry, ferry-train, roundabout-left, roundabout-right
                    switch (maneuverDirection) {
                        case "turn-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_turn_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_turn_right);
                            break;
                        case "turn-slight-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_turn_slight_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_turn_slight_right);
                            break;
                        case "turn-sharp-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_turn_sharp_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_turn_sharp_right);
                            break;
                        case "uturn-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_uturn_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_uturn_right);
                            break;
                        case "roundabout-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_roundabout_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_roundabout_right);
                            break;
                        case "ramp-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_on_ramp_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_on_ramp_right);
                            break;
                        case "fork-right":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_fork_right);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_fork_right);
                            break;
                        case "turn-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_turn_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_turn_left);
                            break;
                        case "turn-slight-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_turn_slight_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_turn_slight_left);
                            break;
                        case "turn-sharp-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_turn_sharp_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_turn_sharp_left);
                            break;
                        case "uturn-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_uturn_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_uturn_left);
                            break;
                        case "roundabout-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_roundabout_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_roundabout_left);
                            break;
                        case "ramp-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_on_ramp_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_on_ramp_left);
                            break;
                        case "fork-left":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_fork_left);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_fork_left);
                            break;
                        case "merge":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_merge);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_merge);
                            break;
                        case "ferry":
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_directions_ferry);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_directions_ferry);
                            break;
                        default:
                            //straight
                            if (isDarkModeEnabled)
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dark_direction_turn_straight);
                            else
                                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.light_direction_turn_straight);
                            break;
                    }
                    if (bmp == null) return;

                    if (maneuverMatrix != null) {
                        Bitmap newBitmap = Bitmap.createBitmap((int) maskWidth + 50,
                                (int) maskHeight + 50, Bitmap.Config.ARGB_8888);
                        Canvas canvas1 = new Canvas(newBitmap);
                        canvas1.drawBitmap(bmp, maneuverMatrix, null);

                        canvas.drawBitmap(newBitmap, pts[0].x - maskWidth / 8,
                                pts[0].y - maskHeight / 3, bitmapFilterPaint);
                    }

                }
            }
        });

        // -----------------
        new Init().execute();
    }

    @Override
    public Size getDesiredPreviewSize() {
        SharedPreferences sp_hs = getSharedPreferences(getString(R.string.sp_homeSettings),0);
        String hs_preview_size = getString(R.string.sp_hs_key_previewSize);
        int i = sp_hs.getInt(hs_preview_size,0);
        return DESIRED_PREVIEW_SIZES[i];
    }

    @Override
    public Size getDesiredImageReaderSize() {
        return new Size(
                CROP_SIZE.getWidth(), CROP_SIZE.getWidth());
    }

    @Override
    public void processImage(int aqWidth, int aqHeight) {
        if (aqWidth == 0 || aqHeight == 0) return;

        if (!isRgbFrameCreated) {
            rgbFrameBitmap = Bitmap.createBitmap(
                    aqWidth,
                    aqHeight, Bitmap.Config.ARGB_8888);
            isRgbFrameCreated = true;
        }

        if (!initialized ||
                (!isLaneDetectionAllowed && !isObjDetectionAllowed && !isSignDetectionAllowed) ||
                (isComputingLaneDetection && isComputingDetection && isComputingSignDetection)) {

            readyForNextImage();
            return;
        }


        rgbFrameBitmap.setPixels(getRgbBytes(),
                0, aqWidth, 0, 0, aqWidth, aqHeight);

        Bitmap resizedBitmap = ImageUtilities.getResizedBitmap(rgbFrameBitmap,
                Detector.OBJ_DETECTOR_INPUT_SIZE, Detector.OBJ_DETECTOR_INPUT_SIZE,
                false);


        if (isLaneDetectionAllowed && !isComputingLaneDetection) {
            threadExecutor.schedule(new LaneTask(resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)),
                    0, TimeUnit.MILLISECONDS);
        }
        if (isSignDetectionAllowed && !isComputingSignDetection) {
            threadExecutor.schedule(new SignTask(resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)),
                    10, TimeUnit.MILLISECONDS);
        }
        if (isObjDetectionAllowed && !isComputingDetection) {
            threadExecutor.schedule(new DetectorTask(resizedBitmap),
                    10, TimeUnit.MILLISECONDS);
        }
        readyForNextImage();
    }

    private class Init extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object[] objects) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initSnackbar.show();
                }
            });

            if (OpenCVLoader.initDebug()) {
                Log.d(TAG, "onCreate: Opencv Loaded Successfully");
            } else {
                Log.d(TAG, "onCreate: Opencv Could not load");
            }

            try {

                distanceCalculator = new DistanceCalculator();

                detector = Detector.create(getAssets(), Detector.OBJ_DETECTOR_MODEL, mWidth, mHeight);
                Log.d(TAG, "run: detector created");

                signDetector = SignDetector.create(getAssets(), mWidth, mHeight);
                Log.d(TAG, "run: SignDetector created");

//                laneDetector = new LaneDetector(mWidth,mHeight,300,300);
                laneDetectorAdvance = new LaneDetectorAdvance(mWidth, mHeight,
                        SharedValues.CROP_SIZE.getWidth(), SharedValues.CROP_SIZE.getHeight());
                laneDetectorAdvance.setPtsResized(pts_resized);
                laneDetectorAdvance.setCarMidpoint((pts[3].x + pts[2].x) / 2);


            } catch (Exception e) {
                Log.e(TAG, "run: Exception initializing classifier!", e);
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
    private static class DetectorTask implements Runnable {
        private Bitmap resizedBmp = null;


        public DetectorTask(Bitmap resizedBmp) {
            this.resizedBmp = resizedBmp;
        }

        @Override
        public void run() {
            if (resizedBmp == null) return;
            if (!isComputingDetection) {
                isComputingDetection = true;
                mappedRecognitions = detector.run(resizedBmp, true);
                draw.postInvalidate();
                isComputingDetection = false;
            }
            if (!resizedBmp.isRecycled()) resizedBmp.recycle();
        }
    }

    private static class SignTask implements Runnable {
        private Bitmap resizedBmp = null;

        public SignTask(Bitmap resizedBmp) {
            this.resizedBmp = resizedBmp;
        }

        @Override
        public void run() {
            if (resizedBmp == null) return;
            if (!isComputingSignDetection) {
                isComputingSignDetection = true;
                mappedSignRecognitions = signDetector.run(resizedBmp, true);
                draw.postInvalidate();
                isComputingSignDetection = false;
            }
            if (!resizedBmp.isRecycled()) resizedBmp.recycle();
        }
    }

    private static class LaneTask implements Runnable {
        private Bitmap resizedBmp = null;

        public LaneTask(Bitmap resizedBmp) {
            this.resizedBmp = resizedBmp;
        }

        @Override
        public void run() {
            if (resizedBmp == null) return;
            if (!isComputingLaneDetection) {
                isComputingLaneDetection = true;
                ArrayList<PointF>[] ret = laneDetectorAdvance.processFrame(resizedBmp, false);
                lft_lane_pts = ret[0];
                rht_lane_pts = ret[1];
                draw.postInvalidate();
                isComputingLaneDetection = false;
            }
            if (!resizedBmp.isRecycled()) resizedBmp.recycle();
        }
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
                    .setInterval(100); // 100ms
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
                    Log.d("check", "language: " + Locale.getDefault());
                }
         initDirectionTask(); // in here so that text to speech in initilized
            }
        });
    }

    private void initDirectionTask(){
        //-- get step info --
        Intent intent = getIntent();
        navigationSteps = intent.getStringArrayListExtra(SharedValues.intent_step_info);
        Log.d(TAG, "run: got navigationSteps = " + navigationSteps);
        if (navigationSteps != null && navigationSteps.size() > 0) hasNavSteps = true;
        if (hasNavSteps && !isDirectionTaskCompleted) {
            getDeviceLocation();
            directionTask = threadExecutor.scheduleWithFixedDelay(new DirectionsTask(),3000,500,TimeUnit.MILLISECONDS) ;
            maneuverMatrix = LaneDetectorAdvance.getFlatPerspectiveMatrix(maskWidth, maskHeight);
        }
    }

    public class DirectionsTask implements Runnable {
        @Override
        public void run() {
            if (navigationSteps == null || !hasNavSteps || fromPosition == null || tts == null) {
                Log.d(TAG, ", hasSteps = "+hasNavSteps+", fromPosition = "+ fromPosition + ", tts = "+tts
                        +"DirectionsTask: navigationSteps= "+navigationSteps);
                maneuverDirection = null;
                return;
            }
            Log.d(TAG, "DirectionsTask: in directionsTask");
            String[] step;
            String distance;
            String instructions;
            double lat;
            double lng;
            String maneuver;

            boolean isStepMoved = false;
            Log.d(TAG, "run: navstepPassed = "+ navStepPassed);
            for (int i = navStepPassed; i < navigationSteps.size(); i++) {
                step = navigationSteps.get(i).split("::");
                distance = step[0];
                instructions = step[1];
                instructions = instructions.replaceAll("\\<.*?\\>", "");
                String lat1 = step[2];
                lat = Double.parseDouble(step[2]);
                lng = Double.parseDouble(step[3]);
                maneuver = step[4];
                Log.d(TAG, "DirectionsTask: current longLat = " + fromPosition.latitude + ", " + fromPosition.latitude);
                if (Math.abs(lat - fromPosition.latitude) < 0.0003) {
                    if (Math.abs(lng - fromPosition.longitude) < 0.0003) {
                        navStepPassed++;
                        mustSpeak(instructions);
                        if (maneuver != null) {
                            maneuverDirection = maneuver;
                        } else {
                            maneuverDirection = "straight";
                        }
                        draw.postInvalidate();
                        isStepMoved = true;
                        break;
                    }
                }
            }

            // return to straight
            if (isStepMoved){
                threadExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (navStepPassed == 0 ||
                                maneuverDirection == null) return;
                        if (navStepPassed == navigationSteps.size()) {
                            maneuverDirection = "straight";
                            navStepPassed++;
                            mustSpeak("Follow this last direction and you will reach your destination.");
                            threadExecutor.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    if (directionTask != null){
                                        isDirectionTaskCompleted = true;
                                        directionTask.cancel(false);
                                    }
                                }
                            },10,TimeUnit.SECONDS);
                        }
                        else {
                            // after 5 sec  straight arrow
                            maneuverDirection = "Straight";
                            draw.postInvalidate();
                        }
                    }
                }, 5, TimeUnit.SECONDS);

            }



        }
    }

    private void speak(String msg) {
        if (isVoiceWarningAllowed && tts !=null) {
            if (!tts.isSpeaking()) {
                tts.speak(msg, TextToSpeech.QUEUE_ADD, null, null);
            }
        }
    }

    private void mustSpeak(String msg) {
        if (isVoiceWarningAllowed && tts !=null) {
            tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void showToast(String msg){
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        readyForNextImage();
        stopPeriodicTask();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        startPeriodicTask();
    }

    @Override
    public void onBackPressed() {
        maneuverDirection = null;
        mappedRecognitions = null;
        mappedSignRecognitions = null;
        navigationSteps = null;
        navStepPassed = 0;
        lft_lane_pts = null;
        rht_lane_pts = null;
        fromPosition = null;
        hasNavSteps = false;
        isDirectionTaskCompleted = false;
        draw.postInvalidate();
        finishAffinity();
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
    }

    private void startPeriodicTask(){
        //---------
        threadExecutor = new ScheduledThreadPoolExecutor(10);
        initializeTextToSpeech();

        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "onCreate: Opencv Loaded Successfully");
        } else {
            Log.d(TAG, "onCreate: Opencv Could not load");
        }


        final SharedPreferences sp_fs = getSharedPreferences(getString(R.string.sp_featureSettings), 0);
        final String fs_lane = getString(R.string.sp_fs_key_isLaneAllowed);
        final String fs_obj_detect = getString(R.string.sp_fs_key_isObjDetectionAllowed);
        final String fs_sign = getString(R.string.sp_fs_key_isSignAllowed);
        final String fs_dist = getString(R.string.sp_fs_key_isDistCalAllowed);
        final String fs_mute = getString(R.string.sp_fs_key_areWarningsMuted);

        // repeatedly check if changed
        flagCheckTask = threadExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                isLaneDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs, fs_lane);
                isSignDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs, fs_sign);
                isObjDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs, fs_obj_detect);
                isDistanceCalculatorAllowed = SharedPreferencesUtils.loadBool(sp_fs, fs_dist);
                isVoiceWarningAllowed = !SharedPreferencesUtils.loadBool(sp_fs, fs_mute);
            }
        }, 3, 5, TimeUnit.SECONDS);

    }
    private void stopPeriodicTask() {
        if (tts != null) tts.shutdown();
        //--------------
        if (flagCheckTask != null){
            flagCheckTask.cancel(false);
            flagCheckTask = null;
        }

        if (directionTask !=null){
            directionTask.cancel(false);
            directionTask = null;
        }

        if(threadExecutor != null){
            threadExecutor.shutdown();
            threadExecutor = null;
        }
    }
}
