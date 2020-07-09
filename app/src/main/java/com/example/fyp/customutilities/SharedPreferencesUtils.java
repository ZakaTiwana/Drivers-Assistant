package com.example.fyp.customutilities;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

public class SharedPreferencesUtils {

    private static final String TAG = "SharedPreferencesUtils";

    public static boolean saveObject(
            SharedPreferences sp, String key, Object obj){
        if (key.isEmpty() || obj == null) return false;
        try{
            Gson gson = new Gson();
            String json = gson.toJson(obj);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(key,json);
            editor.apply();
            Log.d(TAG, String.format("saveObject: (key:value) saved => %s : %s", key,json));
        }catch (Exception ex){
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
