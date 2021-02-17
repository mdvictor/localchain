package com.LocalChain;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Wallet {

    public PublicKey publicKey;
    public PrivateKey privateKey;

    public HashMap<String, TxOutput> UTXOs = new HashMap<>();

    public Wallet() {
        generateKeyPairValues();
    }

    private void generateKeyPairValues() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(ecSpec, random); //256
            KeyPair keyPair = keyGen.generateKeyPair();
            // Set the public and private keys from the keyPair
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public float getBalance() {
        float total = 0;
        for (Map.Entry<String, TxOutput> item: Network.getTxOutputsFromFakeTxPool().entrySet()){
            TxOutput UTXO = item.getValue();
            if(UTXO.isMine(publicKey)) { //if output belongs to me ( if coins belong to me )
                UTXOs.put(UTXO.id,UTXO); //add it to our list of unspent transactions.
                total += UTXO.value ;
            }
        }
        return total;
    }

    public boolean sendFunds(PublicKey _recipient, float value ) {
        if(getBalance() < value) {
            System.out.println("Not enough funds to send transaction. Transaction Discarded.");
            return false;
        }
        ArrayList<TxInput> inputs = new ArrayList<>();

        float total = 0;
        for (Map.Entry<String, TxOutput> item: UTXOs.entrySet()){
            TxOutput UTXO = item.getValue();
            total += UTXO.value;
            inputs.add(new TxInput(UTXO.id));
            if(total > value) break;
        }

        TxRecord newTransaction = new TxRecord(publicKey, _recipient , value, inputs);
        newTransaction.generateSignature(privateKey);
        Network.addUncofirmedTxRecord(newTransaction);

        for(TxInput input: inputs){
            UTXOs.remove(input.txOutputId);
        }

        return true;
    }
}
