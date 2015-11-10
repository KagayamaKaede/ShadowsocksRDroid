package com.proxy.shadowsocksr.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import com.proxy.shadowsocksr.R;

public final class PasswordPreference extends EditTextPreference
{
    public PasswordPreference(Context context)
    {
        super(context);
    }

    public PasswordPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public PasswordPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PasswordPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    Resources res = getContext().getResources();

    @Override public void setText(String text)
    {
        super.setText(text);

        if (text.equals(""))
        {
            setSummary(res.getString(R.string.not_set_yet));
        }
        else
        {
            setSummary(res.getString(R.string.being_set));
        }
    }

    @Override public void setSummary(CharSequence summary)
    {
        if (getText() == null || getText().equals(""))
        {
            super.setSummary(res.getString(R.string.not_set_yet));
        }
        else
        {
            super.setSummary(res.getString(R.string.being_set));
        }
    }
}
