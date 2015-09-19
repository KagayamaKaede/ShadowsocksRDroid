package com.proxy.shadowsocksr.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class SummaryEditTextPreference extends EditTextPreference
{
    private String defaultSummary;

    public SummaryEditTextPreference(Context context)
    {
        super(context);
        defaultSummary = getSummary().toString();
    }

    public SummaryEditTextPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        defaultSummary = getSummary().toString();
    }

    public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        defaultSummary = getSummary().toString();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
        defaultSummary = getSummary().toString();
    }

    @Override public void setText(String text)
    {
        super.setText(text);
        setSummary(text);
    }

    @Override public void setSummary(CharSequence summary)
    {
        if (summary.length() == 0)
        {
            super.setSummary(defaultSummary);
        }
        else
        {
            super.setSummary(summary);
        }
    }
}
