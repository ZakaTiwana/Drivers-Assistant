package com.example.fyp;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.example.fyp.customutilities.ImageUtilities;


public class Detector {

    private static final String TAG = "Detector";

    public static final String OBJ_DETECTOR_MODEL = "detector.tflite";
    private static final String OBJ_DETECTOR_LABEL = "detector_label.txt";
    public static final int OBJ_DETECTOR_INPUT_SIZE = 300;
    private static final Boolean OBJ_DETECTOR_IS_QUANTIZED = true;

    public static final String SIGN_DETECTOR_MODEL = "sign_detect_only.tflite";
    private static final String SIGN_DETECTOR_LABEL = "sign_detect_label.txt";
    public static final int SIGN_DETECTOR_INPUT_SIZE = 300;
    private static final Boolean SIGN_DETECTOR_IS_QUANTIZED = true;
    // Only return this many results. for both above models
    private static final int NUM_DETECTIONS = 10;



    // For Float model
//    private static final float IMAGE_MEAN = 128.0f;
//    private static final float IMAGE_STD = 128.0f;
    private static final float IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 255.0f;



    private int width;
    private int height;
    private Boolean isModelQuantized;

    private int[] intValues;

    private Vector<String> labels = new Vector<String>();
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;

    private ByteBuffer imgData;
//    private ByteBuffer outputBuffer;
//    private int[] outputValues;

    private Interpreter tfLite;

    /** Memory-map the model file in Assets. */
    private static ByteBuffer loadModelFile(AssetManager assets,String model_file)
            throws IOException {
        Log.d(TAG, String.format("loadModelFile: assetManager = %s", assets.toString()));
        AssetFileDescriptor fileDescriptor = assets.openFd(model_file);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /** Initializes a native TensorFlow session. */
    public static Detector create(
            AssetManager assetManager,
            String model) throws IOException,IllegalArgumentException {

        assert  model.contentEquals(OBJ_DETECTOR_MODEL)  ||
                model.contentEquals(SIGN_DETECTOR_MODEL);

        final Detector d = new Detector();
        try {
            GpuDelegate delegate = new GpuDelegate();
            Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
            d.tfLite = new Interpreter(loadModelFile(assetManager,model), options);
        } catch (Exception e) {
            Log.e(TAG, "create: exception at loading model = ",e );
            throw new RuntimeException(e);
        }

        int inputWidth ;
        int inputHeight;
        String labelFilename;
        Boolean isModelQuantized;
        switch (model){
            case OBJ_DETECTOR_MODEL:
                inputHeight = OBJ_DETECTOR_INPUT_SIZE;
                inputWidth = OBJ_DETECTOR_INPUT_SIZE;
                labelFilename = OBJ_DETECTOR_LABEL;
                isModelQuantized = OBJ_DETECTOR_IS_QUANTIZED;
                break;
            case SIGN_DETECTOR_MODEL:
                inputHeight = SIGN_DETECTOR_INPUT_SIZE;
                inputWidth = SIGN_DETECTOR_INPUT_SIZE;
                labelFilename = SIGN_DETECTOR_LABEL;
                isModelQuantized = SIGN_DETECTOR_IS_QUANTIZED;
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("model should be one of %s or %S",
                                OBJ_DETECTOR_MODEL,SIGN_DETECTOR_MODEL));
        }

        d.width = inputWidth;
        d.height = inputHeight;
        d.isModelQuantized = isModelQuantized;
        InputStream labelsInput = null;
        labelsInput = assetManager.open(labelFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
//            Log.w(TAG,line);
            d.labels.add(line);
        }
        br.close();

        int numBytesPerChannel;
        if (d.isModelQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }

        int BATCH_SIZE = 1;
        d.imgData = ByteBuffer.allocateDirect(BATCH_SIZE * inputWidth * inputHeight * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        // Pre-allocate buffers.

        d.intValues = new int[inputWidth * inputHeight];
        d.outputLocations = new float[1][NUM_DETECTIONS][4];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];

        return d;
    }


    private Detector() {}

    public List<RecognizedObject> run(@NotNull Bitmap bmp,boolean allowToRecycleBitmap){
        int srcWidth = bmp.getWidth();
        int srcHeight = bmp.getHeight();
        bmp = ImageUtilities.getResizedBitmap(bmp.copy(Bitmap.Config.ARGB_8888,true),
                width,height,true);
        setImageData(bmp);
        List<RecognizedObject> recs = detectObjectsInImage(srcWidth,srcHeight);
        if (!bmp.isRecycled() && allowToRecycleBitmap) bmp.recycle();
        return recs;
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

    public List<RecognizedObject> detectObjectsInImage(int srcWidth, int srcHeight) {

        //
        Matrix cropToFrame = ImageUtilities.getTransformationMatrix(width,
                height,srcWidth,srcHeight,0,false);
        // Copy the input data into TensorFlow.
        Trace.beginSection("feed");
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Trace.endSection();

        // Show the best detections.
        // after scaling them back to the input size.
        List<RecognizedObject> recognitions = new ArrayList<>(NUM_DETECTIONS);
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
            RectF detection =
                    new RectF(
                            outputLocations[0][i][1] * width,
                            outputLocations[0][i][0] * width,
                            outputLocations[0][i][3] * height,
                            outputLocations[0][i][2] * height);

            // SSD Mobilenet V1 Model assumes class 0 is background class
            // in label file and class labels start from 1 to number_of_classes+1,
            // while outputClasses correspond to class index from 0 to number_of_classes
            int labelOffset = 1;
            cropToFrame.mapRect(detection);
            RecognizedObject rc = new RecognizedObject("" + i,
                    labels.get((int) outputClasses[0][i] + labelOffset),
                    outputScores[0][i],
                    detection);
            recognitions.add(rc);
            Log.d(TAG, "detectObjectsInImage: "+rc);

        }
        Trace.endSection(); // "recognizeImage"
        return recognitions;
    }


}