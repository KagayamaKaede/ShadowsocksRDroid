package com.proxy.shadowsocksr.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class SummaryListPreference extends ListPreference
{
    public SummaryListPreference(Context context)
    {
        super(context);
    }

    public SummaryListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SummaryListPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SummaryListPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override public void setValue(String value)
    {
        super.setValue(value);
        CharSequence entry = getEntry();
        if (entry != null)
        {
            setSummary(entry.toString());
        }
        else
        {
            setSummary(value);
        }
    }

    @Override public void setSummary(CharSequence summary)
    {
        if (summary == null || summary.toString().isEmpty())
        {
            super.setSummary("");
        }
        else
        {
            super.setSummary(summary);
        }
    }
}
