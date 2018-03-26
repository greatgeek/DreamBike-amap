package com.dreambike.amaptest.dialog;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import com.dreambike.amaptest.R;

/**
 * Created by Administrator on 2018/3/25 0025.
 */

public class MyLoadDialog extends DialogFragment {
    public static MyLoadDialog getInstance()
    {
        return FirstQuote.instance;
    }

    //在第一次被引用时被加载
    static class FirstQuote
    {
        private static MyLoadDialog instance = new MyLoadDialog();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(false);
        View view =inflater.inflate(R.layout.loading,container);
        ImageView loadingImageView=(ImageView)view.findViewById(R.id.loding_ImageView);
        loadingImageView.setBackgroundResource(R.drawable.loading);
        AnimationDrawable frameAnimation=(AnimationDrawable)loadingImageView.getBackground();

        frameAnimation.start();
        return view;
    }
}
