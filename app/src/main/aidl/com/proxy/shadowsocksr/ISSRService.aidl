package com.proxy.shadowsocksr;

import com.proxy.shadowsocksr.ISSRServiceCallback;
import com.proxy.shadowsocksr.items.ConnectProfile;

interface ISSRService
{
    boolean status();

    oneway void registerISSRServiceCallBack(ISSRServiceCallback cb);
    oneway void unRegisterISSRServiceCallBack();

    oneway void start(in ConnectProfile cp);
    oneway void stop();
}
