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
import android.speech.tts.TextToSpeech;
import android.util.Log;
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
import com.google.android.material.snackbar.Snackbar;
import com.example.fyp.customview.OverlayView;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AssistantModeActivity extends CameraCaptureActivity {

    // thread handling
    private  ScheduledThreadPoolExecutor threadExecutor = null;
    private  ScheduledFuture<?> flagCheckTask;
    private  ScheduledFuture<?> obdStoppedTask;

    private static final String TAG = "AssistantModeActivity";
    private static final Size[] DESIRED_PREVIEW_SIZES = SharedValues.DESIRED_PREVIEW_SIZES;
    private static final Size CROP_SIZE = SharedValues.CROP_SIZE;
    private static PointF[] pts_resized = null; // for lane
    private static PointF[] pts = null;

    private int mWidth = 0;
    private int mHeight = 0;

    private Bitmap warningBitmap = null;
    private Bitmap rgbFrameBitmap = null;
    private Boolean isRgbFrameCreated = false;

    private static TextToSpeech tts;
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
    private static ArrayList<PointF> all_lane_pts = null;
    private static float timeTakeByLaneDetector = 0;
    private Paint lanePointsPaint = null;

    private LaneDetector laneDetector = null;
    private static LaneDetectorAdvance laneDetectorAdvance = null;
    private static boolean showLaneGuidLines = false;
    private Path laneGuidPath = null;
    private Paint laneGuidPathPaint = null;
    private Paint carLinePaint = null;
    private Paint offsetLinePaint = null;
    private Paint carSpeedBgPaint = null;
    private Paint carSpeedTextPaint = null;

    private float maskWidth;
    private float maskHeight;
    private RectF maskRect = null;

    private Snackbar initSnackbar = null;
    private volatile boolean initialized = false;

    private boolean drawDebugInfo = false;
    private boolean drawCarSpeed = false;
    private int counterForVolumeDown = 0;

    private static OverlayView draw = null;

    private static List<RecognizedObject> mappedRecognitions = null;
    private static List<RecognizedObject> mappedSignRecognitions = null;

    private Paint borderBoxPaint = null;
    private Paint borderTextPaint = null;

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

        warningBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.warning_for_distance);

        borderBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderBoxPaint.setColor(Color.RED);
        borderBoxPaint.setStrokeWidth(8);
        borderBoxPaint.setStyle(Paint.Style.STROKE);

        borderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderTextPaint.setColor(Color.BLUE);
        borderTextPaint.setTextSize(50);

        carSpeedBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        carSpeedBgPaint.setColor(Color.parseColor("#1F2833"));
        carSpeedBgPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        carSpeedTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        carSpeedTextPaint.setColor(Color.parseColor("#008577"));
        carSpeedTextPaint.setTextSize(80);



        bitmapFilterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapFilterPaint.setFilterBitmap(true);

        SharedPreferences sp_hs = getSharedPreferences(
                getString(R.string.sp_homeSettings), 0);
        String sp_hs_dark_mod = getString(R.string.sp_hs_key_darkMode);
        isDarkModeEnabled = SharedPreferencesUtils.loadBool(sp_hs, sp_hs_dark_mod);

        // change icon of mic
//        if (!isDarkModeEnabled) {
//            voiceButton.setBackgroundResource(R.drawable.ic_mic_grey);
//        }

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
        maskRect  = new RectF(mid_x_1 ,pts[0].y,
                mid_x_2 ,pts[2].y);
        laneGuidPath = SharedValues.getPathFromPointF(pts, true);

        laneGuidPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laneGuidPathPaint.setColor(Color.BLUE);
        laneGuidPathPaint.setStrokeWidth(8);
        laneGuidPathPaint.setStyle(Paint.Style.STROKE);

        lanePointsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lanePointsPaint.setColor(Color.argb(255, 255, 170, 0)); // 255,170,0,255 orange
        lanePointsPaint.setStrokeWidth(8);
        lanePointsPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        lanePointsPaint.setAlpha(100);

        offsetLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        offsetLinePaint.setColor(Color.RED);
        offsetLinePaint.setStrokeWidth(8);
        offsetLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        offsetLinePaint.setAlpha(100);

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
//                                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.warning_for_distance);
                                    Bitmap bmp_resized = ImageUtilities.getResizedBitmap(warningBitmap, (int) (location.width() - 5),
                                            (int) (location.height() - 5), false);
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
                        if (object.getLabel().contains("speed")){
                            speak("Caution Speed limit Ahead");
                        }
                        if (count >= 1) break;
                        RectF location = object.getLocation();
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
                Log.d(TAG, "drawCallback: lane detection off center = " + laneDetectorAdvance.getOff_center());
