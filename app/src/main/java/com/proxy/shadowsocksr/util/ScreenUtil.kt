package com.proxy.shadowsocksr.util

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.Display
import android.view.WindowManager

class ScreenUtil
{
    companion object
    {
        @JvmStatic fun dp2px(context: Context, dp: Int): Int
        {
            val scale = context.resources.displayMetrics.density
            return (dp * scale + 0.5f).toInt()
        }

//        @JvmStatic fun getNavigationBarSize(context: Context): Point
//        {
//            val appUsableSize = getAppUsableScreenSize(context)
//            val realScreenSize = getRealScreenSize(context)
//
//            // navigation bar on the right
//            if (appUsableSize.x < realScreenSize.x)
//            {
//                return Point(realScreenSize.x - appUsableSize.x, appUsableSize.y)
//            }
//
//            // navigation bar at the bottom
//            if (appUsableSize.y < realScreenSize.y)
//            {
//                return Point(appUsableSize.x, realScreenSize.y - appUsableSize.y)
//            }
//
//            // navigation bar is not present
//            return Point()
//        }
//
//        @JvmStatic fun getAppUsableScreenSize(context: Context): Point
//        {
//            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//            val display = windowManager.defaultDisplay
//            val size = Point()
//            display.getSize(size)
//            return size
//        }
//
//        @JvmStatic fun getRealScreenSize(context: Context): Point
//        {
//            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//            val display = windowManager.defaultDisplay
//            val size = Point()
//
//            if (Build.VERSION.SDK_INT >= 17)
//            {
//                display.getRealSize(size)
//            }
//            else if (Build.VERSION.SDK_INT >= 14)
//            {
//                try
//                {
//                    size.x = Display::class.java.getMethod("getRawWidth").invoke(display) as Int
//                    size.y = Display::class.java.getMethod("getRawHeight").invoke(display) as Int
//                }
//                catch (ignored: Exception)
//                {
//                }
//
//            }
//
//            return size
//        }
    }
}
