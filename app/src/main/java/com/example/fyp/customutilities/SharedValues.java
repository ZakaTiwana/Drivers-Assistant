package com.example.fyp.customutilities;

import android.util.Size;

public class SharedValues {
    public static final Size[] DESIRED_PREVIEW_SIZES =
            {
                    new Size(640,480),
                    new Size(720,480),
                    new Size(960,720),
                    new Size(1280,720),
                    new Size(1440,1080),
                    new Size(1920,1080),
                    new Size(2048,1152),
                    new Size(3264,1836),
                    new Size(4128,2322)
            };
    public static final Size CROP_SIZE = new Size(300,300);
}
