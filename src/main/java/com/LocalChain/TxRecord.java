package com.LocalChain;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Date;

public class TxRecord {

    public String txId;
    public PublicKey sender;
    public PublicKey recipient;
    public float value;
    public byte[] signature;

    public ArrayList<TxInput> inputs = new ArrayList<>();
    public ArrayList<TxOutput> outputs = new ArrayList<>();

    public TxRecord(PublicKey from, PublicKey to, float value, ArrayList<TxInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;

        this.txId = calculateHash();
    }

    public TxRecord(String id, PublicKey from, PublicKey to, float value, byte[] signature, ArrayList<TxInput> inputs) {
        this.txId = id;
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.signature = signature;
        this.inputs = inputs;
    }

    public boolean processTx() {
        PublicKey networkKey = Network.getNetworkPublicKey();

        if (networkKey != null && networkKey.equals(this.sender)) {
            outputs.add(new TxOutput( this.recipient, value, txId));

            for(TxOutput o : outputs) {
                Network.addTxOutput(o);
            }

            return true;
        }

        if(!verifySignature()) {
            System.out.println("Transaction Signature failed to verify");
            return false;
        }

        //Gathers transaction inputs (Making sure they are unspent):
        for(TxInput i : inputs) {
            i.UTXO = Network.getTxOutput(i.txOutputId);
        }

        if (getInputsValue() < value) {
            System.out.println("Insufficient funds: " + getInputsValue());
            return false;
        }

        //Checks if transaction is valid:
        if(getInputsValue() < Network.MIN_TRANSACTION) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            System.out.println("Please enter the amount greater than " + Network.MIN_TRANSACTION);
            return false;
        }

        //Generate transaction outputs:
        float leftOver = getInputsValue() - value; //get value of inputs then the left over change:
        outputs.add(new TxOutput( this.recipient, value, txId)); //send value to recipient
        outputs.add(new TxOutput( this.sender, leftOver, txId)); //send the left over 'change' back to sender

        //Add outputs to Unspent list
        for(TxOutput o : outputs) {
            Network.addTxOutput(o);
        }

        //Remove transaction inputs from UTXO lists as spent:
        for(TxInput i : inputs) {
            if(i.UTXO == null) continue; //if Transaction can't be found skip it
            if (!Network.removeTxOutput(i.UTXO.id)) {
//                System.out.println("OH NO! It didn't delete the spent input with id: " + i.UTXO.id);
            }
        }

        return true;
    }

    public float getInputsValue() {
        float total = 0;
        for(TxInput i : inputs) {
            if(i.UTXO == null) continue; //if Transaction can't be found skip it, This behavior may not be optimal.
            total += i.UTXO.value;
        }
        return total;
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = CryptoService.getStringFromKey(sender) + CryptoService.getStringFromKey(recipient) + value;
        signature = CryptoService.applyECDSASig(privateKey, data);
    }

    public boolean verifySignature() {
        String data = CryptoService.getStringFromKey(sender) + CryptoService.getStringFromKey(recipient) + value;
        return CryptoService.verifyECDSASig(sender, data, signature);
    }

    private String calculateHash() {
        return CryptoService.applySha256(
                CryptoService.getStringFromKey(sender) +
                        CryptoService.getStringFromKey(recipient) +
                        value +
                        new Date().getTime()
        );
    }
}
