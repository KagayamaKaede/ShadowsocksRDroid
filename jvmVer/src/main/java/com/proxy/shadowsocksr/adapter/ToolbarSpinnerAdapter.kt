package com.proxy.shadowsocksr.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import com.proxy.shadowsocksr.R

import java.util.ArrayList

class ToolbarSpinnerAdapter(var items: List<String>) : BaseAdapter()
{
    fun getPosition(item: String): Int
    {
        return items.indexOf(item)
    }

    override fun getCount(): Int
    {
        return items.size
    }

    override fun getItem(position: Int): Any
    {
        return items[position]
    }

    override fun getItemId(position: Int): Long
    {
        return position.toLong()
    }

    override fun getDropDownView(position: Int, view: View?, parent: ViewGroup): View
    {
        var v = view
        if (v == null || v.tag.toString() != "DROPDOWN")
        {
            v = LayoutInflater.from(parent.context).inflate(
                    R.layout.toolbar_spinner_dropdown_item, parent, false)
            v!!.tag = "DROPDOWN"
        }

        val textView = v.findViewById(android.R.id.text1) as TextView
        textView.text = getTitle(position)

        return v
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View
    {
        var v = view
        if (v == null || v.tag.toString() != "NON_DROPDOWN")
        {
            v = LayoutInflater.from(parent.context).inflate(R.layout.toolbar_spinner_item,
                    parent, false)
            v!!.tag = "NON_DROPDOWN"
        }
        val textView = v.findViewById(android.R.id.text1) as TextView
        textView.text = getTitle(position)
        return v
    }

    private fun getTitle(position: Int): String
    {
        return if (position >= 0 && position < items.size) items[position] else ""
    }
}
