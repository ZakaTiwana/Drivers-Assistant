package com.example.fyp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customview.OverlayView;

import java.io.IOException;
import java.util.List;

public class TestObjectDistance extends AppCompatActivity {

    //    private static final int CROP_SIZE = 30;
//    private static final int CROP_SIZE = 224;
    private static final Size DESIZRED_SIZE = new Size(1280,720);
    private static final int CROP_SIZE = 300;
    private static final String TAG = "TestObjectDistance";
    private final int GALLERY_REQUEST_CODE = 100;

    private Bitmap image;
    private Matrix imageToDesired,frameToCrop,cropToFrame;

    private String filepath;
    private String filename;
    private Snackbar initSnackbar;

    private OverlayView draw;

    private Detector detector;
    private List<RecognizedObject> mappedRecognitions;

    private DistanceCalculator distanceCalculator =null;
    private Paint borderBoxPaint,textPaint;

    private RectF location;

    private boolean first_time = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_test_object_distance);

        draw = findViewById(R.id.overlay_distance);

        borderBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderBoxPaint.setColor(Color.RED);
        borderBoxPaint.setStrokeWidth(5);
        borderBoxPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(22);

//        image = Bitmap.createBitmap(DESIZRED_SIZE.getWidth(),DESIZRED_SIZE.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.road_lane_test_3);

        // resize
//        imageToDesired = ImageUtilities.getTransformationMatrix(bitmap.getWidth(),bitmap.getHeight(),
//                DESIZRED_SIZE.getWidth(), DESIZRED_SIZE.getHeight(),0,false);
//
//        frameToCrop = ImageUtilities.getTransformationMatrix(DESIZRED_SIZE.getWidth(),DESIZRED_SIZE.getHeight(),
//                CROP_SIZE, CROP_SIZE,0,false);
//
//        cropToFrame = new Matrix();
//        frameToCrop.invert(cropToFrame);
//
//        Canvas canvas = new Canvas(image);
//        canvas.drawBitmap(bitmap,imageToDesired,null);
        image = ImageUtilities.getResizedBitmap(bitmap,DESIZRED_SIZE.getWidth(),
                DESIZRED_SIZE.getHeight(),true);

        FrameLayout container = findViewById(R.id.container_distance);
        initSnackbar = Snackbar.make(container, "Initializing...", Snackbar.LENGTH_INDEFINITE);
        new Init().execute();

        draw.addCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                canvas.drawBitmap(image,0,0,null);
                if(!first_time) processImage(canvas);
            }
        });
        draw.postInvalidate();
//        first_time = false;

    }


    private void pickFromGallery(){
        //Create an Intent with action as ACTION_PICK

        Intent intent=new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("image/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        // Launching the Intent
        startActivityForResult(intent,GALLERY_REQUEST_CODE);
    }

    private void recognizeImage() {
//        mappedRecognitions = detector.run(image,false);
        Log.d(TAG, "recognizeImage: mappedRecognitions = " + mappedRecognitions.toString());
    }

    private void processImage(Canvas canvas){

            if( mappedRecognitions == null || mappedRecognitions.isEmpty()) {
                return;
            }

            for (RecognizedObject object : mappedRecognitions){

                if (object.getLabel().equalsIgnoreCase("car") ||object.getLabel().equalsIgnoreCase("bottle")) {

                    location = object.getLocation();
                    canvas.drawRect(location, borderBoxPaint);
                    distanceCalculator = new DistanceCalculator();
                    float dist = distanceCalculator.getDistance();
                    canvas.drawText(String.format("%.1f m", dist),location.left,
                            location.top < 10 ? location.top+20:location.top-20,
                            textPaint);

                }
            }

    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data) {
        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST_CODE:
                    Uri selectedImage = data.getData();
                    //data.getData returns the content URI for the selected Image
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    // Get the cursor
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    // Move to first row
                    Bitmap bitmap = null;
                    if(cursor.moveToFirst()){
                        if (Build.VERSION.SDK_INT >= 29) {
                            // now that you have the media URI, you can decode it to a bitmap
                            try (ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(selectedImage, "r")) {
                                if (pfd != null) {
                                    bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                                }
                            } catch (IOException ex) {

                            }
                        } else {
                            //Get the column index of MediaStore.Images.Media.DATA
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            //Gets the String value in the column
                            String filepath = cursor.getString(columnIndex);
                            bitmap = BitmapFactory.decodeFile(filepath);
                        }
                    }
                    if(bitmap == null) return;

                    image = ImageUtilities.getResizedBitmap(bitmap,DESIZRED_SIZE.getWidth(),
                            DESIZRED_SIZE.getHeight(),true);
//                    image = null;
//                    image = Bitmap.createBitmap(DESIZRED_SIZE.getWidth(),DESIZRED_SIZE.getHeight(), Bitmap.Config.ARGB_8888);
//
//                    imageToDesired = ImageUtilities.getTransformationMatrix(bitmap.getWidth(),bitmap.getHeight(),
//                            DESIZRED_SIZE.getWidth(), DESIZRED_SIZE.getHeight(),0,false);
//                    Canvas canvas = new Canvas(image);
//                    canvas.drawBitmap(bitmap,imageToDesired,null);
//
//                    // Set the Image in ImageView after decoding the String
                    mappedRecognitions = null;

                    first_time = true;
                    draw.postInvalidate();
                    recognizeImage();
                    break;
                default:
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            first_time = false;
            recognizeImage();
            draw.postInvalidate();
            return true;
        }
        else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            pickFromGallery();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private class Init extends AsyncTask<Object,Object,Object> {

        @Override
        protected Object doInBackground(Object[] objects) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initSnackbar.show();
                }
            });

            try {
//                 detector= Detector.create(getAssets(), Detector.OBJ_DETECTOR_MODEL);
                Log.d(TAG, "run: detector created");
            } catch (Exception e) {
                Log.e(TAG,"run: Exception initializing classifier!", e);
                return null;
                //                finish();
            }

//            recoganizeImage();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initSnackbar.dismiss();
                }
            });
            return null;
        }
    }

}
