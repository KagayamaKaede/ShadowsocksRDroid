package com.proxy.shadowsocksr.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.proxy.shadowsocksr.R;

import java.util.ArrayList;
import java.util.List;

public final class ToolbarSpinnerAdapter extends BaseAdapter
{
    private List<String> items = new ArrayList<>();

    public ToolbarSpinnerAdapter(List<String> items)
    {
        this.items = items;
    }

    public int getPosition(String item)
    {
        return items.indexOf(item);
    }

    @Override
    public int getCount()
    {
        return items.size();
    }

    @Override
    public Object getItem(int position)
    {
        return items.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent)
    {
        if (view == null || !view.getTag().toString().equals("DROPDOWN"))
        {
            view = LayoutInflater.from(parent.getContext())
                                 .inflate(R.layout.toolbar_spinner_dropdown_item, parent, false);
            view.setTag("DROPDOWN");
        }

        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(getTitle(position));

        return view;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent)
    {
        if (view == null || !view.getTag().toString().equals("NON_DROPDOWN"))
        {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.
                                                                            toolbar_spinner_item,
                                                                    parent, false);
            view.setTag("NON_DROPDOWN");
        }
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(getTitle(position));
        return view;
    }

    private String getTitle(int position)
    {
        return position >= 0 && position < items.size() ? items.get(position) : "";
    }
}
