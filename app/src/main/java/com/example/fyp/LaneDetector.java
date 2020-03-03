package com.example.fyp;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import static org.opencv.core.Core.bitwise_and;
import static org.opencv.core.Core.inRange;

public class LaneDetector {
    private static final String TAG = "LaneDetector";

    // HT : Hough Transform (below are variables for houghLinesP)

    // shape for roi
    private static final int[] pts = {650,360, 750,360, 1280,600, 100,600}; // {x1,y1, x2,y2, x3,y3 ,x4,y4}

    private static final int HT_THRESHOLD = 50;
    private static final int HT_MIN_LINE_LENGTH = 50;
    private static final int HT_MAX_LINE_GAP = 200;

    private final Scalar lower_yellow = new Scalar(20,100,100);
    private final Scalar upper_yellow = new Scalar(30,255,255);

    private Mat image;

    public LaneDetector(@NonNull Bitmap image){
        this.image  = new Mat();
        Utils.bitmapToMat(image.copy(Bitmap.Config.ARGB_8888,true),this.image);
    }


//    public LaneDetector(@NonNull Bitmap image, int threshold, int minLineLength, int maxLineGap){
//        this.image = new Mat();
//
//        Utils.bitmapToMat(image.copy(Bitmap.Config.ARGB_8888,true),this.image);
//        HT_THRESHOLD = threshold;
//        HT_MIN_LINE_LENGTH = minLineLength;
//        HT_MAX_LINE_GAP = maxLineGap;
//    }

