package com.example.fyp.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.fyp.customutilities.ImageUtilities;

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
    private static final float nearPoint_radius= 50;
    private static final float point_radius = 20f;

    private PointF[] pts = null;
    private PointF[] mid_line_pts = null;
    private boolean isClickedOnMidPoint = false;

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
        pts[1] = new PointF( centerX + 100, centerY - 100 );
        pts[2] = new PointF( centerX + 100, centerY + 100 );
        pts[3] = new PointF( centerX - 100, centerY + 100 );

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
        pts[1] = new PointF( centerX + 100, centerY - 100 );
        pts[2] = new PointF( centerX + 100, centerY + 100 );
        pts[3] = new PointF( centerX - 100, centerY + 100 );
        setMaskPath();
        setCirclePointPath();
    }

    private void setCirclePointPath(){
        if (pointCirclesPath == null) return;
        mid_line_pts = new PointF[4];
        pointCirclesPath.reset();
        int secondIndex;
        for (int i=0; i < pts.length ; i++) {
            pointCirclesPath.addCircle(pts[i].x, pts[i].y, point_radius, Path.Direction.CCW);
            secondIndex  = i+1 >= pts.length ? 0 : i+1;
            float mid_x = (pts[i].x + pts[secondIndex].x ) / 2f ;
            float mid_y = (pts[i].y + pts[secondIndex].y) /2f;
            mid_line_pts[i] = new PointF(mid_x,mid_y);
            pointCirclesPath.addCircle(mid_x,mid_y, point_radius, Path.Direction.CCW);
        }
    }

    private void setMaskPath(){
        if(maskPath == null) return;
        maskPath.reset();
        maskPath.moveTo(pts[0].x,pts[0].y);
        for (int i = 0; i < pts.length; i ++) {
            maskPath.lineTo(pts[i].x,pts[i].y);
        }
        maskPath.close();
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

                // mid point on line, priority to mid line points
                for (int i=0; i< mid_line_pts.length; i++) {
                    if (  Math.pow(mid_line_pts[i].x - center_x,2) + Math.pow(mid_line_pts[i].y - center_y,2) < radius_pow_2){
                        point_to_mov = i;
                        gotPointToMov = true;
                        isClickedOnMidPoint = true;
                        break;
                    }
                    gotPointToMov = false;
                    isClickedOnMidPoint = false;
                }
                if(isClickedOnMidPoint) break;
                //corner points
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

                if(gotPointToMov){
                    if( cx > viewSize.getWidth()  ||
                        cy > viewSize.getHeight() ||
                        cx < 0  ||
                        cy < 0) break;
                   if (isClickedOnMidPoint){
                       float movY = y - mid_line_pts[point_to_mov].y;
                       float movX = x - mid_line_pts[point_to_mov].x;
                       switch (point_to_mov){
                           case 0:
                               pts[0].set(pts[0].x,pts[0].y + movY);
                               pts[1].set(pts[1].x,pts[1].y + movY);
                               break;
                           case 1:
                               pts[1].set(pts[1].x + movX,pts[1].y);
                               pts[2].set(pts[2].x + movX,pts[2].y);
                               break;
                           case 2:
                               pts[2].set(pts[2].x,pts[2].y + movY);
                               pts[3].set(pts[3].x,pts[3].y + movY);
                               break;
                           case 3:
                               pts[3].set(pts[3].x + movX,pts[3].y);
                               pts[0].set(pts[0].x + movX,pts[0].y);
                               break;
                       }
                   }else {
                       pts[point_to_mov].set(x,y);
                   }
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


    public void setPts(PointF[] pts) {
        this.pts = pts;
        setMaskPath();
        setCirclePointPath();
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
