package com.LocalChain;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Network network;
        Scanner userInput = new Scanner(System.in);
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        File f = new File(System.getProperty("user.dir") + "/localchain.db");
        if (!f.exists()) {
            System.out.println("Starting the network!");
            network = new Network();
            network.startNetwork();

            System.out.println("\n\nPrint the current longest chain? (just press whatever, doesn't matter)");
            String answer = userInput.next();

            while (answer != null) {
                System.out.println("CHAIN:");
                Network.printLatestChain();
                System.out.println("MINER COUNT:");
                System.out.println(Network.getMinerCount());
                System.out.println("\n\n");

                System.out.println("\n\nPrint the current longest chain? (just press whatever, doesn't matter)");
                answer = userInput.next();
            }
        } else {
            System.out.println("What do you wish to be?");
            System.out.println("(1) Miner");
            System.out.println("(2) Trader");

            int answer = userInput.nextInt();
            userInput.nextLine();

            System.out.println("Give a name to your user...");
            String userName = userInput.nextLine();

            switch (answer) {
                case 1:
                    System.out.println("Joining network as a miner!");
                    Miner miner = new Miner(userName);
                    miner.startMining();
                    break;
                case 2:
                    System.out.println("Joining network as a normal trading user!");
                    User trader = new User(userName);

                    if (Network.noTransactionsRecorded()) {
                        PublicKey pubKey = Network.getNetworkPublicKey();
                        PrivateKey privKey = Network.getNetworkPrivateKey();

                        TxRecord newTransaction = new TxRecord(pubKey, trader.wallet.publicKey, 1000, null);
                        newTransaction.generateSignature(privKey);
                        Network.addUncofirmedTxRecord(newTransaction);
                    }

                    trader.startTrading(userInput);
                    break;
            }
        }
    }
}
