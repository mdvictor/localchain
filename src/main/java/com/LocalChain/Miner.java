package com.LocalChain;

import java.util.ArrayList;
import java.util.List;

public class Miner extends User{

    private List<Block> localChain;
    private final MineUtils pickaxe;

    public Miner(String name) {
        super(name);

        localChain = Network.getLatestChainFromFakeNetwork();
        pickaxe = new MineUtils();

        Network.raiseMinerCount();
    }

    public void startMining() {
        List<Block> networkChain;

        while(true) {
            networkChain = Network.getLatestChainFromFakeNetwork();
            if (localChain.size() < networkChain.size()) {
                localChain = networkChain;
            }

            Network.adaptDifficulty();

            Block lastBlock = Network.getLastBlockInChain();
            System.out.println(lastBlock.toString());
            if (!lastBlock.isMined()) {
                Block solvedBlock = pickaxe.mine(lastBlock, Network.difficulty);

                localChain.get(localChain.size() - 1).transactionIds.forEach(Network::confirmTxRecord);
                localChain.get(localChain.size() - 1).transactionIds = null;
                localChain.get(localChain.size() - 1).nonce = solvedBlock.nonce;
                localChain.get(localChain.size() - 1).hash = solvedBlock.hash;

                Block block = new Block(localChain.get(localChain.size() - 1).hash);
                ArrayList<TxRecord> txRecords = Network.getUnconfirmedTxRecords();
                if (txRecords != null) {
                    txRecords.forEach(block::addTx);
                }
                block.merkleRoot = CryptoService.getMerkleRoot(block.transactions);
                //transactions need not stay on block. only merkleRoot
                block.transactions = null;

                localChain.add(block);

                networkChain = Network.getLatestChainFromFakeNetwork();
                if (localChain.size() < networkChain.size()) {
                    localChain = networkChain;
                    System.out.println("My chain is smaller than network chain, switching to that one...\n\n");
                } else {
                    Network.sendChainOverFakeNetwork(localChain);
                }
            }
        }
    }
}