//                if (lft_lane_pts != null && lft_lane_pts.size() > 3) {
//                    canvas.drawPath(SharedValues.getPathFromPointF(lft_lane_pts, false), lanePointsPaint);
//                }
//                if (rht_lane_pts != null && rht_lane_pts.size() > 3) {
//                    canvas.drawPath(SharedValues.getPathFromPointF(rht_lane_pts, false), lanePointsPaint);
//                }

                float offset = Math.abs(laneDetectorAdvance.getOff_center());
                if (offset > 1.5){
                    if (lft_lane_pts != null && rht_lane_pts != null){
                        ArrayList<PointF> lane__ = new ArrayList<>();

                        lane__.add(lft_lane_pts.get(lft_lane_pts.size()-1));
                        lane__.get(0).y = Math.max(lft_lane_pts.get(lft_lane_pts.size()-1).y,
                                rht_lane_pts.get(rht_lane_pts.size()-1).y);
                        lane__.add(rht_lane_pts.get(rht_lane_pts.size()-1));
                        lane__.get(1).y = lane__.get(0).y;

                        lane__.add(rht_lane_pts.get(0));
                        lane__.get(2).y = Math.min(lft_lane_pts.get(0).y,
                                rht_lane_pts.get(0).y);
                        lane__.add(lft_lane_pts.get(0));
                        lane__.get(3).y = lane__.get(2).y;

                        canvas.drawPath(SharedValues.getPathFromPointF(lane__, true), offsetLinePaint);
                    }
                }else {
                    if (lft_lane_pts != null && rht_lane_pts != null){
                        ArrayList<PointF> lane__ = new ArrayList<>();
                        lane__.add(lft_lane_pts.get(lft_lane_pts.size()-1));
                        lane__.get(0).y = Math.max(lft_lane_pts.get(lft_lane_pts.size()-1).y,
                                rht_lane_pts.get(rht_lane_pts.size()-1).y);
                        lane__.add(rht_lane_pts.get(rht_lane_pts.size()-1));
                        lane__.get(1).y = lane__.get(0).y;

                        lane__.add(rht_lane_pts.get(0));
                        lane__.get(2).y = Math.min(lft_lane_pts.get(0).y,
                                rht_lane_pts.get(0).y);
                        lane__.add(lft_lane_pts.get(0));
                        lane__.get(3).y = lane__.get(2).y;
                        canvas.drawPath(SharedValues.getPathFromPointF(lane__, true), lanePointsPaint);
                    }
                }


//                float x1 = (pts[3].x + pts[2].x) / 2;
//                float y1 = mHeight;
//                float x2 = x1;
//                float y2 = mHeight - (maskHeight - maskHeight / 4f);
//                canvas.drawLine(x1, y1,
//                        x2, y2, carLinePaint);
//
//                float x2_n = x2 + laneDetectorAdvance.getPixOffcenter();
//                float offset = Math.abs(laneDetectorAdvance.getOff_center());
//                if (offset > 0.9) {
//                    canvas.drawCircle(x2, y2, 10f, offsetLinePaint);
//                    canvas.drawLine(x2, y2,
//                            x2_n, y2, offsetLinePaint);
//                    canvas.drawText(String.format("offset : %.3f", offset), x2, y2 + 20, offsetLinePaint);
//                } else {
//                    canvas.drawCircle(x2, y2, 10f, carLinePaint);
//                    canvas.drawLine(x2, y2,
//                            x2_n, y2, carLinePaint);
//                    canvas.drawText(String.format("offset : %.3f", offset), x2, y2 + 20, carLinePaint);
//                }

            }
        });

        // lane Mask
        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
