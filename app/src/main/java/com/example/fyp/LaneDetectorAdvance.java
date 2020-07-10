package com.example.fyp;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;

import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customutilities.SharedPreferencesUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.calib3d.Calib3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.Core.inRange;
import static org.opencv.core.Core.max;

public class LaneDetectorAdvance {
    private static final String TAG = "LaneDetectorAdvance";
    private final Scalar lower_yellow = new Scalar(20,100,100);
    private final Scalar upper_yellow = new Scalar(30,255,255);
    private PointF[] pts_resized ; //roi
    private Matrix cropToFrame;

    private Mat mtx;
    private Mat dist;
    private Bitmap chessBoardPattern;
    private Bitmap unDistImg;
    private Bitmap edgesBmp;
    private Bitmap warperBmp;
    private Bitmap markedBmp;
    private Mat M_inv;
    private Mat image;

    private static SharedPreferences config;
    public static final String mtx_in_sp = "mtx";
    public static final String dist_in_sp = "dist";

    public LaneDetectorAdvance(int srcWidth, int srcHeight, int cropWidth, int cropHeight) {
        Matrix frameToCrop = ImageUtilities.getTransformationMatrix(
                srcWidth,srcHeight,
                cropWidth,cropHeight,
                0,false
        );

        cropToFrame = new Matrix();
        frameToCrop.invert(cropToFrame);
    }

    public void setPtsResized (PointF[] pts){
        pts_resized = pts;
    }

    public void setNewSize(int srcWidth, int srcHeight, int cropWidth, int cropHeight){
        Matrix frameToCrop = ImageUtilities.getTransformationMatrix(
                srcWidth,srcHeight,
                cropWidth,cropHeight,
                0,false
        );

        cropToFrame = new Matrix();
        frameToCrop.invert(cropToFrame);
    }

