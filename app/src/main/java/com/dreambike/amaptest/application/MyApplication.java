package com.dreambike.amaptest.application;

import android.app.Application;


/**
 * Created by Administrator on 2017/4/20.
 */

public class MyApplication extends Application {

    private static MyApplication myApplication;
    public static MyApplication getInstance() {
        return myApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
    }
}