//                Log.d(TAG, "drawCallback: lanGuidLines = "+laneGuidLines);
                if (showLaneGuidLines) {
                    canvas.drawPath(laneGuidPath, laneGuidPathPaint);
                }
            }
        });
        // debug information
        draw.addCallback(new OverlayView.DrawCallback() {
            @SuppressLint("DefaultLocale")
            @Override
            public void drawCallback(Canvas canvas) {
                if (drawDebugInfo) {
                    canvas.drawText(
                            String.format("Time Taken Object Detection: %.0f ms", timeTakeByObjDetector), 10, 50, borderTextPaint);
                    canvas.drawText(
                            String.format("Time Taken Sign Detection: %.0f ms", timeTakeBySignDetector), 10, 100, borderTextPaint);
                    canvas.drawText(
                            String.format("Time Taken Lane Detection: %.0f ms", timeTakeByLaneDetector), 10, 150, borderTextPaint);


                }
                if (drawCarSpeed ){
                    if (carSpeed != null){
                        canvas.drawRect(new RectF(0,0,mWidth/3,mHeight/6),carSpeedBgPaint);
                        canvas.drawText(
                                "Speed of Car: " +carSpeed+ " km/h", 10, (mHeight/6)/2 + 20, carSpeedTextPaint
                        );
                    }


                }
            }
        });

        // -----------------
        new Init().execute();
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
                float start = SystemClock.currentThreadTimeMillis();
                mappedRecognitions = detector.run(resizedBmp, true);

                float end = SystemClock.currentThreadTimeMillis();
                timeTakeByObjDetector = end - start;
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
                float start = SystemClock.currentThreadTimeMillis();
                mappedSignRecognitions = signDetector.run(resizedBmp, true);
                float end = SystemClock.currentThreadTimeMillis();
//                Log.d(TAG, String.format("doInBackground in SignLaneTask: sign detection time = %f ms", (end-start)));
                timeTakeBySignDetector = end - start;
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
                float start = SystemClock.currentThreadTimeMillis();

                ArrayList<PointF>[] ret = laneDetectorAdvance.processFrame(resizedBmp, false);
                lft_lane_pts = ret[0];
                rht_lane_pts = ret[1];
                float end = SystemClock.currentThreadTimeMillis();
                timeTakeByLaneDetector = end - start;
                draw.postInvalidate();
                isComputingLaneDetection = false;
//                System.gc();
            }
            if (!resizedBmp.isRecycled()) resizedBmp.recycle();
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



    private void speak(String msg) {
        if (isVoiceWarningAllowed && tts !=null) {
            if (!tts.isSpeaking()) {
                tts.speak(msg, TextToSpeech.QUEUE_ADD, null, null);
            }
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
        mappedRecognitions = null;
        mappedSignRecognitions = null;
        counterForVolumeDown = 0;
        drawDebugInfo = false;
        lft_lane_pts = null;
        rht_lane_pts = null;
        showLaneGuidLines = false;
        draw.postInvalidate();
        finishAffinity();
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            switch (counterForVolumeDown) {
                case 0:
                    showLaneGuidLines = true;
                    break;
                case 1:
                    drawDebugInfo = true;
                    showLaneGuidLines = false;
                    break;
                case 2:
                    drawDebugInfo = false;
                    break;
            }
            counterForVolumeDown++;
            counterForVolumeDown %= 3;
            draw.postInvalidate();
            return true;
        }else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            drawCarSpeed = !drawCarSpeed;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void startPeriodicTask(){
        initializeTextToSpeech();
        //---------
        threadExecutor = new ScheduledThreadPoolExecutor(10);


        final SharedPreferences sp_bt = getSharedPreferences(getString(R.string.sp_blueTooth), 0);
        final String key_bt_conn = getString(R.string.sp_bt_key_isDeviceConnected);
        final String key_bt_speed = getString(R.string.sp_bt_key_car_speed);
        // if obd connected then get speed at intervals
//        if (SharedPreferencesUtils.loadBool(sp_bt, key_bt_conn)) {
            obdStoppedTask = threadExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: OBD-ii speed checker");
                    carSpeed = sp_bt.getString(key_bt_speed, null);
                    draw.postInvalidate();
                }
            }, 1,1 , TimeUnit.MILLISECONDS);
//        }


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
                Log.d(TAG, "run: AssistantMode flag checks");
                isLaneDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs, fs_lane);
                isSignDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs, fs_sign);
                isObjDetectionAllowed = SharedPreferencesUtils.loadBool(sp_fs, fs_obj_detect);
                isDistanceCalculatorAllowed = SharedPreferencesUtils.loadBool(sp_fs, fs_dist);
                isVoiceWarningAllowed = !SharedPreferencesUtils.loadBool(sp_fs, fs_mute);
            }
        }, 1, 1, TimeUnit.SECONDS);

    }
    private void stopPeriodicTask() {
        if (tts != null) tts.shutdown();
        //--------------
        if (flagCheckTask != null){
            flagCheckTask.cancel(false);
            flagCheckTask = null;
        }

        if (obdStoppedTask !=null){
            obdStoppedTask.cancel(false);
            obdStoppedTask = null;
        }

        if(threadExecutor != null){
            threadExecutor.shutdown();
            threadExecutor = null;
        }
    }
}
