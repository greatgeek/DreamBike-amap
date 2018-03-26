package com.dreambike.amaptest.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.dreambike.amaptest.R;

/**
 * Created by Administrator on 2018/3/25 0025.
 */

public class LoadDialog extends DialogFragment {
    public static LoadDialog getInstance()
    {
        return FirstQuote.instance;
    }

    //在第一次被引用时被加载
    static class FirstQuote
    {
        private static LoadDialog instance = new LoadDialog();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);/**取消标题**/
        getDialog().setCanceledOnTouchOutside(false);/**加载动画时触摸无效**/
        View view = inflater.inflate(R.layout.load, container);/**获取到这个load布局**/
        Animation operatingAnim = AnimationUtils.loadAnimation(view.getContext(), R.anim.tips);/**获得动画效果**/
        LinearInterpolator lin = new LinearInterpolator();/**生成插值器**/
        operatingAnim.setInterpolator(lin);/**动画加载插值器**/
        ImageView iv_load = (ImageView)view.findViewById(R.id.iv_load);/**获得view中的ImageView**/
        iv_load.setAnimation(operatingAnim);/**为这个ImageView设置动画效果**/
        return view;
    }
}
