package com.proxy.shadowsocksr.impl.crypto.crypto;

/*
 * If you need add new Crypto, just extends this class, and change CryptoChooser class.
 */
public abstract class AbsCrypto
{
    /**
     *
     * @param cryptMethod Crypt method name,should lower case.
     * @param key crypt key
     */
    public AbsCrypto(String cryptMethod, byte[] key)
    {
    }

    public abstract void updateEncryptIV(byte[] iv);//If cipher not use IV, just write empty method.
    public abstract void updateDecryptIV(byte[] iv);//If cipher not use IV, just write empty method.
    public abstract byte[] encrypt(byte[] data);
    public abstract byte[] decrypt(byte[] data);
}
