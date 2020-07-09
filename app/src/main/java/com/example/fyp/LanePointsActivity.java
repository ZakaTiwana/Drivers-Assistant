package com.example.fyp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customutilities.SharedPreferencesUtils;
import com.example.fyp.customview.LanePointsView;
import com.example.fyp.customview.OverlayView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LanePointsActivity extends AppCompatActivity {

    private static final String TAG = "LanePointsActivity";
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;

    private static final Size[] DESIRED_PREVIEW_SIZES =
            {
                    new Size(640,480),
                    new Size(720,480),
                    new Size(960,720),
                    new Size(1280,720),
                    new Size(1440,1080),
                    new Size(1920,1080),
                    new Size(2048,1152),
                    new Size(3264,1836),
                    new Size(4128,2322)
            };

    private TextureView mTextureView;
    private LanePointsView lanePointsView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, String.format("onSurfaceTextureAvailable: width = %d and height = %d", width,height));
            setupCamera(width, height);
            transformImage(width,height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
            Log.d(TAG,"At mCameraDeviceStateCallback onOpened");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
            Log.e(TAG,"At mCameraDeviceStateCallback onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
            Log.e(TAG,"At mCameraDeviceStateCallback onError error = "+error);
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
//            super.onClosed(camera);
//            camera.close();
            mCameraDevice = null;
            Log.e(TAG,"At mCameraDeviceStateCallback onClose.");
        }
    };
    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;

    private String mCameraId;
    private Size mPreviewSize;

    private CameraCaptureSession mPreviewCaptureSession;
    private CaptureRequest.Builder mPreviewRequestBuilder;



    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum( (long)(lhs.getWidth() * lhs.getHeight()) -
                    (long)(rhs.getWidth() * rhs.getHeight()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lane_points);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTextureView = (TextureView) findViewById(R.id.textureView_lane_points);
        lanePointsView = findViewById(R.id.lanePointsView);
        Button btn_set_lane_p = findViewById(R.id.btn_set_lane_points);

        final String sp_ld = getString(R.string.sp_laneDetection);
        final String sp_ld_key_op = getString(R.string.sp_ld_key_original_mask_pts);
        final String sp_ld_key_tp = getString(R.string.sp_ld_key_transformed_mask_pts);
        final SharedPreferences sp = getSharedPreferences(sp_ld,0);
        btn_set_lane_p.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PointF[] op = lanePointsView.getPoints();
                PointF[] tp = lanePointsView.getTransformPoints(300,300);

                SharedPreferencesUtils.saveObject(sp,sp_ld_key_op,op);
                SharedPreferencesUtils.saveObject(sp,sp_ld_key_tp,tp);
            }
        });

        Button btn_get_lane_p = findViewById(R.id.btn_get_lane_points);
        btn_get_lane_p.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PointF[] transformedPoints = (PointF[]) SharedPreferencesUtils.loadObject(
                        sp,sp_ld_key_tp,PointF[].class);
                Log.d(TAG, String.format("onClick: transformedPoints = %s",
                        Arrays.asList(transformedPoints)));
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(mTextureView.isAvailable()) {
            Log.d(TAG, String.format("onResume: mTextureView.width = %d and height = %d",
                    mTextureView.getWidth(),mTextureView.getHeight()));
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            transformImage(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CAMERA_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not run without camera services", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            Size desiredInput = getDesiredPreviewSize();
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                int mWidth = desiredInput.getWidth();
                int mHeight = desiredInput.getHeight();

                Log.d(TAG, String.format("setupCamera: (device resolution) mWidth = %d and mHeight = %d", mWidth, mHeight));
                assert map != null;
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), mWidth, mHeight);

                onPreviewSizeSelected(mWidth, mHeight);
                // set overlay and texture view width and height
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        mPreviewSize.getWidth(),mPreviewSize.getHeight());
                params.gravity = Gravity.CENTER_HORIZONTAL;

                mTextureView.setLayoutParams(params);
                lanePointsView.setLayoutParams(params);

                mCameraId = cameraId;

                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
                        Toast.makeText(getApplicationContext(),
                                "Video app required access to camera", Toast.LENGTH_SHORT).show();
                    }
                }

            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private synchronized void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigured: startPreview");
                            mPreviewCaptureSession = session;
                            try {
                                if (mCameraDevice != null)
                                    mPreviewCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                                            null, mBackgroundHandler); // mPreviewCaptureCallback

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed: startPreview");

                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void closeCamera() {
        if(mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("CameraThread");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option : choices) {
            Log.d(TAG, String.format("chooseOptimalSize: option = %s", option.toString()));
            if(option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                Log.d(TAG, String.format("chooseOptimalSize: in if option = %s", option.toString()));
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0) {
            return Collections.min(bigEnough, new LanePointsActivity.CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    private void transformImage(int width, int height){
        if(mPreviewSize == null || mTextureView == null){
            return;
        }

        Matrix matrix = new Matrix();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        RectF textureRectF = new RectF(0,0,width,height);
        RectF previewRectF = new RectF(0,0,mPreviewSize.getHeight(),mPreviewSize.getWidth());
        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();
        if(rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
            previewRectF.offset(centerX - previewRectF.centerX(),
                    centerY - previewRectF.centerY() );
            matrix.setRectToRect(textureRectF,previewRectF, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) width / mPreviewSize.getWidth(),
                    (float) height / mPreviewSize.getHeight());
            matrix.postScale(scale,scale,centerX,centerY);
            matrix.postRotate(90 * (rotation - 2),centerX,centerY); // rotate value for 90 or 270
        }
        mTextureView.setTransform(matrix);
    }


    private synchronized void runInBackground(final Runnable r) {
        if (mBackgroundHandler != null) {
            mBackgroundHandler.post(r);
        }
    }
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


    public void onPreviewSizeSelected(int width, int height){
        Log.d(TAG, String.format("onPreviewSizeSelected: width = %d, height= %d", width,height));
        lanePointsView.setSize(new Size(width,height));
        lanePointsView.draw(new Canvas());
        lanePointsView.postInvalidate();
    }
}