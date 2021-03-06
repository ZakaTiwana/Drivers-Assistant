package com.example.fyp.customutilities;

import android.graphics.Path;
import android.graphics.PointF;
import android.util.Size;

import java.util.ArrayList;

public class SharedValues {
    public volatile static boolean isBlueToothConnected = false;
    public static final Size[] DESIRED_PREVIEW_SIZES =
            {
                    new Size(640,480),
                    new Size(720,480),
                    new Size(960,720),
                    new Size(1280,720),
                    new Size(1440,1080),
                    new Size(1920,1080),
                    new Size(2288,1080),
                    new Size(2048,1152),
                    new Size(2560,1440),
                    new Size(3264,1836),
                    new Size(4128,2322)
            };
    public static final Size CROP_SIZE = new Size(300,300);

    public static final String intent_to_assistant_mode = "lane_points_img_processor";
    public static final String intent_step_info = "directions_steps_info";
    public static final String intent_to_nav_mode = "from_direction";
    public static final String intent_dest_latitude = "dest_lat";
    public static final String intent_dest_longitude = "dest_lng";

    public static Path getPathFromPointF(PointF[] pts,boolean loop){
        Path path = new Path();
        path.moveTo(pts[0].x,pts[0].y);
        for (PointF pt : pts) {
            path.lineTo(pt.x, pt.y);
        }
        if(loop) path.close();

        return path;
    }

    public static Path getPathFromPointF(ArrayList<PointF> pts, boolean loop){
        Path path = new Path();
        path.moveTo(pts.get(0).x,pts.get(0).y);
        for (PointF pt : pts) {
            path.lineTo(pt.x, pt.y);
        }
        if(loop) path.close();
        return path;
    }

}
