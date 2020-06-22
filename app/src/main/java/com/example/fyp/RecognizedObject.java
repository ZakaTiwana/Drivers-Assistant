package com.example.fyp;

import android.graphics.RectF;

public class RecognizedObject {
    /**
     * A unique identifier for what has been recognized. Specific to the class, not the instance of
     * the object.
     */
    private final String id;

    /** Optional location within the source image for the location of the recognized object. */
    private RectF location;
    private String label;
    private float score;

    public RecognizedObject(final String id, final String label, final Float score, final RectF location) {

        this.id = id;
        this.location = location;
        this.label = label;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public RectF getLocation() {
        return new RectF(location);
    }

    public String getLabel() {return label;}
    public Float getScore(){return score;}


    @Override
    public String toString() {
//        String resultString = "";
//        if (id != null) {
//            resultString += String.format("[id: %s, label: %s, score: %f ] location: ",id,label,score);
//        }
//
//        if (location != null) {
//            resultString += location + " ";
//        }
//
//        return resultString.trim();

        String resultString = "";
        if (id != null) {
            resultString += String.format("[id: %s label: %s score: %s]",id,label,score);
        }
        return resultString.trim();
    }
}
