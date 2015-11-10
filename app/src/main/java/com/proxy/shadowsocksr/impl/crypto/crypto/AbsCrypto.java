package com.proxy.shadowsocksr.impl.crypto.crypto;

/*
 * If you need add new Crypto, just extends this class, and change CryptoManager class.
 */
public abstract class AbsCrypto
{
    public String cryptMethod;
    public byte[] key;

    /**
     * @param cryptMethod Crypt method name,should lower case.
     * @param key         crypt key
     */
    public AbsCrypto(String cryptMethod, byte[] key)
    {
        this.cryptMethod = cryptMethod;
        this.key = key;
    }

    public abstract void updateEncryptIV(byte[] iv);//If cipher not use IV, just write empty method.

    public abstract void updateDecryptIV(byte[] iv);//If cipher not use IV, just write empty method.

    public abstract byte[] encrypt(byte[] data);

    public abstract byte[] decrypt(byte[] data);
}
