package com.proxy.shadowsocksr.preference

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.preference.EditTextPreference
import android.util.AttributeSet

class SummaryEditTextPreference : EditTextPreference
{
    private var defaultSummary: String? = null

    constructor(context: Context) : super(context)
    {
        defaultSummary = summary.toString()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    {
        defaultSummary = summary.toString()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs,
            defStyleAttr)
    {
        defaultSummary = summary.toString()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int,
                defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    {
        defaultSummary = summary.toString()
    }

    override fun setText(text: String)
    {
        super.setText(text)
        summary = text
    }

    override fun setSummary(summary: CharSequence?)
    {
        if (summary != null && summary.length == 0)
        {
            super.setSummary(defaultSummary)
        }
        else
        {
            super.setSummary(summary)
        }
    }
}