    public void calibration(int nx, int ny, List<Bitmap> bmps,boolean forceCalibrate) {
        if(bmps == null || config == null) return;
        if(hasCalibConfigValues() && !forceCalibrate) return;
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

        boolean gotChessBoardImg = false;
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
                    gotChessBoardImg = true;
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
        String key="";
        try {
            key = mtx_in_sp;
            if(SharedPreferencesUtils.saveMat(config,mtx_in_sp,mtx))
                Log.d(TAG, "calibration: could not save "+mtx_in_sp);
            key = dist_in_sp;
            if(SharedPreferencesUtils.saveMat(config,dist_in_sp,dist))
                Log.d(TAG, "calibration: could not save "+dist_in_sp);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "calibration: could not save "+key);
        }
    }

    public void processFrame(Bitmap bmp){
        this.image = new Mat();
        Utils.bitmapToMat(bmp,this.image);
        // un-distort skipped
        Point[] p = orderedPoints();

        Imgproc.drawMarker(this.image,p[0],new Scalar(255,255,255));
        Imgproc.drawMarker(this.image,p[1],new Scalar(255,255,255));

        Imgproc.drawMarker(this.image,p[2],new Scalar(255,255,255));
        Imgproc.drawMarker(this.image,p[3],new Scalar(255,255,255));

        markedBmp = Bitmap.createBitmap(this.image.width(),this.image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(this.image,markedBmp);

        Mat img_binray  = findEdges();
        edgesBmp = Bitmap.createBitmap(
                img_binray.width(),img_binray.height(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_binray,edgesBmp);
        Mat img_bird_view = warper(img_binray);
        warperBmp = Bitmap.createBitmap(
                img_bird_view.width(),img_bird_view.height(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_bird_view,warperBmp);

    }

    private Point[] orderedPoints(){
        Point[] rect = new Point[4];
        Point[] temp = new Point[4];
        float[] diff = new float[4];
        float[] sums = new float[4];

        Log.d(TAG, "orderedPoints: pts_resized = "+Arrays.toString(pts_resized));
        int index = 0;
        for (int i = 0; i < pts_resized.length; i++) {
            temp[index] = new Point(pts_resized[i].x,pts_resized[i].y);
            sums[index] = pts_resized[i].x + pts_resized[i].y;
            diff[index] = pts_resized[i].x - pts_resized[i].y;
            index++;
        }
        Log.d(TAG, "orderedPoints: sums = "+Arrays.toString(sums));
        Log.d(TAG, "orderedPoints: diff = "+Arrays.toString(diff));
        int max_sum, min_sum, max_diff, min_diff;
        max_sum=0;max_diff=0;min_diff=0;min_sum=0;

        for (int i = 1; i <sums.length; i++) {
            if(sums[i] > sums[max_sum]) max_sum=i;
            if(sums[i] < sums[min_sum]) min_sum=i;
            if(diff[i] > diff[max_diff]) max_diff=i;
            if(diff[i] < diff[min_diff]) min_diff=i;
        }
        Log.d(TAG, String.format(
                "orderedPoints: min_sum = %d , min_diff = %d, max_sum = %d, max_diff = %d",
                min_sum,min_diff,max_sum,max_diff));
        rect[0] = temp[min_sum];
        rect[1] = temp[min_diff];
        rect[2] = temp[max_sum];
        rect[3] = temp[max_diff];

        return rect;
    }

    private String pointArrayToString(Point[] ps){
        StringBuilder s = new StringBuilder("[");
        for (Point p :
                ps) {
            s.append("(").append(p.x).append(",").append(p.y).append(") ");
        }
        s.append("]");
        return s.toString();
    }
    private Mat warper(Mat edge){
        Mat warped = new Mat();

        Point[] p = orderedPoints();

        Log.d(TAG, "warper: order points = "+ pointArrayToString(p));
        MatOfPoint2f inshape = new MatOfPoint2f();
        inshape.fromArray(p);

        // calculate destination mat width and height
        int width_a = (int) Math.sqrt(Math.pow((p[2].x - p[3].x),2)
                        + Math.pow((p[2].y - p[3].y),2));
        int width_b = (int) Math.sqrt(Math.pow((p[0].x - p[1].x),2)
                        + Math.pow((p[0].y - p[1].y),2));
        int max_width = Math.max(width_a,width_b);

        int height_a = (int) Math.sqrt(Math.pow((p[0].x - p[3].x),2)
                + Math.pow((p[0].y - p[3].y),2));
        int height_b = (int) Math.sqrt(Math.pow((p[1].x - p[2].x),2)
                + Math.pow((p[1].y - p[2].y),2));
        int max_height = Math.max(height_a,height_b);


        Point p1_d = new Point(0,0);
        Point p2_d = new Point(max_width-1,0);

        Point p3_d = new Point(max_width-1,max_height-1);
        Point p4_d = new Point(0,max_height-1);

        MatOfPoint2f outshape = new MatOfPoint2f();
        outshape.fromArray(p1_d,p2_d,p3_d,p4_d);

        Log.d(TAG, "warper : type inshape = "+ inshape.type());

        Mat M = Imgproc.getPerspectiveTransform(inshape,outshape);
        M_inv = Imgproc.getPerspectiveTransform(outshape,inshape);

        Imgproc.warpPerspective(edge,warped,M,
                edge.size());
        return warped;
    }
    private Mat findEdges(){
        Mat edge = isolateColor(convertToGrayScale());
        cannay(edge);
        return edge;
    }

    private Mat convertToGrayScale(){
        Mat gray = new Mat();
        Imgproc.cvtColor(image,gray,Imgproc.COLOR_RGBA2GRAY);
        return gray;
    }


    private Mat isolateColor(Mat gray){
        Mat hsv = new Mat();
        Imgproc.cvtColor(this.image,hsv,Imgproc.COLOR_BGR2HSV);

        Mat mask_w = new Mat();
        Mat mask_y = new Mat();
        Mat mask_yw = new Mat();
        Mat temp = new Mat();
        Mat edge = new Mat();

        inRange(gray,new Scalar(190),new Scalar(255),mask_w);
        inRange(hsv,lower_yellow,upper_yellow,mask_y);
        Core.bitwise_or(mask_w,mask_y,mask_yw);

        Core.bitwise_and(gray,mask_yw,temp);
        Imgproc.GaussianBlur(temp,edge,new Size(5,5),0);
        return edge;
    }

    private void cannay(Mat edge){
        Imgproc.Canny(edge,edge,50,150);
    }

    public void unDistortImage(Bitmap bmp){
        if (!hasCalibConfigValues()) return;
        try{
            mtx  = SharedPreferencesUtils.loadMat(config,mtx_in_sp);
            dist = SharedPreferencesUtils.loadMat(config,mtx_in_sp);
        } catch (IllegalArgumentException  ex){
            return;
        }
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






    public static void setSharedPreference(SharedPreferences s){
        config = s;
    }

    private static boolean hasCalibConfigValues(){
        try{
            SharedPreferencesUtils.loadMat(config,mtx_in_sp);
            SharedPreferencesUtils.loadMat(config,dist_in_sp);
        }catch (IllegalArgumentException ex){
            return false;
        }
        return true;
    }

    public Bitmap getChessBoardPattern() {
        return chessBoardPattern;
    }

    public Bitmap getUnDistImg() {
        return unDistImg;
    }

    public Bitmap getEdgesBmp() {
        return edgesBmp;
    }

    public Bitmap getWarperBmp() {
        return warperBmp;
    }

    public Bitmap getMarkedBmp() {
        return markedBmp;
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
