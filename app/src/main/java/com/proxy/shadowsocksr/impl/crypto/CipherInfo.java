package com.proxy.shadowsocksr.impl.crypto;

public class CipherInfo
{
    public String cryptMethod;
    public byte[] key;
    public byte[] eIV;

    public CipherInfo(String cryptMethod, byte[] key, byte[] eIV)
    {
        this.cryptMethod = cryptMethod;
        this.key = key;
        this.eIV = eIV;
    }
}
