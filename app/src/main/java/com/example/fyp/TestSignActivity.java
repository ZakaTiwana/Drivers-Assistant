package com.example.fyp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class TestSignActivity extends AppCompatActivity {

//    private static final int CROP_SIZE = 30;
    private static final int CROP_SIZE = 224;
    private static final String TAG = "TestSignActivity";
    private final int GALLERY_REQUEST_CODE = 100;

    private boolean initialized = false;

    private Snackbar initSnackbar = null;
    private SignRecoganizor recoganizor = null;

    private Bitmap image;
    private Bitmap copyBitmap;
    private Matrix frameToCrop;
    private Bitmap crropedBitmap;

    private Button btn_recoganize,btn_pick_from_gallery;
    private TextView result;
    private ImageView imageView;

    private String filepath;
    private String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_sign);

        imageView = findViewById(R.id.imageView);
        btn_recoganize = findViewById(R.id.btn_sign_recoganize);
        btn_pick_from_gallery = findViewById(R.id.btn_pick_from_gallery);
        result = findViewById(R.id.sign_result);

        btn_pick_from_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickFromGallery();
            }
        });

        btn_recoganize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processImage();
            }
        });

        image = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.sign_test_2);
        copyBitmap = image.copy(Bitmap.Config.ARGB_8888,true);
        // resize
        frameToCrop = ImageUtilities.getTransformationMatrix(image.getWidth(),image.getHeight(),
                CROP_SIZE, CROP_SIZE,0,false);

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
                    Uri selectedImage = data.getData();
                    //data.getData returns the content URI for the selected Image
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    // Get the cursor
                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    // Move to first row
                    cursor.moveToFirst();
                    //Get the column index of MediaStore.Images.Media.DATA
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    //Gets the String value in the column
                    filepath = cursor.getString(columnIndex);
                    cursor.close();
                    // Set the Image in ImageView after decoding the String
                    imageView.setImageBitmap(BitmapFactory.decodeFile(filepath));
                    image = BitmapFactory.decodeFile(filepath);
                    copyBitmap = image.copy(Bitmap.Config.ARGB_8888,true);
                    // resize
                    frameToCrop = ImageUtilities.getTransformationMatrix(image.getWidth(),image.getHeight(),
                            CROP_SIZE, CROP_SIZE,0,false);
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
                recoganizor = SignRecoganizor.create(getAssets(), CROP_SIZE, CROP_SIZE);
                Log.d(TAG, "run: detector created");
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

            initialized = true;
            return null;
        }
    }
    private void processImage(){
        crropedBitmap = Bitmap.createBitmap(CROP_SIZE,CROP_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(crropedBitmap);
        canvas.drawBitmap(copyBitmap,frameToCrop,null);

        String s = recoganizor.recognizeSign(crropedBitmap);
        result.setText(s);

    }


}
