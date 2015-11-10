package com.proxy.shadowsocksr.impl.crypto.crypto;

import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.ChaChaEngine;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

public final class ChachaCrypto extends AbsCrypto
{
    private StreamCipher chachaE;
    private StreamCipher chachaD;

    /**
     * @param cryptMethod Crypt method name,should lower case.
     * @param key         crypt key
     */
    public ChachaCrypto(String cryptMethod, byte[] key)
    {
        super(cryptMethod, key);
        //
        chachaE = new ChaChaEngine();
        chachaD = new ChaChaEngine();
    }

    @Override public void updateEncryptIV(byte[] iv)
    {
        chachaE.reset();
        chachaE.init(true, new ParametersWithIV(new KeyParameter(key), iv));
    }

    @Override public void updateDecryptIV(byte[] iv)
    {
        chachaD.reset();
        chachaD.init(false, new ParametersWithIV(new KeyParameter(key), iv));
    }

    @Override public byte[] encrypt(byte[] data)
    {
        byte[] out = new byte[data.length];
        try
        {
            chachaE.processBytes(data, 0, data.length, out, 0);
        }
        catch (DataLengthException e)
        {
            return new byte[0];
        }
        return out;
    }

    @Override public byte[] decrypt(byte[] data)
    {
        byte[] out = new byte[data.length];
        try
        {
            chachaD.processBytes(data, 0, data.length, out, 0);
        }
        catch (DataLengthException e)
        {
            return new byte[0];
        }
        return out;
    }
}
