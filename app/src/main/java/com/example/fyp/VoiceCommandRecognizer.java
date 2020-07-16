package com.example.fyp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.List;

public class VoiceCommandRecognizer {

    private static final String DEFAULT_UNSUCCESSFUL_MSG = "Unable to understand request, please try again";
    private static final String TAG = "VoiceCommandRecognizer";
    private SpeechRecognizer speechRecog;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 4;
    private Context context;
    private boolean isMicrophonePermissionsGranted = false;
    private OnResultCallback onResultCallback;
    private OnReadyCallback onReadyCallback;

    public interface OnResultCallback {
        public void performTask(String msg);
    }

    public interface OnReadyCallback {
        public void ready();
    }

    public VoiceCommandRecognizer(Context context) {
        this.context = context;
    }

    private void checkMicrophonePermission() {
        Log.d("TAG", "getMicrophonePermission: getting microphone permission");
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED) {
            isMicrophonePermissionsGranted = true;
        }
    }

    public boolean run() {
        checkMicrophonePermission();
        Log.d(TAG, "run: speachRecog = " + speechRecog);
        if (!isMicrophonePermissionsGranted || speechRecog == null) return false;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //      intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecog.startListening(intent);
        return true;
    }

    public void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecog = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecog.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    onReadyCallback.ready();
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                }

                @Override
                public void onError(int error) {
                }

                @Override
                public void onResults(Bundle results) {
                    List<String> result_arr = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    processResult(result_arr.get(0));
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        }
    }

    private void processResult(String result_message) {
        result_message = result_message.toLowerCase();

        Log.d(TAG, "Msg: " + result_message);

        String task_result_msg = DEFAULT_UNSUCCESSFUL_MSG;
        //       if ((result_message.contains("turn"))) {
        if (result_message.contains("turn on")) {
            if (result_message.contains("lane guide")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("lane_guide", true);
                editor.apply();
                task_result_msg = "lane guide turned on";
            } else if (result_message.contains("distance calculator")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("distance_calculator", true);
                editor.apply();
                task_result_msg = "distance calculator has been turned on!";
            } else if (result_message.contains("object detection")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("object_detection", true);
                editor.apply();
                task_result_msg = "object detection has been turned on!";
            } else if (result_message.contains("sign detection")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("sign_detection", true);
                editor.apply();
                task_result_msg = "sign detection has been turned on!";
            } else if (result_message.contains("mute warnings")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("mute_warnings", true);
                editor.apply();
                task_result_msg = "mute warnings has been turned on!";
            }

        } else if (result_message.contains("turn off")) {
            if (result_message.contains("lane guide")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("lane_guide", false);
                editor.apply();
                task_result_msg = "Lane guide has been turned off!";
            } else if (result_message.contains("distance calculator")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("distance_calculator", false);
                editor.apply();
                task_result_msg = "distance calculator has been turned off!";
            } else if (result_message.contains("object detection")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("object_detection", false);
                editor.apply();
                task_result_msg = "object detection has been turned off!";
            } else if (result_message.contains("sign detection")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("sign_detection", false);
                editor.apply();
                task_result_msg = "sign detection turned off";

            } else if (result_message.contains("mute warnings")) {
                SharedPreferences settings = context.getSharedPreferences("feature_settings", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("mute_warnings", false);
                editor.apply();
                task_result_msg = "mute warnings has been turned off!";
            }
        }
        //   }
        onResultCallback.performTask(task_result_msg);
    }

    public void setOnResultCallback(OnResultCallback onResultCallback) {
        this.onResultCallback = onResultCallback;
    }

    public void setOnReadyCallback(OnReadyCallback onReadyCallback) {
        this.onReadyCallback = onReadyCallback;
    }
}
