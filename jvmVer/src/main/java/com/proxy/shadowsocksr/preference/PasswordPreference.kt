package com.proxy.shadowsocksr.preference

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.preference.EditTextPreference
import android.util.AttributeSet

import com.proxy.shadowsocksr.R

class PasswordPreference : EditTextPreference
{
    constructor(context: Context) : super(context)
    {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs,
            defStyleAttr)
    {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int,
                defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    {
    }

    internal var res = context.resources

    override fun setText(text: String)
    {
        super.setText(text)

        if (text == "")
        {
            summary = res.getString(R.string.not_set_yet)
        }
        else
        {
            summary = res.getString(R.string.being_set)
        }
    }

    override fun setSummary(summary: CharSequence)
    {
        if (text == null || text == "")
        {
            super.setSummary(res.getString(R.string.not_set_yet))
        }
        else
        {
            super.setSummary(res.getString(R.string.being_set))
        }
    }
}
