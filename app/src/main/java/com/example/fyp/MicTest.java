package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class MicTest extends AppCompatActivity {


    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mic_test);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }

        ImageView fab = (ImageView) findViewById(R.id.imageView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Speak now", Toast.LENGTH_SHORT).show();
                // Here, thisActivity is the current activity
//                if (ContextCompat.checkSelfPermission(MicTest.this,
//                        Manifest.permission.RECORD_AUDIO)
//                        != PackageManager.PERMISSION_GRANTED) {
//
//                    // Permission is not granted
//                    // Should we show an explanation?
//                    if (ActivityCompat.shouldShowRequestPermissionRationale(MicTest.this,
//                            Manifest.permission.RECORD_AUDIO)) {
//                        // Show an explanation to the user *asynchronously* -- don't block
//                        // this thread waiting for the user's response! After the user
//                        // sees the explanation, try again to request the permission.
//                    } else {
//                        // No explanation needed; request the permission
//                        ActivityCompat.requestPermissions(MicTest.this,
//                                new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
//
//                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
//                        // app-defined int constant. The callback method gets the
//                        // result of the request.
//                    }
//                } else {
                // Permission has already been granted
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                //      intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                speechRecog.startListening(intent);
                //              }
            }
        });

        initializeTextToSpeech();
        initializeSpeechRecognizer();
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecog = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecog.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {

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

//        Handle at least four sample cases

//        First: What is your Name?
//        Second: What is the time?
//        Third: Is the earth flat or a sphere?
//        Fourth: Open a browser and open url
        Log.d("MicTest", "Msg: " + result_message);
        if (result_message.indexOf("Kia haal hai") != -1) {
            speak("main theekh tm sunao");
        }
        if (result_message.indexOf("who") != -1) {
            if (result_message.indexOf("is kanjar") != -1) {
                speak("Ali!");
            }
//            if (result_message.indexOf("time") != -1) {
//                //String time_now = DateUtils.formatDateTime(this, new Date().getTime(), DateUtils.FORMAT_SHOW_TIME);
//                speak("The time is now: ");
//            }
//        } else if (result_message.indexOf("earth") != -1) {
//            speak("Don't be silly, The earth is a sphere. As are all other planets and celestial bodies");
//        } else if (result_message.indexOf("browser") != -1) {
//            speak("Opening a browser right away master.");
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/AnNJPf-4T70"));
//            startActivity(intent);
//        }

        }
//        else if ((result_message.indexOf("turn off object detection") != -1)) {
//            SharedPreferences settings = getSharedPreferences("feature_settings", 0);
//            SharedPreferences.Editor editor = settings.edit();
//            editor.putBoolean("object_detection", false);
//            editor.commit();
//            speak("object detection has been turned off!");
//
//        } else if ((result_message.indexOf("turn off sign detection") != -1)) {
//            SharedPreferences settings = getSharedPreferences("feature_settings", 0);
//            SharedPreferences.Editor editor = settings.edit();
//            editor.putBoolean("sign_detection", false);
//            editor.commit();
//            speak("sign detection has been turned off!");
//
//        }
        else if ((result_message.indexOf("turn") != -1)) {
            if (result_message.matches("^on$")) {
                if (result_message.indexOf("lane guide") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("lane_guide", true);
                    editor.commit();
                    speak("Lane Guide has been turned on!");
                } else if (result_message.indexOf("distance calculator") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("distance_calculator", true);
                    editor.commit();
                    speak("distance calculator has been turned on!");
                } else if (result_message.indexOf("object detection") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("object_detection", true);
                    editor.commit();
                    speak("object detection has been turned on!");
                } else if (result_message.indexOf("sign detection") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("sign_detection", true);
                    editor.commit();
                    speak("sign detection has been turned on!");
                } else if (result_message.indexOf("mute warnings") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("mute_warnings", true);
                    editor.commit();
                    speak("mute warnings has been turned on!");
                }


            } else if (result_message.indexOf("off") != -1) {
                if (result_message.indexOf("lane guide") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("lane_guide", false);
                    editor.commit();
                    speak("Lane guide has been turned off!");
                } else if (result_message.indexOf("distance calculator") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("distance_calculator", false);
                    editor.commit();
                    speak("distance calculator has been turned off!");
                } else if (result_message.indexOf("object detection") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("object_detection", false);
                    editor.commit();
                    speak("object detection has been turned off!");
                } else if (result_message.indexOf("sign detection") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("sign_detection", false);
                    editor.commit();
                    speak("sign detection has been turned off!");
                } else if (result_message.indexOf("mute warnings") != -1) {
                    SharedPreferences settings = getSharedPreferences("feature_settings", 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("mute_warnings", false);
                    editor.commit();
                    speak("mute warnings has been turned off!");
                }
            }
        }

    }

    private void initializeTextToSpeech() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (tts.getEngines().size() == 0) {
                    Toast.makeText(MicTest.this, "No TTS engine on your device", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    tts.setLanguage(Locale.getDefault());
                    tts.setLanguage(Locale.US);
                    //   Toast.makeText(getApplicationContext(),"check"+Locale.getDefault(),Toast.LENGTH_SHORT);
                    Log.d("check", "language: " + Locale.getDefault());
                    speak("Hello there!");
//                    speak("Oye bhai kesa hai");
                }
            }
        });
    }

    private void speak(String message) {
        if (Build.VERSION.SDK_INT >= 21) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        tts.shutdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        Reinitialize the recognizer and tts engines upon resuming from background such as after openning the browser
        initializeSpeechRecognizer();
        initializeTextToSpeech();
    }

}
