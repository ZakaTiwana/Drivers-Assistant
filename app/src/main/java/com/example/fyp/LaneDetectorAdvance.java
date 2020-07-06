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
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    public void calibration(int nx, int ny, List<Bitmap> bmps,boolean forceCalibrate) {
        if(bmps == null) return;
        if(hasConfigValues() && !forceCalibrate) return;

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

        for (int i = 0; i < ny; i++) {
            for (int j = 0; j < nx; j++) {
//                objp.put(index,0,new float[]{(float) j,(float) i,0f});
                objp.push_back(new MatOfPoint3f(new Point3(j, i,0.0d)));
            }
        }

//        for (int j=0; j<n_total; j++)
//        {
//            objp.push_back(new MatOfPoint3f(new Point3((double)j/(double)nx, (double)j%(double)ny, 0.0d)));
//        }

        for (Bitmap bitmap :
                bmps) {
            if (bitmap == null) continue;
            Mat img = new Mat();
            Utils.bitmapToMat(bitmap,img);
//            bitmap.recycle();
            Mat gray = new Mat();
            Imgproc.cvtColor(img,gray,Imgproc.COLOR_RGBA2GRAY);
            imgSize = gray.size();
            MatOfPoint2f corners = new MatOfPoint2f();
            boolean gotChessBoardCorners = Calib3d.findChessboardCorners(
                    gray,
                    boardSize,
                    corners,Calib3d.CALIB_CB_ADAPTIVE_THRESH |
                            Calib3d.CALIB_CB_FILTER_QUADS | Calib3d.CALIB_CB_FAST_CHECK);
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
        Log.d(TAG, String.format("calibration: mtx : width= %d , height= %d", mtx.width(),mtx.height()));
        Log.d(TAG, String.format("calibration: dist : width= %d , height= %d", dist.width(),dist.height()));

        double[][] mtx_db = new double[mtx.width()][mtx.height()];
        double[][] dist_db = new double[dist.width()][dist.height()];

        for (int i = 0; i < mtx.width(); i++) {
            for (int j = 0; j < mtx.height(); j++) {
                mtx_db[i][j] = mtx.get(i, j)[0];
            }
        }
        Log.d(TAG, "calibration: mtx content  => " + matContent(mtx));


        for (int i = 0; i < dist.width(); i++) {
            for (int j = 0; j < dist.height(); j++) {
                double[] db = dist.get(i, j);
                if (db != null){
                    dist_db[i][j] = db[0];
                }
            }
        }
        Log.d(TAG, "calibration: dist content  => " + matContent(dist));


        saveConfig(mtx_db, dist_db);
    }

    public void unDistortImage(Bitmap bmp){
        if (!hasConfigValues()) return;
//        mtx = loadMtx();
//        dist = loadDist();
        mtx  = loadMtx();
        dist = loadDist();
        Log.d(TAG, "unDistortImage: mtx => "+matContent(mtx));
        Log.d(TAG, "unDistortImage: dist => "+matContent(dist));

        Mat img = new Mat();
        Mat un_img = new Mat();
        Utils.bitmapToMat(bmp,img);
        bmp.recycle();
        Imgproc.undistort(img,un_img,mtx,dist);
        unDistImg = Bitmap.createBitmap(
                un_img.width(),un_img.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(un_img,unDistImg);
        img.release();
    }


    private static void saveConfig(double[][] mtx,double[][] dist){
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

    private static Mat loadMtx(){
        Gson gson = new Gson();
        Double[][] mtx = new Double[3][];
        String json = config.getString(mtx_in_sp, "");
        if (json != null && !json.isEmpty()){
            mtx = gson.fromJson(json,Double[][].class);
        }
        Mat mtx_mat = new Mat(mtx[0].length,mtx.length,CvType.CV_32F);
        for (int i = 0; i < mtx_mat.width(); i++) {
            for (int j = 0; j < mtx_mat.height(); j++) {
                mtx_mat.put(i,j,mtx[i][j]);
            }
        }
        Log.d(TAG, "loadMtx: loaded mtx from share preference");
        Log.d(TAG, "loadMtx: mtx = "+json);
        return mtx_mat;
    }
    private static Mat loadDist(){
        Gson gson = new Gson();
        Double[][] dist = new Double[5][];
        String json = config.getString(dist_in_sp, "");
        if (json != null && !json.isEmpty()){
            dist = gson.fromJson(json,Double[][].class);
        }
        Mat dist_mat = new Mat(dist[0].length,dist.length,CvType.CV_32F);
        for (int i = 0; i < dist_mat.width(); i++) {
            for (int j = 0; j < dist_mat.height(); j++) {
                dist_mat.put(i,j,dist[i][j]);
            }
        }
        Log.d(TAG, "loadMtx: loaded dist from share preference");
        Log.d(TAG, "loadMtx: dist = "+json);
        return dist_mat;
    }

    public static void setSharedPreference(SharedPreferences s){
        config = s;
    };

    private static boolean hasConfigValues(){
        boolean check = false;
        String mtx = config.getString(mtx_in_sp, "");
        String dist = config.getString(mtx_in_sp, "");
        if ((mtx != null && dist != null)
                && !mtx.isEmpty() && !dist.isEmpty())
            check = true;
        return check;
    }

    public Bitmap getChessBoardPattern() {
        return chessBoardPattern;
    }

    public Bitmap getUnDistImg() {
        return unDistImg;
    }

    private String matContent(Mat dist){
        StringBuilder dist_s = new StringBuilder("[");
        for (int i = 0; i < dist.width(); i++) {
            dist_s.append("[");
            for (int j = 0; j < dist.height(); j++) {
                double[] db = dist.get(i, j);
                if (db != null){
                    dist_s.append(db[0]).append(", ");
                }
            }
            dist_s.append("]");
        }
        dist_s.append("]");
        return dist_s.toString();
    }
}
