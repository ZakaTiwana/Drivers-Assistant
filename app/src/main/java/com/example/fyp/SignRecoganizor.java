package com.example.fyp;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;
import android.util.Log;

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
import java.util.Vector;

public class SignRecoganizor {
    private static final String TAG = "SignRecoganizor";
    private static final String MODEL_FILE = "gtsrb_model.lite";

    // Float model
//    private static final float IMAGE_MEAN = 128.0f;
//    private static final float IMAGE_STD = 128.0f;
    private static final float IMAGE_MEAN = 0;
    public static final float IMAGE_STD = 255.0f;
    private static final int BYTE_SIZE_OF_FLOAT = 4;

    // Only return this many results.
    private static final String labelFilename = "file:///android_asset/sign_labelmap.txt";

    private int width;
    private int height;

    private int[] intValues;

    private Vector<String> labels = new Vector<String>();

    private Interpreter tfLite;
    private ByteBuffer imgData;
    float[][] outScore = new float[1][43];

    private Boolean isModelQuantized = false;

    public SignRecoganizor(){

    }



    /** Memory-map the model file in Assets. */
    private static ByteBuffer loadModelFile(AssetManager assets)
            throws IOException {
        Log.d(TAG, String.format("loadModelFile: assetManager = %s", assets.toString()));
        AssetFileDescriptor fileDescriptor = assets.openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /** Initializes a native TensorFlow session. */
    public static SignRecoganizor create(
            AssetManager assetManager,
            int inputWidth,
            int inputHeight) throws IOException {
        final SignRecoganizor d = new SignRecoganizor();

        try {
            GpuDelegate delegate = new GpuDelegate();
            Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
            d.tfLite = new Interpreter(loadModelFile(assetManager), options);
        } catch (Exception e) {
            Log.e(TAG, "create: exception at loading model = ",e );
            throw new RuntimeException(e);
        }

        d.width = inputWidth;
        d.height = inputHeight;

        Log.d(TAG, String.format("create: width = %d and height = %d", inputWidth,inputHeight));

        InputStream labelsInput = null;
        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        labelsInput = assetManager.open(actualFilename);
        BufferedReader br = null;
        br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            Log.w(TAG,line);
            d.labels.add(line);
        }
        br.close();

        int numBytesPerChannel;
        if (d.isModelQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(1 * inputWidth * inputHeight * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        // Pre-allocate buffers.
        d.intValues = new int[inputWidth * inputHeight];
        return d;
    }

    public String recognizeSign(final Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeSign");

        Trace.beginSection("Sign_preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

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


//        Object inputArray = imgData.;
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        long start = System.currentTimeMillis();
        tfLite.run(imgData,outScore);
        long end = System.currentTimeMillis();
        Log.d(TAG, "recognizeSign: Time elapsed is "+ (end - start));
        Trace.endSection();

        int j=0;
        int max_index = 0;
        float max = outScore[j][0];

        for(int i=0; i<labels.size(); i++){

            if(max < outScore[j][i]){
                max = outScore[j][i];
                max_index = i;
            }
        }
        return labels.get(max_index);
    }

}
