package com.example.fyp;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.gson.Gson;

import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LaneDetectorAdvance {
    private static final String TAG = "LaneDetectorAdvance";
    private Mat mtx;
    private Mat dist;
    private boolean gotChessBoardImg = false;
    private Bitmap chessBoardPattern;
    private Bitmap unDistImg;

    private static SharedPreferences config;
    private static final String mtx_in_sp = "mtx";
    private static final String dist_in_sp = "dist";

    public LaneDetectorAdvance( ) {  }

    public void calibration(int nx, int ny, List<Bitmap> bmps) {
        if(bmps == null) return;

//        Mat objp = new Mat(nx*ny,3,CvType.CV_32F);
        List<Mat> obj_ps = new ArrayList<>();
        List<Mat> img_ps = new ArrayList<>();
        Size imgSize = new Size();
//        int index= 0;
//        for (int i = 0; i < ny; i++) {
//            for (int j = 0; j < nx; j++) {
//                objp.put(index,0,new float[]{(float) j,(float) i,0f});
//                index++;
//            }
//        }

        MatOfPoint3f objp = new MatOfPoint3f();
        int n_total = nx*ny;
        Size boardSize = new Size (nx,ny);
        for (int j=0; j<n_total; j++)
        {
            objp.push_back(new MatOfPoint3f(new Point3((double)j/(double)nx, (double)j%(double)ny, 0.0d)));
        }

        for (Bitmap bitmap :
                bmps) {
            if (bitmap == null) continue;
            Mat img = new Mat();
            Utils.bitmapToMat(bitmap,img);
            bitmap.recycle();
            Mat gray = new Mat();
            Imgproc.cvtColor(img,gray,Imgproc.COLOR_RGBA2GRAY);
            imgSize = gray.size();
            MatOfPoint2f corners = new MatOfPoint2f();
            boolean gotChessBoardCorners = Calib3d.findChessboardCorners(
                    gray,
                    boardSize,
                    corners,Calib3d.CALIB_CB_ADAPTIVE_THRESH | Calib3d.CALIB_CB_FILTER_QUADS);
            if (gotChessBoardCorners){
                Log.d(TAG, "calibration: detected chess board");
                TermCriteria aCriteria = new TermCriteria(TermCriteria.EPS |
                        TermCriteria.MAX_ITER, 30,0.1);
                Imgproc.cornerSubPix(gray, corners, new Size(11,11), new Size(-1,-1), aCriteria);
                if (!gotChessBoardImg){
                    Calib3d.drawChessboardCorners(img,boardSize,corners, true);
                    gotChessBoardImg = true;
                    if (gotChessBoardImg) {
                        chessBoardPattern = Bitmap.createBitmap(
                                img.width(),img.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(img,chessBoardPattern);
                    }
                }
                obj_ps.add(objp);
                img_ps.add(corners);
            }
            img.release();
            gray.release();
        }
        if (obj_ps.isEmpty() || img_ps.isEmpty()){
            Log.d(TAG, "calibration: did not find chessboard");
            return;
        }
        mtx = new Mat();
        dist = new Mat();
        Calib3d.calibrateCamera(obj_ps,img_ps,imgSize,mtx,dist,
                new ArrayList<Mat>(),new ArrayList<Mat>());
        saveConfig(mtx, dist);
    }

    public void unDistortImage(Bitmap bmp){
        if (!hasConfigValues()) return;
        loadConfig(mtx,dist);
        Mat img = new Mat();
        Mat un_img = new Mat();
        Utils.bitmapToMat(bmp,img);
        bmp.recycle();
        Imgproc.undistort(img,un_img,mtx,dist);
        Utils.matToBitmap(un_img,unDistImg);
        img.release();
    }


    private static void saveConfig(Mat mtx,Mat dist){
        SharedPreferences.Editor prefsEditor = config.edit();
        Gson gson = new Gson();

        String json = gson.toJson(mtx);
        prefsEditor.putString(mtx_in_sp, json);
        prefsEditor.apply();

        json = gson.toJson(dist);
        prefsEditor.putString(dist_in_sp, json);
        prefsEditor.apply();
        Log.d(TAG, "saveConfig: config saved in shared preference");
    }

    private static void loadConfig(Mat mtx, Mat dist){
        Gson gson = new Gson();
        String json = config.getString(mtx_in_sp, "");
        if (json != null && !json.isEmpty()){
            mtx = gson.fromJson(json, Mat.class);
        }
        json = config.getString(dist_in_sp, "");
        if (json != null && !json.isEmpty()){
            dist = gson.fromJson(json, Mat.class);
        }
        Log.d(TAG, "loadConfig: loaded from share preference");
    }

    public static void setSharedPreference(SharedPreferences s){
        config = s;
    };

    private static boolean hasConfigValues(){
        boolean check = true;
        String mtx = config.getString(mtx_in_sp, "");
        String dist = config.getString(mtx_in_sp, "");
        assert mtx != null;
        assert dist != null;
        if (mtx.isEmpty() || dist.isEmpty()){
            check = false;
        }
        return check;
    }

    public Bitmap getChessBoardPattern() {
        return chessBoardPattern;
    }

    public Bitmap getUnDistImg() {
        return unDistImg;
    }
}
