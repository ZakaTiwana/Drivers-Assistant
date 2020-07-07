package com.example.fyp;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.Calib3d;

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
    public static final String mtx_in_sp = "mtx";
    public static final String dist_in_sp = "dist";

    public LaneDetectorAdvance( ) {  }

    public void calibration(int nx, int ny, List<Bitmap> bmps,boolean forceCalibrate) {
        if(bmps == null) return;
        if(hasConfigValues() && !forceCalibrate) return;
        List<Mat> obj_ps = new ArrayList<>();
        List<Mat> img_ps = new ArrayList<>();
        Size imgSize = new Size();

        MatOfPoint3f objp = new MatOfPoint3f();
        Size boardSize = new Size (nx,ny);

        for (int i = 0; i < ny; i++) {
            for (int j = 0; j < nx; j++) {
                objp.push_back(new MatOfPoint3f(new Point3(j, i,0.0d)));
            }
        }

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
                    chessBoardPattern = Bitmap.createBitmap(
                            img.width(),img.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(img,chessBoardPattern);

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



        Log.d(TAG, "calibration: mtx content  => " + matContentInString(mtx));
        Log.d(TAG, "calibration: dist content  => " + matContentInString(dist));
        try {
            saveConfig(mtx, mtx_in_sp);
            saveConfig(dist,dist_in_sp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unDistortImage(Bitmap bmp){
        if (!hasConfigValues()) return;
        mtx = loadConfig(mtx_in_sp);
        dist = loadConfig(dist_in_sp);
        Log.d(TAG, "unDistortImage: mtx => "+ matContentInString(mtx));
        Log.d(TAG, "unDistortImage: dist => "+ matContentInString(dist));

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

    private static void saveConfig(Mat mat,String key) throws IllegalArgumentException {
        SharedPreferences.Editor prefsEditor = config.edit();

        String json = matToJson(mat);
        if (json.contentEquals("{}")) throw new IllegalArgumentException("could not "+key+" convert to json");
        prefsEditor.putString(key, json);
        prefsEditor.apply();
        Log.d(TAG, "saveConfig: config ("+key+") saved in shared preference");
    }

    public static void saveConfigByString(String key, String json){
        SharedPreferences.Editor prefsEditor = config.edit();
        prefsEditor.putString(key, json);
        prefsEditor.apply();
        Log.d(TAG, "saveConfig: config ("+key+") saved in shared preference");
    }

    private static Mat loadConfig(String key){
        String json = config.getString(key, "");
        Mat mat;
        if (json != null && !json.isEmpty()){
            Log.d(TAG, "loadMtx: "+key+" => "+json);
             mat = matFromJson(json);
        }else return null;
        Log.d(TAG, "loadMtx: loaded "+key+" from share preference");

        return mat;
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

    private String matContentInString(Mat dist){
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

    private static String matToJson(Mat mat){
        JsonObject obj = new JsonObject();

        if(mat.isContinuous()){
            int cols = mat.cols();
            int rows = mat.rows();

            JsonArray data = new JsonArray();
            for (int i = 0; i < rows*cols; i++) {
                if (mat.get(i/rows,i%cols) !=null) data.add(mat.get(i/rows,i%cols)[0]);
            }
            obj.addProperty("rows", mat.rows());
            obj.addProperty("cols", mat.cols());
            obj.addProperty("type", mat.type());

            obj.add("data",data);

            Gson gson = new Gson();
            return gson.toJson(obj);
        } else {
            Log.e(TAG, "Mat not continuous.");
        }
        return "{}";
    }

    private static Mat matFromJson(String json){
        JsonParser parser = new JsonParser();
        JsonObject JsonObject = parser.parse(json).getAsJsonObject();

        int rows = JsonObject.get("rows").getAsInt();
        int cols = JsonObject.get("cols").getAsInt();
        int type = JsonObject.get("type").getAsInt();

        JsonArray data = JsonObject.get("data").getAsJsonArray();
        Mat mat = new Mat(rows, cols, type);

        for (int i = 0; i < rows*cols; i++) {
            try{
                mat.put(i/rows, i%cols, data.get(i).getAsDouble());
            }catch (Exception ignored){}
        }
        return mat;
    }

}