    public Bitmap getGray(){
        Mat gary_m = convertToGrayScale();
        Bitmap gary = Bitmap.createBitmap(this.image.width(), this.image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(gary_m,gary);
        return gary;
    }

    public Bitmap getEdge(){
        Mat edge_m = edgeDetection(convertToGrayScale());
        Bitmap edge = Bitmap.createBitmap(this.image.width(), this.image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edge_m,edge);
        return edge;
    }


    public Bitmap getRoi() {
//        roiSeperation();
        Mat roi_m = edgeDetection(convertToGrayScale());
        roiSeperation2(roi_m);
        Bitmap roi = Bitmap.createBitmap(this.image.width(), this.image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(roi_m,roi);
        return roi;
    }

    public Bitmap getResult(@Nullable Bitmap baseImage) {
        if(baseImage != null) {
            this.image = null;
            this.image = new Mat();
            Utils.bitmapToMat(baseImage.copy(Bitmap.Config.ARGB_8888,true),this.image);
        }

        Mat edge_m = edgeDetection(convertToGrayScale());
        roiSeperation2(edge_m);
//        Mat result_m = drawLines(edge_m);
        Mat result_m = drawLinesOnImage(edge_m);

        Bitmap result = Bitmap.createBitmap(this.image.width(), this.image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result_m,result);
        return result;
    }

    public Double[][] getResult2() {
        if (this.image.empty())return null;

        Mat edge_m = edgeDetection(convertToGrayScale());
        roiSeperation2(edge_m);
//        Mat result_m = drawLines(edge_m);
        Double[][] points = drawLinesOnImageAndRetrunPoints(edge_m);
        return points;
    }


    public Bitmap getMask(){
        Mat mask = maskShape();
        Bitmap result = Bitmap.createBitmap(mask.width(), mask.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mask,result);
        return result;

    }




    @Deprecated
    private void roiSeperation(){
        this.image.submat(new Rect(0, 0, this.image.width(), this.image.height()/2)).setTo(new Scalar(0));
    }

    private Mat maskShape(){
        Mat mask = new Mat();
        mask.create(new Size(this.image.width(),this.image.height()),CvType.CV_8UC1);
        mask.setTo(new Scalar(0));


        Point p1 = new Point(pts[0],pts[1]);
        Point p2 = new Point(pts[2],pts[3]);

        Point p3 = new Point(pts[4],pts[5]);
        Point p4 = new Point(pts[6],pts[7]);

// test

//        Point p1 = new Point(100,720);
//        Point p2 = new Point(600,420);
//
//        Point p3 = new Point(700,420);
//        Point p4 = new Point(1280,720);

        MatOfPoint shape = new MatOfPoint();
        shape.fromArray(p1,p2,p3,p4);
        Imgproc.fillConvexPoly(mask,shape,new Scalar(255));
        return mask;
    }

    private void roiSeperation2(Mat edge){
        Mat mask = maskShape();
        bitwise_and(edge,mask,edge);
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

    private Mat edgeDetection(Mat gray){
        Mat edge = isolateColor(gray);
        cannay(edge);
        return edge;
    }

    private Mat drawLines(Mat edge){

        Mat lines = new Mat();
        Imgproc.HoughLinesP(edge, lines, 1, Math.PI / 180, HT_THRESHOLD, HT_MIN_LINE_LENGTH, HT_MAX_LINE_GAP);
        Mat result = new Mat();
        result.create(edge.rows(),edge.cols(), CvType.CV_8UC1);

        //Drawing lines on the image

        for (int i = 0; i < lines.rows(); i++) {
//           if(i == 2) return result;
            double[] points = lines.get(i,0);
            double x1, y1, x2, y2;

            x1 = points[0];
            y1 = points[1];
            x2 = points[2];
            y2 = points[3];

            Point pt1 = new Point(x1, y1);
            Point pt2 = new Point(x2, y2);

            //Drawing lines on an image
            Imgproc.line(result, pt1, pt2, new Scalar(255), 2 );
        }
        return result;
    }
//
    private Mat drawLinesOnImage(Mat edge){

        Mat lines = new Mat();
        Imgproc.HoughLinesP(edge, lines, 1, Math.PI / 180, HT_THRESHOLD, HT_MIN_LINE_LENGTH, HT_MAX_LINE_GAP);
        Mat result =this.image.clone();

        //Drawing lines on the image

        for (int i = 0; i < lines.rows(); i++) {
//           if(i == 2) return result;
                double[] points = lines.get(i,0);
                double x1, y1, x2, y2;

                x1 = points[0];
                y1 = points[1];
                x2 = points[2];
                y2 = points[3];

                double deltaX =  (x2 - x1);
                double deltaY =  (y2 - y1);

                double angle =  Math.atan2(deltaY,deltaX);
                angle = angle * 180 / Math.PI;

                Log.d(TAG, String.format("drawLinesOnImage: angle of line %d = %f", i, angle));

                if (Math.abs(angle) > 19 && Math.abs(angle) < 51){
                    Point pt1 = new Point(x1, y1);
                    Point pt2 = new Point(x2, y2);

                    //Drawing lines on an image
                    Imgproc.line(result, pt1, pt2, new Scalar(255,170,0,255), 4 );
                }

//                Point pt1 = new Point(x1, y1);
//                Point pt2 = new Point(x2, y2);
//
//                //Drawing lines on an image
//                Imgproc.line(result, pt1, pt2, new Scalar(255,170,0,255), 4 );
        }
        return result;
    }

    private Double[][] drawLinesOnImageAndRetrunPoints(Mat edge){

        Mat lines = new Mat();
        Imgproc.HoughLinesP(edge, lines, 1, Math.PI / 180, HT_THRESHOLD, HT_MIN_LINE_LENGTH, HT_MAX_LINE_GAP);
        //Drawing lines on the image

        ArrayList<Double> temp = new ArrayList<Double>();
        int line_no = 0;
        for (int i = 0; i < lines.rows(); i++) {
//           if(i == 2) return result;
            double[] points = lines.get(i,0);
            double x1, y1, x2, y2;

            x1 = points[0];
            y1 = points[1];
            x2 = points[2];
            y2 = points[3];
//            boolean place_line = false;
            /// logic
            double deltaX =  (x2 - x1);
            double deltaY =  (y2 - y1);

            double angle =  Math.atan2(deltaY,deltaX);
            angle = angle *180/Math.PI;
//            float angle = fastAtan2(deltaY,deltaX);
            Log.d(TAG, String.format("drawLinesOnImageAndRetrunPoints: angle of line %d = %f", i, angle));
            if (Math.abs(angle) > 19 && Math.abs(angle) < 51) {
                temp.add(x1);
                temp.add(y1);
                temp.add(x2);
                temp.add(y2);
                line_no++;
            }

        }

        Double[][] result = new Double[line_no][4];
        line_no = 0;
        for (int i = 0 ; i<temp.size() ; i+=4){
            result[line_no][0] = temp.get(i);
            result[line_no][1] = temp.get(i+1);
            result[line_no][2] = temp.get(i+2);
            result[line_no][3] = temp.get(i+3);

            line_no++;
        }

        return result;
    }
}
