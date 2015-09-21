package com.proxy.shadowsocksr.util;

import android.content.Context;

public class ScreenUtil
{
    public static int dp2px(Context context,int dp)
    {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
