package com.LocalChain;

public class MineUtils {

    public Block mine(Block block, int difficulty) {
        System.out.println("Starting mining:\n");
        int nonce = 0;

        String hash;
        String target = getDificultyString(difficulty);
        if (block.hash == null) {
            block.nonce = nonce;
            hash = calculateHash(block);
            block.hash = hash;
        }

        while(!block.hash.substring( 0, difficulty).equals(target)) {
            nonce++;
            block.nonce = nonce;
            hash = calculateHash(block);
            block.hash = hash;
        }

        System.out.println("Block Mined!!!: " + block.hash + "  NONCE:" + block.nonce);
        return block;
    }

    private String calculateHash(Block block) {
        return CryptoService.applySha256(
            block.previousBlockHash +
            Long.toString(block.timestamp) +
            block.merkleRoot +
            Integer.toString(block.nonce)
        );
    }

    private String getDificultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }
}
