package com.proxy.shadowsocksr.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.proxy.shadowsocksr.R;
import com.proxy.shadowsocksr.adapter.items.AppItem;

import java.util.List;

public final class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder>
{
    private List<AppItem> appLst;
    private OnItemClickListener onItemClickListener;

    public AppsAdapter(List<AppItem> appLst)
    {
        this.appLst = appLst;
    }

    @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.list_app_item, parent, false);
        return new ViewHolder(v);
    }

    @Override public void onBindViewHolder(ViewHolder vh, int position)
    {
        AppItem ai = appLst.get(position);
        vh.tv.setText(ai.name);
        vh.iv.setImageDrawable(ai.icon);
        vh.cb.setChecked(ai.checked);
    }

    @Override public int getItemCount()
    {
        return appLst.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        public ImageView iv;
        public TextView tv;
        public CheckBox cb;

        public ViewHolder(View v)
        {
            super(v);
            iv = (ImageView) v.findViewById(R.id.iv);
            tv = (TextView) v.findViewById(R.id.tv_name);
            cb = (CheckBox) v.findViewById(R.id.cb);
            v.setOnClickListener(this);
        }

        @Override public void onClick(View v)
        {
            if (onItemClickListener != null)
            {
                appLst.get(getAdapterPosition()).checked = !cb.isChecked();
                cb.setChecked(!cb.isChecked());
                onItemClickListener.onItemClick(v, getAdapterPosition());
            }
        }
    }

    public interface OnItemClickListener
    {
        void onItemClick(View v, int pos);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener)
    {
        this.onItemClickListener = onItemClickListener;
    }
}
