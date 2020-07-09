package com.example.fyp.customutilities;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.util.Log;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.gson.Gson;

import java.lang.reflect.Type;

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

    public static Object loadObject(SharedPreferences sp, String key, Type objClass){
        Gson gson = new Gson();
        String json = sp.getString(key,null);
        if(json == null) throw new IllegalArgumentException(key+" (key) is not a present in provided SharedPreference");
        Log.d(TAG, String.format("loadObject: %s => %s", key,json));
        return gson.fromJson(json, objClass);
    }
}
