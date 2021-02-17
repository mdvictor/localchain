package com.LocalChain;

import java.security.PublicKey;

public class TxOutput {
    public String id;
    public PublicKey recipient;
    public float value;
    public String parentTxId;

    public TxOutput(PublicKey recipient, float value, String parentTxId) {
        this.recipient = recipient;
        this.value = value;
        this.parentTxId = parentTxId;
        this.id = CryptoService.applySha256(CryptoService.getStringFromKey(recipient) + value + parentTxId);
    }

    public boolean isMine(PublicKey publicKey) {
        return (publicKey.equals(recipient));
    }
}
