package com.proxy.shadowsocksr;

import com.proxy.shadowsocksr.ISSRServiceCallback;

interface ISSRService
{
    boolean status();

    oneway void registerISSRServiceCallBack(ISSRServiceCallback cb);
    oneway void unRegisterISSRServiceCallBack(ISSRServiceCallback cb);

    oneway void start();
    oneway void stop();
}
