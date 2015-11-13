package com.proxy.shadowsocksr.preference

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.preference.ListPreference
import android.util.AttributeSet

class SummaryListPreference : ListPreference
{
    constructor(context: Context) : super(context)
    {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs,
            defStyleAttr)
    {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int,
                defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    {
    }

    override fun setValue(value: String)
    {
        super.setValue(value)
        val entry = entry
        if (entry != null)
        {
            summary = entry.toString()
        }
        else
        {
            summary = value
        }
    }

    override fun setSummary(summary: CharSequence?)
    {
        if (summary == null || summary.toString().isEmpty())
        {
            super.setSummary("")
        }
        else
        {
            super.setSummary(summary)
        }
    }
}
