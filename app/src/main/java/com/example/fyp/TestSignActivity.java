package com.example.fyp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.example.fyp.customutilities.ImageUtilities;

import java.io.IOException;
import java.util.List;

public class TestSignActivity extends AppCompatActivity {

//    private static final int CROP_SIZE = 30;

    private static final String TAG = "TestSignActivity";
    private final int GALLERY_REQUEST_CODE = 100;

    private Snackbar initSnackbar = null;
    private SignDetector recoganizor = null;

    private Bitmap image;
    private Bitmap copyBitmap;
    private Paint borderBoxPaint;
    private Paint textPaint;

    private TextView result;
    private ImageView imageView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_sign);

        imageView = findViewById(R.id.imageView);
        Button btn_recoganize = findViewById(R.id.btn_sign_recoganize);
        Button btn_pick_from_gallery = findViewById(R.id.btn_pick_from_gallery);
        Button btn_test_classify_sign = findViewById(R.id.btn_test_classify_sign);
        result = findViewById(R.id.sign_result);

        borderBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderBoxPaint.setColor(Color.RED);
        borderBoxPaint.setStrokeWidth(8);
        borderBoxPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(23);

        btn_pick_from_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickFromGallery();
            }
        });

        btn_recoganize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectAndClassifyImage();
            }
        });
        btn_test_classify_sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                classifyImage();
            }
        });

        image = ImageUtilities.getResizedBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.sign_test_3),300,300,true);
        copyBitmap = image.copy(Bitmap.Config.ARGB_8888,true);

        imageView.setImageResource(R.drawable.sign_test_3);

        FrameLayout container = (FrameLayout) findViewById(R.id.container2);
        initSnackbar = Snackbar.make(container, "Initializing...", Snackbar.LENGTH_INDEFINITE);
        new Init().execute();

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

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data) {
        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST_CODE:
                    Bitmap bmp = null;
                    Uri selectedImage = data.getData();
                    //data.getData returns the content URI for the selected Image
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    // Get the cursor
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    // Move to first row
                    if(cursor.moveToFirst()){
                        if (Build.VERSION.SDK_INT >= 29) {
                            // now that you have the media URI, you can decode it to a bitmap
                            try (ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(selectedImage, "r")) {
                                if (pfd != null) {
                                    bmp = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                                }
                            } catch (IOException ex) {

                            }
                        } else {
                            //Get the column index of MediaStore.Images.Media.DATA
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            //Gets the String value in the column
                            String filepath = cursor.getString(columnIndex);
                            bmp = BitmapFactory.decodeFile(filepath);
                        }
                    }
                    cursor.close();
                    // Set the Image in ImageView after decoding the String
                    if (bmp == null) break;
                    image = ImageUtilities.getResizedBitmap(bmp,300,300,true);
                    imageView.setImageBitmap(image);
                    copyBitmap = image.copy(Bitmap.Config.ARGB_8888,true);
                    break;
                default:
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);


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
                float start = SystemClock.currentThreadTimeMillis();
                recoganizor = SignDetector.create(getAssets(),300,300);
                float end = SystemClock.currentThreadTimeMillis();
                Log.d(TAG, "run: detector created in "+(end-start)+"ms");
            } catch (Exception e) {
                Log.e(TAG,"run: Exception initializing classifier!", e);
                return null;
                //                finish();
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initSnackbar.dismiss();
                }
            });

            boolean initialized = true;
            return null;
        }
    }
    private void classifyImage(){
        float start = SystemClock.currentThreadTimeMillis();
        String to_show = recoganizor.runOnlyClassification(copyBitmap.copy(Bitmap.Config.ARGB_8888,true),true);
        float end = SystemClock.currentThreadTimeMillis();
        Log.d(TAG, "processImage: (classify) time take to classify sign : "+ (end-start) +" ms");
        result.setText(to_show);
    }
    private void detectAndClassifyImage(){
        float start = SystemClock.currentThreadTimeMillis();
        List<RecognizedObject> recs = recoganizor.run(copyBitmap.copy(Bitmap.Config.ARGB_8888,true),true);
        float end = SystemClock.currentThreadTimeMillis();
        Log.d(TAG, "processImage: (detectAndClassify) time take to detect sign : "+ (end-start) +" ms");
        String to_show="";
        Canvas canvas1 = new Canvas(copyBitmap);
        int index = 0;
        for (RecognizedObject rc :
                recs) {
            RectF location = rc.getLocation();
            ImageUtilities.createCustomFile(getApplicationContext(),
                    ImageUtilities.getCropBitmapByCPU(copyBitmap,location,false),"test_"+index+".png");
            canvas1.drawRect(location,borderBoxPaint);
            canvas1.drawText(
                    String.format("%s , %.1f %%", rc.getLabel(), rc.getScore() * 100),
                    location.left, location.top < 10 ? location.top + 20 : location.top - 10, textPaint);
            to_show += rc.getLabel()+" ; ";
        }
        result.setText(to_show);
        imageView.setImageBitmap(copyBitmap);
        if(image != null && !image.isRecycled()) image.recycle();
    }


}
