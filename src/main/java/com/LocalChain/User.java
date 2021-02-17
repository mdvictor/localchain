package com.LocalChain;

import java.util.Scanner;

public class User {

    protected String name;
    protected Wallet wallet;

    public User(String name) {
        this.wallet = new Wallet();
        this.name = name;
    }

    public void startTrading(Scanner userInput) {
        while (true) {
            System.out.println("Your wallet ID is: " + CryptoService.getStringFromKey(this.wallet.publicKey));
            System.out.println("Balance: " + this.wallet.getBalance() + " LocalCoin");
            System.out.println("Do you wanna trade? (y/n)");
            String tradeSomething = userInput.next();
            userInput.nextLine();

            if (tradeSomething.equals("y")) {
                System.out.println("\nWho do you wanna send LC to?");
                String publicKeyIdentifier = userInput.nextLine();

                System.out.println("\nWhat amount of LC to send?");
                float val = userInput.nextFloat();
                userInput.nextLine();

                if (this.wallet.sendFunds(CryptoService.getPublicKeyFromString(publicKeyIdentifier),val)) {
                    System.out.println(this.name + " has sent funds to wallet ID " + publicKeyIdentifier);
                }
            }
        }
    }
}
