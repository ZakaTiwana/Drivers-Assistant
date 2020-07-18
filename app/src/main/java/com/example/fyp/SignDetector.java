package com.example.fyp;


import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Trace;
import android.util.Log;


import org.jetbrains.annotations.NotNull;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.example.fyp.customutilities.ImageUtilities;
public class SignDetector{

    private static final String TAG = "SignDetector";

    private static final float THRESHOLD_SCORE = 0.9f;
    public static final String SIGN_CLASSIFIER_MODEL = "traffic_nomalized.tflite";//"traffic.tflite";//"gtsrb.lite";
//    public static final String SIGN_CLASSIFIER_MODEL = "traffic.tflite";
    private static final String SIGN_CLASSIFIER_LABEL = "gtsrb_label.txt";
    public static final int SIGN_CLASSIFIER_INPUT_SIZE = 30;//224;
    private static final Boolean SIGN_CLASSIFIER_IS_QUANTIZED = false;
    //only for Sign Classifier model
    private static final int NUM_CLASS_FOR_SIGN = 43;

    // For Float model
//    private static final float IMAGE_MEAN = 128.0f;
//    private static final float IMAGE_STD = 128.0f;

    private static final float IMAGE_MEAN = 0;
//    private static final float IMAGE_STD = 1;// for traffic.tflite
    private static final float IMAGE_STD = 255f; //for traffic_normalized.tflite

    private Detector detector;
    private int width;
    private int height;
    private int srcWidth;
    private int srcHeight;
    private boolean isModelQuantized;

    private Matrix frameToCrop;
    private Matrix cropToFrame;

    private int[] intValues;

    private Vector<String> labels = new Vector<String>();

    //output of sign classifier
    private float[][] outputScore_sign_classifier;
    private ByteBuffer imgData;
//    private ByteBuffer outputBuffer;
//    private int[] outputValues;

    private Interpreter tfLite;
    private SignDetector(){}

    /** Memory-map the model file in Assets. */
    private static ByteBuffer loadModelFile(AssetManager assets)
            throws IOException {
        Log.d(TAG, String.format("loadModelFile: assetManager = %s", assets.toString()));
        AssetFileDescriptor fileDescriptor = assets.openFd(SIGN_CLASSIFIER_MODEL);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }



    public static SignDetector create(AssetManager assetManager ,int srcWidth,int srcHeight) throws IOException{
        SignDetector s = new SignDetector();
        s.detector =  Detector.create(assetManager,Detector.SIGN_DETECTOR_MODEL,srcWidth,  srcHeight);

        try {
//            GpuDelegate delegate = new GpuDelegate();
//            Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
//            s.tfLite = new Interpreter(loadModelFile(assetManager), options);
            s.tfLite = new Interpreter(loadModelFile(assetManager));
        } catch (Exception e) {
            Log.e(TAG, "create: Exception when loading tflite interpreter with gpu option now trying with gpu option");
            try {
                s.tfLite = new Interpreter(loadModelFile(assetManager));
            } catch (Exception ex){
                throw e;
            }
        }
        s.srcWidth = srcWidth;
        s.srcHeight = srcHeight;
        s.width = SIGN_CLASSIFIER_INPUT_SIZE;
        s.height = SIGN_CLASSIFIER_INPUT_SIZE;
        s.isModelQuantized = SIGN_CLASSIFIER_IS_QUANTIZED;

        s.frameToCrop = ImageUtilities.getTransformationMatrix(
                srcWidth,srcHeight,
                Detector.SIGN_DETECTOR_INPUT_SIZE,Detector.SIGN_DETECTOR_INPUT_SIZE,
                0,false
        );
        s.cropToFrame = ImageUtilities.getTransformationMatrix(
                Detector.OBJ_DETECTOR_INPUT_SIZE,Detector.OBJ_DETECTOR_INPUT_SIZE,
                srcWidth,srcHeight,
                0,false
        );


        InputStream labelsInput = null;
        labelsInput = assetManager.open(SIGN_CLASSIFIER_LABEL);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
//            Log.w(TAG,line);
            s.labels.add(line.toLowerCase().trim());
        }
        br.close();

