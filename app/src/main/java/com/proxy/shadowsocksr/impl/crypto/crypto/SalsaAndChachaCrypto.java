package com.proxy.shadowsocksr.impl.crypto.crypto;

import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.ChaChaEngine;
import org.spongycastle.crypto.engines.Salsa20Engine;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

public class SalsaAndChachaCrypto extends AbsCrypto
{
    private byte[] key;

    private StreamCipher chachaE;
    private StreamCipher chachaD;
    private StreamCipher salsaE;
    private StreamCipher salsaD;

    private boolean isChacha = true;

    public SalsaAndChachaCrypto(String cryptMethod, byte[] key)
    {
        super(cryptMethod, key);
        //
        if (cryptMethod.equals("salsa20"))
        {
            salsaE = new Salsa20Engine();
            salsaD = new Salsa20Engine();
            isChacha = false;
        }
        else if (cryptMethod.equals("chacha20"))
        {
            chachaE = new ChaChaEngine();
            chachaD = new ChaChaEngine();
        }
        //
        this.key = key;
    }

    @Override public void updateEncryptIV(byte[] iv)
    {
        if (isChacha)
        {
            chachaE.reset();
            chachaE.init(true, new ParametersWithIV(new KeyParameter(key), iv));
            return;
        }
        salsaE.reset();
        salsaE.init(true, new ParametersWithIV(new KeyParameter(key), iv));
    }

    @Override public void updateDecryptIV(byte[] iv)
    {
        if (isChacha)
        {
            chachaD.reset();
            chachaD.init(false, new ParametersWithIV(new KeyParameter(key),iv));
            return;
        }
        salsaD.reset();
        salsaD.init(false, new ParametersWithIV(new KeyParameter(key), iv));
    }

    @Override public byte[] encrypt(byte[] data)
    {
        byte[] out = new byte[data.length];
        try
        {
            if (isChacha)
            {
                chachaE.processBytes(data, 0, data.length, out, 0);
            }
            else
            {
                salsaE.processBytes(data, 0, data.length, out, 0);
            }
        }
        catch (Exception ignored)
        {
        }
        return out;
    }

    @Override public byte[] decrypt(byte[] data)
    {
        byte[] out = new byte[data.length];
        try
        {
            if (isChacha)
            {
                chachaD.processBytes(data, 0, data.length, out, 0);
            }
            else
            {
                salsaD.processBytes(data, 0, data.length, out, 0);
            }
        }
        catch (Exception ignored)
        {
        }
        return out;
    }
}
