package com.example.fyp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

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
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customview.OverlayView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TestVideoActivity extends AppCompatActivity {

    private static final String TAG = "TestVideoActivity";
    ImageView imageView;
    OverlayView overlayView;
    final int GALLERY_REQUEST_CODE = 100;
    static int  i = 0;
    Bitmap bmp = null;
    Paint borderBoxPaint;
    final Size desiredSize =  new Size(1280,920);
    final Size originalSize = new Size(1920,1080);
    Matrix desiredSizeMatrix ;
    MediaMetadataRetriever retriever;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_video);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        imageView = findViewById(R.id.img_view_vid);
        overlayView = findViewById(R.id.overlay_video);
        retriever = new MediaMetadataRetriever();

        borderBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderBoxPaint.setColor(Color.RED);
        borderBoxPaint.setStrokeWidth(8);
        borderBoxPaint.setStyle(Paint.Style.STROKE);

        desiredSizeMatrix = ImageUtilities.getTransformationMatrix(
                originalSize.getWidth(),originalSize.getHeight(),
                desiredSize.getWidth(),desiredSize.getHeight(),
                0,true
        );

        addCallBacks();

    }

    private void addCallBacks(){
        overlayView.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(Canvas canvas) {
                       if(bmp !=null) canvas.drawBitmap(bmp,0,0,null);
                    }
                }
        );
    }
    private void pickFromGallery(){
        //Create an Intent with action as ACTION_PICK

        Intent intent=new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("video/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"video/mp4"};
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
                    Bitmap bitmap = null;
                    if(cursor.moveToFirst()){
                        if (Build.VERSION.SDK_INT >= 29) {
                            // now that you have the media URI, you can decode it to a bitmap
                            try (ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(selectedImage, "r")) {
                                if (pfd != null) {
//                                    bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                                    retriever.setDataSource(pfd.getFileDescriptor());
                                }
                            } catch (IOException ex) {

                            }
                        } else {
                            //Get the column index of MediaStore.Images.Media.DATA
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            //Gets the String value in the column
                            String filepath = cursor.getString(columnIndex);
                            retriever.setDataSource(filepath);
//                            bitmap = BitmapFactory.decodeFile(filepath);
                        }
                    }
//                    if(retriever.e == null) return;
                    new ProcessImageTask().execute();
//                    processImage();
                    break;
                default:
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void processImage(){
        try {
            int total_frames = Integer.parseInt(retriever.extractMetadata(retriever.METADATA_KEY_VIDEO_FRAME_COUNT)) ;
            String frame_rate =   retriever.extractMetadata(retriever.METADATA_KEY_CAPTURE_FRAMERATE);
            Log.d(TAG, String.format("processImage: total_frames = %d , frame_rate = %s", total_frames,frame_rate));
//                for (int i =0 ; i < total_frames ; i++ ) {
//                    imageView.setImageBitmap(retriever.getFrameAtIndex(i));
//                    Thread.sleep(33);
//                }
            imageView.setImageBitmap(retriever.getFrameAtIndex(0));
//                retriever.getFrameAtIndex(0);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            pickFromGallery();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        retriever.close();
    }

    @Override
    protected void onPause() {
//        retriever.close();
        super.onPause();

    }

    private class ProcessImageTask extends AsyncTask<Object,Object,Object>{

        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        protected Object doInBackground(Object... objects) {

            final int total_frames = Integer.parseInt(retriever.extractMetadata(retriever.METADATA_KEY_VIDEO_FRAME_COUNT));
            final int total_time_in_ms = Integer.parseInt(
                    retriever.extractMetadata(retriever.METADATA_KEY_DURATION));
            final int factor = 33000; // in microsec
            TimerTask tm = new TimerTask() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "run: here i = "+i);
                        if((i*factor)/1000 >= total_frames) {
                            try {
                                retriever.release();
                            } catch (RuntimeException ex) {
                                ex.printStackTrace();
                            }
                            this.cancel();
                        }
                        if(bmp != null) bmp.recycle();
                        bmp = null;
//                        bmp = retriever.getFrameAtIndex(i);
                        bmp = retriever.getScaledFrameAtTime(factor*i,
                                retriever.OPTION_CLOSEST,
                                desiredSize.getWidth(),desiredSize.getHeight());
//                        overlayView.invalidate();
                        overlayView.postInvalidate();
                        i++;

                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    } catch (RuntimeException ex) {
                        ex.printStackTrace();
                }
            }
            };
            new Timer().schedule(tm,1,1);
            return null;
        }
    }

}