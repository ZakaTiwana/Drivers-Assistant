package com.example.fyp.customview;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.fyp.LaneDetectorAdvance;
import com.example.fyp.customutilities.ImageUtilities;
import com.example.fyp.customutilities.SharedPreferencesUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;


public class LanePointsView extends View {
    private static final String TAG = "LanePointsView";
    private Paint maskFillPaint = null;
    private Paint maskBorderPaint = null;
    private Paint pointPaint = null;
    private Paint onClickPaint = null;

    private boolean hasClicked = false;
    private float cx ;
    private float cy ;
    private static final float onClicked_radius = 50;
    private static final float nearPoint_radius= 40;

    private PointF[] pts = null;

    private Path pointCirclesPath = null;
    private Path maskPath = null;

    private Size viewSize = null;
    private int point_to_mov = 0;
    private boolean gotPointToMov = false;

    private Context context;

    public LanePointsView(Context context) {
        super(context);
        init(context);
    }

    public LanePointsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);

    }

    public LanePointsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void init(Context context) {
        this.context = context;

        onClickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        onClickPaint.setColor(Color.WHITE);
        onClickPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        onClickPaint.setAlpha(100);

        maskBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskBorderPaint.setColor(Color.DKGRAY);
        maskBorderPaint.setStyle(Paint.Style.STROKE);
        maskBorderPaint.setStrokeWidth(10);

        maskFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskFillPaint.setColor(Color.BLACK);
        maskFillPaint.setStyle(Paint.Style.FILL);
        maskFillPaint.setAlpha(130);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.LTGRAY);
        pointPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        float centerX = getWidth()/2f;  // landscape mode.
        float centerY = getHeight()/2f; //
        viewSize = new Size(getWidth() , getHeight());

        pts = new PointF[4];
        pts[0] = new PointF( centerX - 100, centerY - 100 );
        pts[1] = new PointF( centerX - 100, centerY + 100 );
        pts[2] = new PointF( centerX + 100, centerY + 100 );
        pts[3] = new PointF( centerX + 100, centerY - 100 );

        maskPath  = new Path();
        setMaskPath();
        pointCirclesPath = new Path();
        setCirclePointPath();
    }
    public void setSize(Size size){
        viewSize = size;
        float centerX = size.getWidth()/2f;  // landscape mode.
        float centerY = size.getHeight()/2f; //
        Log.d(TAG, String.format("setSize: centerY = %f, CenterX = %f", centerY,centerX));

        pts = new PointF[4];
        pts[0] = new PointF( centerX - 100, centerY - 100 );
        pts[1] = new PointF( centerX - 100, centerY + 100 );
        pts[2] = new PointF( centerX + 100, centerY + 100 );
        pts[3] = new PointF( centerX + 100, centerY - 100 );
        setMaskPath();
        setCirclePointPath();
    }

    private void setCirclePointPath(){
        if (pointCirclesPath == null) return;
        pointCirclesPath.reset();
        for (int i = 0; i < pts.length; i++) {
            pointCirclesPath.addCircle(pts[i].x,pts[i].y,20f, Path.Direction.CCW);
        }
    }

    private void setMaskPath(){
        if(maskPath == null) return;
        maskPath.reset();
        maskPath.moveTo(pts[0].x,pts[0].y);
        for (int i = 1; i < pts.length; i = (i + 1) % pts.length) {
            maskPath.lineTo(pts[i].x,pts[i].y);
            if(i == 0) break;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.performClick();

        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch(action) {
            case MotionEvent.ACTION_DOWN : {
                hasClicked = true;
                // check if with in reach for
                // circle
                cx = event.getX();
                cy = event.getY();
                int center_x = (int) cx;
                int center_y = (int) cy;
                int radius_pow_2 = (int) Math.pow(nearPoint_radius,2);
                for (int i=0; i< pts.length; i++) {
                   if (  Math.pow(pts[i].x - center_x,2) + Math.pow(pts[i].y - center_y,2) < radius_pow_2){
                       point_to_mov = i;
                       gotPointToMov = true;
                       break;
                   }
                   gotPointToMov = false;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE : {
                // change the selected point
                float x = event.getX();
                float y = event.getY();
                cx = event.getX();
                cy = event.getY();
                if(cx > viewSize.getWidth() ||
                        cy > viewSize.getHeight()) break;
                if(gotPointToMov){
                    pts[point_to_mov].set(x,y);
                    setMaskPath();
                    setCirclePointPath();
                }
                break;
            }
            case MotionEvent.ACTION_UP:{
                hasClicked = false;
            }
        }

        if (gotPointToMov | hasClicked)invalidate();
        return true;
    }



    public Path getMaskPaths(){
        return maskPath;
    }
    public PointF[] getPoints(){
        return pts;
    }
    private Path getTransformPath(int newWidth, int newHeight){
        Matrix frameToCrop = getTransformMatrix(newWidth,newHeight);
        if (frameToCrop == null) return null;

        Path t_maskPath = new Path(maskPath);
        t_maskPath.transform(frameToCrop);
        return t_maskPath;
    }

    private Matrix getTransformMatrix(int newWidth, int newHeight){
        if(maskPath == null) return null;
        return ImageUtilities.getTransformationMatrix(
                viewSize.getWidth(),viewSize.getHeight(),
                newWidth,newHeight,
                0,false);
    }

    public PointF[] getTransformPoints(int newWidth, int newHeight){
        PointF[] transformedPoints = new PointF[4];
        Matrix frameToCrop = getTransformMatrix(newWidth,newHeight);

        if (frameToCrop == null) return null;
        float[] pts_temp = new float[8];

        int j = 0;
        for (int i = 0; i < pts.length; i++) {
            pts_temp[j] = pts[i].x;
            pts_temp[j+1]= pts[i].y;
            j+=2;
        }
        frameToCrop.mapPoints(pts_temp);
        for (int i = 0; i < pts_temp.length; i+=2) {
            transformedPoints[i/2] = new PointF(pts_temp[i],pts_temp[i+1]);
        }
        return transformedPoints;
    }

    @Override
    public synchronized void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(maskPath,maskFillPaint);
        canvas.drawPath(maskPath,maskBorderPaint);
        canvas.drawPath(pointCirclesPath,pointPaint);
        if (hasClicked) canvas.drawCircle(cx,cy,onClicked_radius,onClickPaint);
    }
}
