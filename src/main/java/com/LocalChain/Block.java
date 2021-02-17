package com.LocalChain;

import java.util.ArrayList;
import java.util.Date;

public class Block {

    public String hash;
    public String previousBlockHash;
    public String merkleRoot = "emptyString";
    public ArrayList<TxRecord> transactions = new ArrayList<>();
    public ArrayList<String> transactionIds = new ArrayList<>();
    public int nonce = 0;
    public long timestamp;

    public Block(String previousBlockHash) {
        this.hash = null;
        this.previousBlockHash = previousBlockHash;
        this.timestamp = new Date().getTime();
    }

    public boolean isMined() {
        return hash != null;
    }

    public boolean addTx(TxRecord transaction) {
        if(transaction == null) return false;
        if((!transaction.processTx())) {
            System.out.println("Transaction failed to process. Discarded.");
            return false;
        }

        transactions.add(transaction);
        transactionIds.add(transaction.txId);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }

    @Override
    public String toString() {
        return "Block{" +
                "hash='" + hash + '\'' +
                ", previousBlockHash='" + previousBlockHash + '\'' +
                ", merkleRoot='" + merkleRoot + '\'' +
                ", nonce=" + nonce +
                ", timestamp=" + timestamp +
                '}';
    }
}
