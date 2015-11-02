package com.proxy.shadowsocksr.impl.crypto.crypto;

import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.CFBBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

public class AESCrypto extends AbsCrypto
{
    private int bit;
    private String mode;
    private byte[] key;

    /**
     * @param cryptMethod must be aes-bit-mode,e.g. aes-256-cfb
     * @param key         crypt key
     */
    public AESCrypto(String cryptMethod, byte[] key)
    {
        super(cryptMethod, key);
        String[] cpt = cryptMethod.split("-");
        //
        bit = Integer.valueOf(cpt[1]);
        mode = cpt[2];
        this.key = key;
        init();
    }

    private StreamCipher encrypt;
    private StreamCipher decrypt;

    private void init()
    {
        switch (mode)
        {
            case "cfb":
                encrypt = new CFBBlockCipher(new AESFastEngine(), bit);//save power...may be...=_=
                decrypt = new CFBBlockCipher(new AESFastEngine(), bit);
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
        encrypt.reset();
        decrypt.init(false, new ParametersWithIV(new KeyParameter(key), iv));
    }

    @Override public byte[] encrypt(byte[] data)
    {
        byte[] out = new byte[data.length];
        try
        {
            encrypt.processBytes(data, 0, data.length, out, 0);
            return out;
        }
        catch (DataLengthException ignored)
        {
        }
        return null;
    }

    @Override public byte[] decrypt(byte[] data)
    {
        byte[] out = new byte[data.length];
        try
        {
            decrypt.processBytes(data, 0, data.length, out, 0);
            return out;
        }
        catch (Exception ignored)
        {
        }
        return null;
    }
}
