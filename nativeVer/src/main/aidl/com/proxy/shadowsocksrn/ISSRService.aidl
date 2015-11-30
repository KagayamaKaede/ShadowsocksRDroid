package com.proxy.shadowsocksrn;

import com.proxy.shadowsocksrn.ISSRServiceCallback;
import com.proxy.shadowsocksrn.items.ConnectProfile;

interface ISSRService
{
    boolean status();

    oneway void registerISSRServiceCallBack(ISSRServiceCallback cb);
    oneway void unRegisterISSRServiceCallBack();

    oneway void start(in ConnectProfile cp);
    oneway void stop();
}
