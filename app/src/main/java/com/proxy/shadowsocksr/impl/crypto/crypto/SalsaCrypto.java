package com.proxy.shadowsocksr.impl.crypto.crypto;

import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.Salsa20Engine;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

public final class SalsaCrypto extends AbsCrypto
{
    private StreamCipher salsaE;
    private StreamCipher salsaD;

    public SalsaCrypto(String cryptMethod, byte[] key)
    {
        super(cryptMethod, key);
        //
        salsaE = new Salsa20Engine();
        salsaD = new Salsa20Engine();
    }

    @Override public void updateEncryptIV(byte[] iv)
    {
        salsaE.reset();
        salsaE.init(true, new ParametersWithIV(new KeyParameter(key), iv));
    }

    @Override public void updateDecryptIV(byte[] iv)
    {
        salsaD.reset();
        salsaD.init(false, new ParametersWithIV(new KeyParameter(key), iv));
    }

    @Override public byte[] encrypt(byte[] data)
    {
        byte[] out = new byte[data.length];
        try
        {
            salsaE.processBytes(data, 0, data.length, out, 0);
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
            salsaD.processBytes(data, 0, data.length, out, 0);
        }
        catch (DataLengthException e)
        {
            return new byte[0];
        }
        return out;
    }
}
