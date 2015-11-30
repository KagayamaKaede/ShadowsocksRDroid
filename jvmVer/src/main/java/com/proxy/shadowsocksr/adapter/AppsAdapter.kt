package com.proxy.shadowsocksr.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView

import com.proxy.shadowsocksr.R
import com.proxy.shadowsocksr.adapter.items.AppItem

class AppsAdapter(private val appLst: List<AppItem>) : RecyclerView.Adapter<AppsAdapter.ViewHolder>()
{
    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_app_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(vh: ViewHolder, position: Int)
    {
        val ai = appLst[position]
        vh.tv.text = ai.name
        vh.iv.setImageDrawable(ai.icon)
        vh.cb.isChecked = ai.checked
    }

    override fun getItemCount(): Int
    {
        return appLst.size
    }

    public inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener
    {
        var iv: ImageView
        var tv: TextView
        var cb: CheckBox

        init
        {
            iv = v.findViewById(R.id.iv) as ImageView
            tv = v.findViewById(R.id.tv_name) as TextView
            cb = v.findViewById(R.id.cb) as CheckBox
            v.setOnClickListener(this)
        }

        override fun onClick(v: View)
        {
            if (onItemClickListener != null)
            {
                cb.isChecked = !cb.isChecked
                appLst[adapterPosition].checked = cb.isChecked
                onItemClickListener!!.onItemClick(v, adapterPosition)
            }
        }
    }

    interface OnItemClickListener
    {
        fun onItemClick(v: View, pos: Int)
    }
}
