package com.example.fyp.customutilities;

import android.graphics.Path;
import android.graphics.PointF;
import android.util.Size;

import java.util.ArrayList;

public class SharedValues {
    public static final Size[] DESIRED_PREVIEW_SIZES =
            {
                    new Size(640,480),
                    new Size(720,480),
                    new Size(960,720),
                    new Size(1280,720),
                    new Size(1440,1080),
                    new Size(1920,1080),
                    new Size(2048,1152),
                    new Size(3264,1836),
                    new Size(4128,2322)
            };
    public static final Size CROP_SIZE = new Size(300,300);

    public static Path getPathFromPointF(PointF[] pts,boolean loop){
        Path path = new Path();
        path.moveTo(pts[0].x,pts[0].y);
        if (loop){
            for (int i = 1; i < pts.length; i = (i + 1) % pts.length) {
                path.lineTo(pts[i].x,pts[i].y);
                if(i == 0) break;
            }
        }else {
            for (int i = 1; i < pts.length; i++) {
                path.lineTo(pts[i].x,pts[i].y);
            }
        }
        path.close();
        return path;
    }

    public static Path getPathFromPointF(ArrayList<PointF> pts, boolean loop){
        Path path = new Path();
        path.moveTo(pts.get(0).x,pts.get(0).y);
        if (loop){
            for (int i = 1; i < pts.size(); i = (i + 1) % pts.size()) {
                path.lineTo(pts.get(i).x,pts.get(i).y);
                if(i == 0) break;
            }
        }else {
            for (int i = 1; i < pts.size(); i++) {
                path.lineTo(pts.get(i).x,pts.get(i).y);
            }
        }
        path.close();
        return path;
    }

}
