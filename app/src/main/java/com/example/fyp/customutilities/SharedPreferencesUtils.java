package com.example.fyp.customutilities;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.util.Log;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.core.Mat;

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

    public static Object loadObject(SharedPreferences sp, String key, Type objClass)
            throws IllegalArgumentException{
        Gson gson = new Gson();
        String json = sp.getString(key,null);
        if(json == null) throw new IllegalArgumentException(key+" (key) is not a present in provided SharedPreference");
        Log.d(TAG, String.format("loadObject: %s => %s", key,json));
        return gson.fromJson(json, objClass);
    }

    public static boolean saveMat(
            SharedPreferences sp, String key, Mat mat){
        if (key.isEmpty() || mat == null) return false;
        try{
            String json = matToJson(mat);
            if (json.contentEquals("{}")) return false;
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

    public static void saveJsonString(SharedPreferences sp, String key, String json){
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key,json);
        editor.apply();
        Log.d(TAG, String.format("saveString: (key:value) saved => %s : %s", key,json));
    }

    public static void saveBool(SharedPreferences sp, String key,boolean value){
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(key,value);
        editor.apply();
        Log.d(TAG, String.format("saveBool: (key:value) saved => %s : %b", key,value));
    }

    public static boolean loadBool(SharedPreferences sp, String key){
        // default will be false
        boolean flag = sp.getBoolean(key,false);
        Log.d(TAG, String.format("loadBool: %s => %s", key,flag));
        return flag;
    }

    public static Mat loadMat(SharedPreferences sp, String key)
            throws IllegalArgumentException{
        String json = sp.getString(key,null);
        if(json == null) throw new IllegalArgumentException(key+" (key) is not a present in provided SharedPreference");
        Log.d(TAG, String.format("loadMat: %s => %s", key,json));
        return matFromJson(json);
    }

    private static String matToJson(Mat mat){
        JsonObject obj = new JsonObject();

        if(mat.isContinuous()){
            int cols = mat.cols();
            int rows = mat.rows();

            JsonArray data = new JsonArray();
            for (int i = 0; i < rows*cols; i++) {
                if (mat.get(i/rows,i%cols) !=null) data.add(mat.get(i/rows,i%cols)[0]);
            }
            obj.addProperty("rows", mat.rows());
            obj.addProperty("cols", mat.cols());
            obj.addProperty("type", mat.type());

            obj.add("data",data);

            Gson gson = new Gson();
            return gson.toJson(obj);
        } else {
            Log.e(TAG, "Mat not continuous.");
        }
        return "{}";
    }

    private static Mat matFromJson(String json){
        JsonParser parser = new JsonParser();
        JsonObject JsonObject = parser.parse(json).getAsJsonObject();

        int rows = JsonObject.get("rows").getAsInt();
        int cols = JsonObject.get("cols").getAsInt();
        int type = JsonObject.get("type").getAsInt();

        JsonArray data = JsonObject.get("data").getAsJsonArray();
        Mat mat = new Mat(rows, cols, type);

        for (int i = 0; i < rows*cols; i++) {
            try{
                mat.put(i/rows, i%cols, data.get(i).getAsDouble());
            }catch (Exception ignored){} // ignoring null
        }
        return mat;
    }

}