        int numBytesPerChannel;
        if (s.isModelQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        int BATCH_SIZE = 1;
        s.imgData = ByteBuffer.allocateDirect(BATCH_SIZE * SIGN_CLASSIFIER_INPUT_SIZE *
                                                SIGN_CLASSIFIER_INPUT_SIZE * 3 * numBytesPerChannel);
        s.imgData.order(ByteOrder.nativeOrder());
        s.intValues = new int[s.width * s.height];
        s.outputScore_sign_classifier = new float[1][NUM_CLASS_FOR_SIGN];

        return s;
    }

    private void setImageData(final Bitmap bmp){
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bmp.getPixels(intValues, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

        imgData.rewind();

        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                int pixelValue = intValues[i * width + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Trace.endSection(); // preprocessBitmap
    }

    public String runOnlyClassification(Bitmap bmp,boolean allowToRecycle){
        Bitmap resized = ImageUtilities.getResizedBitmap(bmp,SignDetector.SIGN_CLASSIFIER_INPUT_SIZE,
                SignDetector.SIGN_CLASSIFIER_INPUT_SIZE,false);
        setImageData(resized);
        String rec = recognizeSign();
        if (!resized.isRecycled())resized.recycle();
        if (!bmp.isRecycled() && allowToRecycle) bmp.recycle();
        return rec;
    }

    public List<RecognizedObject> run(@NotNull Bitmap bmp,boolean allowToRecycleBitmap) {
        List<RecognizedObject> recognizedObjects = new ArrayList<>();
        List<RecognizedObject> recognizedObjects_temp = detector.run(
                bmp,false);

        Bitmap resizeBitmap = null;
        Bitmap croppedBmp = null ;
        RectF location;
        String label;
        for (RecognizedObject rc :
                recognizedObjects_temp) {

            if(rc.getScore() >= THRESHOLD_SCORE) {
                Log.d(TAG, "run: sign score = "+rc.getScore());


                location = rc.getLocation();
                // increasing location
                location.set(location.left,location.top - 10 < 0 ? 0 : location.top - 10,location.right,location.bottom);
                frameToCrop.mapRect(location);


                croppedBmp = getCropBitmapByCPU(bmp,
                        location,false);

                resizeBitmap = ImageUtilities.getResizedBitmap(croppedBmp,
                        width,height,
                        true);
//                createCustomFile(resizeBitmap,rc.getLabel()+Calendar.getInstance().getTime());
                setImageData(resizeBitmap);
                label = recognizeSign();
                Log.d(TAG, "run: sign label = "+ label);

                cropToFrame.mapRect(location);
                recognizedObjects.add(
                        new RecognizedObject(
                                "" + rc.getId(),   label,
                                rc.getScore(),         location  )
                );
                Log.d(TAG, "run: label : "+rc.getLabel());

                if (!resizeBitmap.isRecycled()) resizeBitmap.recycle();
                if (!croppedBmp.isRecycled()) croppedBmp.recycle();
            }
        }
        if (!bmp.isRecycled() && allowToRecycleBitmap) bmp.recycle();
        return recognizedObjects;
    }


    private String recognizeSign() {

        // Run the inference call.
        Trace.beginSection("run");
        tfLite.run(imgData,outputScore_sign_classifier);
        Trace.endSection();

        int j=0;
        int max_index = 0;
        float max = outputScore_sign_classifier[j][0];
        for(int i=0; i < labels.size(); i++){
            if(max < outputScore_sign_classifier[j][i]){
                max = outputScore_sign_classifier[j][i];
                max_index = i;
            }
        }
        return labels.get(max_index);
    }
    private static Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF, boolean allowToRecycleBitmap) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(),
                (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas cavas = new Canvas(resultBitmap);

        // draw background
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        cavas.drawRect(
                new RectF(0, 0, cropRectF.width(), cropRectF.height()),
                paint);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);

        cavas.drawBitmap(source, matrix, paint);

        if (!source.isRecycled() && allowToRecycleBitmap) source.recycle();
        return resultBitmap;
    }
}
