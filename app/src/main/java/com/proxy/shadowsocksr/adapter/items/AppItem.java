package com.proxy.shadowsocksr.adapter.items;

import android.graphics.drawable.Drawable;

public final class AppItem
{
    public Drawable icon;
    public String name;
    public String pkgname;
    public boolean checked;

    public AppItem(Drawable icon, String name, String pkgname, boolean checked)
    {
        this.icon = icon;
        this.name = name;
        this.pkgname = pkgname;
        this.checked = checked;
    }
}
