package com.LocalChain;

public class TxInput {
    public String txOutputId;
    public TxOutput UTXO;

    public TxInput(String txOutputId) {
        this.txOutputId = txOutputId;
    }
}
