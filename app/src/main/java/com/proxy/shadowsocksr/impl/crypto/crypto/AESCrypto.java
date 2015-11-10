package com.proxy.shadowsocksr.impl.crypto.crypto;

import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.CFBBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

public final class AESCrypto extends AbsCrypto
{
    private String mode;

    /**
     * @param cryptMethod must be aes-bit-mode,e.g. aes-256-cfb
     * @param key         crypt key
     */
    public AESCrypto(String cryptMethod, byte[] key)
    {
        super(cryptMethod, key);
        mode = cryptMethod.split("-")[2];
        init();
    }

    private StreamCipher encrypt;
    private StreamCipher decrypt;

    private void init()
    {
        switch (mode)
        {
        case "cfb":
            encrypt = new CFBBlockCipher(new AESFastEngine(), 128);//save power...may be...=_=
            decrypt = new CFBBlockCipher(new AESFastEngine(), 128);
            break;
        }
    }

    @Override public void updateEncryptIV(byte[] iv)
    {
        encrypt.reset();
        encrypt.init(true, new ParametersWithIV(new KeyParameter(key), iv));
    }

    @Override public void updateDecryptIV(byte[] iv)
    {
        decrypt.reset();
        decrypt.init(false, new ParametersWithIV(new KeyParameter(key), iv));
    }

    @Override public byte[] encrypt(byte[] data)
    {
        byte[] out = new byte[data.length];
        try
        {
            encrypt.processBytes(data, 0, data.length, out, 0);
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
            decrypt.processBytes(data, 0, data.length, out, 0);
        }
        catch (DataLengthException e)
        {
            return new byte[0];
        }

        return out;
    }
}
