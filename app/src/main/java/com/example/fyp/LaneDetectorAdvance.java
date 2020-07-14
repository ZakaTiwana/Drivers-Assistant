package com.example.fyp;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;

import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customutilities.SharedPreferencesUtils;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
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

    private Point[] points;
    private Mat mtx;
    private Mat dist;
    private Bitmap chessBoardPattern = null;
    private Bitmap unDistImg;
    private Bitmap edgesBmp;
    private Bitmap warperBmp;
    private Bitmap markedBmp;
    private Mat M_inv;
    private Mat image;

    private static SharedPreferences config;
    private static String mtx_in_sp;
    private static String dist_in_sp;
    private static String has_cal_mtx_dist;

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
        orderedPoints();
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
            SharedPreferencesUtils.saveBool(config,has_cal_mtx_dist,true);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "calibration: could not save "+key);
        }
    }

    public ArrayList<PointF>[] processFrame(Bitmap bmp, boolean visualize){

        // un-distort skipped
//        this.image = unDistortImage(bmp);
        this.image = new Mat();
        Utils.bitmapToMat(bmp,this.image);
//        Point[] p = orderedPoints();

//        Imgproc.drawMarker(this.image,p[0],new Scalar(255,255,255));
//        Imgproc.drawMarker(this.image,p[1],new Scalar(255,255,255));
//
//        Imgproc.drawMarker(this.image,p[2],new Scalar(255,255,255));
//        Imgproc.drawMarker(this.image,p[3],new Scalar(255,255,255));

//        markedBmp = Bitmap.createBitmap(this.image.width(),this.image.height(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(this.image,markedBmp);

        Mat img_binray  = findEdges();
        if(visualize){
            edgesBmp = Bitmap.createBitmap(
                    img_binray.width(),img_binray.height(),
                    Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_binray,edgesBmp);
        }
        Mat img_bird_view = wraper(img_binray);
        if (visualize){
            warperBmp = Bitmap.createBitmap(
                    img_bird_view.width(),img_bird_view.height(),
                    Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_bird_view,warperBmp);
        }

        ArrayList<PointF>[] lanes_points = windowSearch(img_bird_view,visualize);
        ArrayList<PointF> lft_lane_pts = lanes_points[0];
        ArrayList<PointF> rht_lane_pts = lanes_points[1];

        float[] p_lft_float = pointFArrayToFloat(lft_lane_pts);
        float[] p_rht_float = pointFArrayToFloat(rht_lane_pts);


        if (visualize){
            markedBmp = Bitmap.createBitmap(
                    img_bird_view.width(),img_bird_view.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_bird_view,markedBmp);
        }
        //

        //
        Matrix android_M_inv = new Matrix();
        transformMatrix(M_inv,android_M_inv);

        android_M_inv.mapPoints(p_lft_float);
        cropToFrame.mapPoints(p_lft_float);

        android_M_inv.mapPoints(p_rht_float);
        cropToFrame.mapPoints(p_rht_float);

        lft_lane_pts = arrayToPointF(p_lft_float);
        rht_lane_pts = arrayToPointF(p_rht_float);
        polyFit(lft_lane_pts,2);
        polyFit(rht_lane_pts,2);
        ArrayList<PointF>[] res = new ArrayList[2];
        res[0] = lft_lane_pts;
        res[1] = rht_lane_pts;
        return res;
    }
    private void polyFit(ArrayList<PointF> pts, int degree){
        PolynomialCurveFitter
                curveFitter = PolynomialCurveFitter.create(degree)
                .withMaxIterations(500);

        WeightedObservedPoints points = new WeightedObservedPoints();
        for(int i = 0; i < pts.size(); i++) {
            points.add(pts.get(i).x, pts.get(i).y);
        }
        PolynomialFunction func = new PolynomialFunction(curveFitter.fit(points.toList()));
        for(int i = 0; i < pts.size(); i++) {
            pts.get(i).y = (float) func.value(pts.get(i).x);
        }
    }
    private void orderedPoints(){
        points = new Point[4];
        Point[] temp = new Point[4];
        float[] diff = new float[4];
        float[] sums = new float[4];

        Log.d(TAG, "orderedPoints: pts_resized = " + Arrays.toString(pts_resized));
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
        points[0] = temp[min_sum];
        points[1] = temp[min_diff];
        points[2] = temp[max_sum];
        points[3] = temp[max_diff];
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

    private float offCenter(float left, float car , float right) {

        float a = car - left;
        float b = car - right;
        float width = right - left;
        float LANE_WIDTH = 3.7f; // average lane width meter (m)
        float offset = 0;

        if ( Math.abs(a) >= Math.abs(b) ) {
            offset = a / width * LANE_WIDTH - LANE_WIDTH /2f;
        }
        else {
            offset =  b / width * LANE_WIDTH  + LANE_WIDTH /2f;
        }
        return offset;
    }

    private ArrayList<PointF>[] windowSearch(Mat wrapped, boolean visualize){
        int midpoint = wrapped.cols()/2;
        Mat roi = wrapped.submat(wrapped.rows() - wrapped.rows()/4,
                wrapped.rows(),0,wrapped.cols());
        Log.d(TAG, "windowSearch: roi = "+roi.size());
        float[] hist_lane = histogram(roi);
        int left_lane_index = getMaxIndex(hist_lane,0,midpoint-30);
        int right_lane_index = getMaxIndex(hist_lane,midpoint+30,wrapped.cols());
        Log.d(TAG, String.format("windowSearch: hist point lft=%d, rht=%d", left_lane_index,right_lane_index));

        ArrayList<PointF> left_lane_indexes= new ArrayList<>();
        ArrayList<PointF> right_lane_indexes = new ArrayList<>();

        int min_px = 80;
        int windows_n_rows = 30;
        int windows_n_cols = 60;
        // Step of each window
        int StepSlide = 30;
        for (int row = wrapped.rows() ; row > windows_n_rows - StepSlide ; row -= StepSlide) {

            int topX_lft = left_lane_index - windows_n_cols/2;
            int topX_rht = right_lane_index - windows_n_cols/2;
            if(topX_lft < 0) topX_lft = 0;
            if(topX_rht < 0) topX_rht = 0;

            int y_axis = row-windows_n_rows;
            if (y_axis < 0) y_axis = 0;



            Log.d(TAG, String.format("windowSearch: y_axis = %d , topX_lft = %d, topX_rht=%d", y_axis,topX_lft,topX_rht));

            Mat mat_lft = wrapped.submat(y_axis,
                    y_axis+windows_n_rows ,
                    topX_lft, Math.min(topX_lft + windows_n_cols, wrapped.cols()));

            Mat mat_rht = wrapped.submat(y_axis,
                    y_axis+windows_n_rows,
                    topX_rht, Math.min(topX_rht + windows_n_cols, wrapped.cols()));
//            Log.d(TAG, "windowSearch: mat content ="+matContentInString(mat_lft));
//            break;
//
            ArrayList<PointF> nz_pts_lft_lane = nonZerosIndex(mat_lft,topX_lft,y_axis);
            Log.d(TAG, "windowSearch: nz_lft = "+nz_pts_lft_lane.size());
            ArrayList<PointF> nz_pts_rht_lane = nonZerosIndex(mat_rht,topX_rht,y_axis);
            Log.d(TAG, "windowSearch: nz_rht = "+nz_pts_rht_lane.size());

            if (visualize){
                Imgproc.rectangle(wrapped,new Point(topX_lft,y_axis),
                        new Point(topX_lft + windows_n_cols,y_axis + windows_n_rows),
                        new Scalar(255,255,255),
                        2);
                Imgproc.rectangle(wrapped,new Point(topX_rht,y_axis),
                        new Point(topX_rht + windows_n_cols,y_axis + windows_n_rows),
                        new Scalar(255,255,255),
                        2);

                Imgproc.drawMarker(wrapped,new Point(left_lane_index,row - windows_n_rows/2f),new Scalar(255,255,255));
                Imgproc.drawMarker(wrapped,new Point(right_lane_index,row - windows_n_rows/2f),new Scalar(255,255,255));
            }

//            left_lane_indexes.addAll(
//                    nz_pts_lft_lane
//            );
//            right_lane_indexes.addAll(
//                    nz_pts_rht_lane
//            );
            left_lane_indexes.add(new PointF(left_lane_index,row - windows_n_rows/2f));
            right_lane_indexes.add(new PointF(right_lane_index,row - windows_n_rows/2f));
            if(nz_pts_lft_lane.size() >= min_px){
                left_lane_index = meanXIndex(nz_pts_lft_lane);
                Log.d(TAG, "windowSearch: new mean lft = "+ left_lane_index);
            }
            if (nz_pts_rht_lane.size() >= min_px){
                right_lane_index = meanXIndex(nz_pts_rht_lane);
                Log.d(TAG, "windowSearch: new mean rht = "+ right_lane_index);
            }
        }
        ArrayList<PointF>[] res = new ArrayList[2];
        res[0] = left_lane_indexes;
        res[1] = right_lane_indexes;
        return res;
    }

    private int meanXIndex(ArrayList<PointF> pts){
        int mean = 0;
        for (PointF p :
                pts) {
            mean += p.x;
        }
        mean /= pts.size();
        return mean;
    }

    private ArrayList<PointF> nonZerosIndex(Mat mat,int offsetX,int offsetY){
        ArrayList<PointF>  non_zero_points = new ArrayList<>();
        for(int row =  0; row < mat.rows()/2; row++){
            for(int col = 0;  col < mat.cols(); col++) {
                if ((int)mat.get(row,col)[0] > 0d){
                    non_zero_points.add(new PointF(offsetX+col, offsetY+row));
                }

            }
        }
        return non_zero_points;
    }
    private float[] histogram(Mat mat){
        float[] sum_of_y = new float[mat.cols()];
        for (int i = 0; i < mat.cols(); i++) {
            float sum = 0;
            for (int j = 0; j < mat.rows(); j++) {
                sum += (float) mat.get(j,i)[0];
            }
            sum_of_y[i] = sum;
        }
        return sum_of_y;
    }

    private int getMaxIndex(float[] sum_of_y,int offset, int end){
        if(end > sum_of_y.length || end < offset )
            throw new IllegalArgumentException("end should be <= sum_of_y and > offset");
        if(offset < 0 )
            throw new IllegalArgumentException("offset should be >= 0");

        int max = offset;
        for (int i = offset; i < end; i++) {
            if(sum_of_y[i] > sum_of_y[max]) max = i;
        }
        return max;
    }

    private Mat wraper(Mat edge){
        Mat warped = new Mat();

//        Log.d(TAG, "warper: order points = "+ pointArrayToString(points));
        MatOfPoint2f inshape = new MatOfPoint2f(points);
        Point[] p_d = new Point[4];
        p_d[0] = new Point(0,0);
        p_d[1] = new Point(0,edge.rows() -1); //new Point(width - 1,0);

        p_d[2] = new Point(edge.cols() -1 ,edge.rows() - 1);
        p_d[3] = new Point(edge.cols() - 1,0); //new Point(0,height -1);

        MatOfPoint2f outshape = new MatOfPoint2f(p_d);

        Mat M = Imgproc.getPerspectiveTransform(inshape,outshape);
        M_inv = Imgproc.getPerspectiveTransform(outshape,inshape);
//        Log.d(TAG, "warper: M = "+ matContentInString(M));
//        Log.d(TAG, "warper: M_inv = "+ matContentInString(M_inv));

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

    public Mat unDistortImage(Bitmap bmp){
        if (!hasCalibConfigValues()) return null;
        try{
            mtx  = SharedPreferencesUtils.loadMat(config,mtx_in_sp);
            dist = SharedPreferencesUtils.loadMat(config,dist_in_sp);
        } catch (IllegalArgumentException  ex){
            return null;
        }
        Log.d(TAG, "unDistortImage: mtx => "+ matContentInString(mtx));
        Log.d(TAG, "unDistortImage: dist => "+ matContentInString(dist));

        Mat img = new Mat();
        Mat un_img = new Mat();
        Utils.bitmapToMat(bmp,img);
        Imgproc.undistort(img,un_img,mtx,dist);
        unDistImg = Bitmap.createBitmap(
                un_img.width(),un_img.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(un_img,unDistImg);
        img.release();
        return un_img;
    }






    public static void setSharedPreference(SharedPreferences s,
                                           String mtx_in_sp,
                                           String dist_in_sp,
                                           String has_cal_mtx_dist){
        config = s;
        LaneDetectorAdvance.mtx_in_sp = mtx_in_sp;
        LaneDetectorAdvance.dist_in_sp = dist_in_sp;
        LaneDetectorAdvance.has_cal_mtx_dist = has_cal_mtx_dist;
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
                    for (double d :
                            db) {
                        dist_s.append(d).append(", ");
                    }

                }
            }
            dist_s.append("]");
        }
        dist_s.append("]");
        return dist_s.toString();
    }

    static void transformMatrix(Mat src, Matrix dst) {

        int columns = src.cols();
        int rows = src.rows();

        float[] values = new float[columns * rows];
        int index = 0;
        for (int x = 0; x < columns; x++)
            for (int y = 0; y < rows; y++) {
                double[] value = src.get(x, y);
                values[index] = (float) value[0];
                index++;
            }

        dst.setValues(values);
    }

    private static ArrayList<PointF> arrayToPointF(float[] pts){
        ArrayList<PointF> pts_list = new ArrayList<>();
        for (int i = 0; i < pts.length; i+=2) {
            pts_list.add(new PointF(pts[i],pts[i+1]));
        }
        return pts_list;
    }
    private static float[] pointFArrayToFloat(ArrayList<PointF> pts){
        float[] pts_float = new float[pts.size()*2];
        for (int i = 0; i < pts_float.length; i+=2) {
            pts_float[i] = pts.get(i/2).x;
            pts_float[i+1] = pts.get(i/2).y;
        }
        return pts_float;
    }

}
