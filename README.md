to install:
mvn clean compile assembly:single

Simple, hacky toy blockchain implementation.

Thank you @CryptoKass for the knowledge and code bits - https://medium.com/programmers-blockchain/blockchain-development-mega-guide-5a316e6d10df#bba8 / https://github.com/CryptoKass/NoobChain-Tutorial-Part-2

You compile it and run multiple instances of the program in your console.
Follow terminal text to figure out things.

First instance run will start the network itself.
Consecutive instances will be ask you for input on what you want to add, be it a miner of a trader.

Adding more miners won't do much since all miners have the same processing power and start with nonce = 0;
So the first miner will always be the top one with the longest chain. (Might be different if two miners are started at roughly the same time)

Each trader will have its wallet PublicKey posted on the terminal. To send LocalCoins from one Trader to another, you copy the receivers Public key and paste it in the terminal where it asks for it

For now, there is no miner reward for mining a block, neither is there a reward based on transaction fees. Miners do the work out of the goodness of their hearts.

Coins are generated once, in a Genesis Transaction that happens when the first Trader joins the network. When that happens, 1000LC is being transfered to this trader.
