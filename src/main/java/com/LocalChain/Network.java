package com.LocalChain;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.*;
import java.util.*;

public class Network {

    public static final int MIN_TRANSACTION = 1;
    public static final int MIN_DIFFICULTY = 5;
    public static int difficulty = MIN_DIFFICULTY;

    public void startNetwork() {
        try {
            createDB();
        } catch (SQLException exception) {
            System.out.println("DB Init Error -----");
            System.out.println(exception.getMessage());
        }

        List<Block> chain = new ArrayList<>();

        Block genesisBlock = new Block(null);
        Wallet wallet = new Wallet();
        saveKeysToDB(wallet.publicKey, wallet.privateKey);
        chain.add(genesisBlock);

        if (sendChainOverFakeNetwork(chain)) {
            System.out.println("Genesis block has been created.");
            System.out.println("Broadcasting to whoever may listen...");
            System.out.println("LocalChain is live (locally) !\n");
            System.out.println("Chain looks like this: \n");
            printLatestChain();
            System.out.println("\n\n");
        }
    }

    //aka broadcast to everyone, but faking it by saving it to a db and users fetch it constantly
    public static boolean sendChainOverFakeNetwork(List<Block> chain) {
        Gson gson = new Gson();
        String json = gson.toJson(chain);
        return saveChainToDB(json);
    }

    public static Block getLastBlockInChain() {
        List<Block> chain = getLatestChainFromFakeNetwork();

        return chain.get(chain.size() - 1);
    }

    //aka get longest chain for a specific user from db
    public static ArrayList<Block> getLatestChainFromFakeNetwork() {
        Gson gson = new Gson();
        String json = getLatestChainFromDB();
        return gson.fromJson(json, new TypeToken<ArrayList<Block>>() {}.getType());
    }

    //aka get rows of tx data from db
    public static Map<String, TxOutput> getTxOutputsFromFakeTxPool() {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM transaction_outputs");
            HashMap<String, TxOutput> UTXOs = new HashMap<>();
            while (rs.next()) {
                UTXOs.put(rs.getString(1), new TxOutput(
                        CryptoService.getPublicKeyFromString(rs.getString(2)),
                        rs.getFloat(3),
                        rs.getString(4)
                ));
            }

            return UTXOs;
        } catch (SQLException e) {
            System.out.println("ERR in getting TX from POOL");
            System.out.println(e.getMessage());
        }

        return new HashMap<>();
    }

    public static TxOutput getTxOutput(String txId) {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM transaction_outputs WHERE id = '" + txId + "'");
            if (rs.next()) {
                return new TxOutput(
                        CryptoService.getPublicKeyFromString(rs.getString(2)),
                        rs.getFloat(3),
                        rs.getString(4)
                );
            }
        } catch (SQLException e) {
            System.out.println("ERR getting TX");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public static boolean addTxOutput(TxOutput txOutput) {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            return stmt.execute("INSERT INTO transaction_outputs(id, publicKeyRecipient, value, parentTxId) VALUES('" +
                    txOutput.id + "','" + Base64.getEncoder().encodeToString(txOutput.recipient.getEncoded()) + "'," +
                    txOutput.value + ",'" + txOutput.parentTxId +
                    "')");
        } catch (SQLException e) {
            System.out.println("ERR in adding TX to DB");
            System.out.println(e.getMessage());
        }

        return false;
    }

    public static boolean removeTxOutput(String txId) {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            return stmt.execute("DELETE FROM transaction_outputs WHERE id = '" + txId + "'");
        } catch (SQLException e) {
            System.out.println("ERR in removing TX to DB");
            System.out.println(e.getMessage());
        }

        return true;
    }

    public static boolean addUncofirmedTxRecord(TxRecord txRecord) {
        Connection connection = getConnection();
        Gson gson = new Gson();
        try (Statement stmt = connection.createStatement()) {
            return stmt.execute("INSERT INTO transactions(id, publicKeySender, publicKeyRecipient, value, signature, inputs, confirmed)" +
                    "VALUES('" + txRecord.txId + "','" + Base64.getEncoder().encodeToString(txRecord.sender.getEncoded()) + "','" +
                            Base64.getEncoder().encodeToString(txRecord.recipient.getEncoded()) + "'," + txRecord.value + ",'" +
                            Base64.getEncoder().encodeToString(txRecord.signature) +
                            "','" + gson.toJson(txRecord.inputs) + "', 0" +
                    ")");
        } catch (SQLException e) {
            System.out.println("ERR in add unconfirmed Tx");
            System.out.println(e.getMessage());
        }

        return false;
    }

    public static boolean confirmTxRecord(String txId) {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            System.out.println("Confirmed: " + txId);
            return stmt.execute("UPDATE transactions SET confirmed = 1 WHERE id = '" + txId + "'");
        } catch (SQLException e) {
            System.out.println("ERR in confirmTxRecord");
            System.out.println(e.getMessage());
        }

        return false;
    }

    public static ArrayList<TxRecord> getUnconfirmedTxRecords() {
        Connection connection = getConnection();
        Gson gson = new Gson();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM transactions WHERE confirmed = 0 LIMIT 5");
            ArrayList<TxRecord> txRecords = new ArrayList<>();

            while (rs.next()) {
                System.out.println("Got unconfirmed: " + rs.getString(1));
                TxRecord txRecord = new TxRecord(
                        rs.getString(1),
                        CryptoService.getPublicKeyFromString(rs.getString(2)),
                        CryptoService.getPublicKeyFromString(rs.getString(3)),
                        rs.getFloat(4),
                        Base64.getDecoder().decode(rs.getString(5)),
                        gson.fromJson(rs.getString(6), new TypeToken<ArrayList<TxInput>>() {}.getType())
                );

                txRecords.add(txRecord);
            }

            return txRecords;
        } catch (SQLException e) {
            System.out.println("Error in getUnconfirmedTxRecords");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public static void printLatestChain() {
        getLatestChainFromFakeNetwork().forEach(block -> System.out.println(block.toString()));
    }

    public static void adaptDifficulty() {
        if (getMinerCount() > 2) {
            difficulty = MIN_DIFFICULTY + getMinerCount() / 2;
        }
    }

    public static int getMinerCount() {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT usersNo from user_count WHERE id = 1");
            if (rs.next()) {
                return rs.getInt(1);
            }

            return -1;
        } catch (SQLException e) {
            System.out.println("ERR printing user count");
            System.out.println(e.getMessage());
            return -1;
        }
    }

    public static boolean raiseMinerCount() {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("UPDATE user_count SET usersNo = (SELECT usersNo FROM user_count WHERE id = 1) + 1 WHERE id = 1;");
        } catch (SQLException e) {
            System.out.println("ERR failed to raise user count");
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    private static boolean saveChainToDB(String data) {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("UPDATE network SET data_json='" + data + "' WHERE id = 1");
        } catch (SQLException e) {
            System.out.println("SAVE ON DB ERROR");
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    public static boolean fku() {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM transactions WHERE confirmed = 1");
            if (rs.next()) {
                return false;
            }

            return true;
        } catch (SQLException e) {
            System.out.println("Err in noTransactionsRecorded");
            System.out.println(e.getMessage());
        }

        return true;
    }

    public static boolean noTransactionsRecorded() {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM transactions");
            if (rs.next()) {
                return false;
            }

            return true;
        } catch (SQLException e) {
            System.out.println("Err in noTransactionsRecorded");
            System.out.println(e.getMessage());
        }

        return true;
    }

    private static String getLatestChainFromDB() {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT data_json FROM network WHERE id = 1");
            if (rs.next()) {
                return rs.getString(1);
            }

            return null;
        } catch (SQLException exception) {
            System.out.println("GET FROM DB ERROR");
            System.out.println(exception.getMessage());
            return null;
        }
    }

    public static PublicKey getNetworkPublicKey() {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT networkPublicKey FROM network_keys WHERE id = 1");
            if (rs.next()) {
                return CryptoService.getPublicKeyFromString(rs.getString(1));
            }
        } catch (SQLException e) {
            System.out.println("ERR in get public key");
            System.out.println(e.getMessage());
        }

        return null;
    }

    public static PrivateKey getNetworkPrivateKey() {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT networkPrivateKey FROM network_keys WHERE id = 1");
            if (rs.next()) {
                return CryptoService.getPrivateKeyFromString(rs.getString(1));
            }
        } catch (SQLException e) {
            System.out.println("ERR in get private key");
            System.out.println(e.getMessage());
        }

        return null;
    }

    private static boolean saveKeysToDB(PublicKey publicKey, PrivateKey privateKey) {
        Connection connection = getConnection();
        try (Statement stmt = connection.createStatement()) {
            return stmt.execute("INSERT INTO network_keys(id, networkPublicKey, networkPrivateKey) VALUES(" +
                    "1," +
                    "'" + CryptoService.getStringFromKey(publicKey) + "'," +
                    "'" + CryptoService.getStringFromKey(privateKey) + "'" +
                    ")");
        } catch (SQLException e) {
            System.out.println("ERR in saveKeysToDB");
            System.out.println(e.getMessage());
        }

        return false;
    }

    private static void createDB() throws SQLException {
        Connection connection = getConnection();
        Statement stmt = connection.createStatement();
        stmt.execute("CREATE TABLE network (" +
                "id integer PRIMARY KEY, " +
                "data_json text NOT NULL " +
                ");");
        stmt.execute("CREATE TABLE user_count (id integer PRIMARY KEY, usersNo integer NOT NULL);");
        stmt.execute("CREATE TABLE transaction_outputs (" +
                "id text PRIMARY KEY," +
                "publicKeyRecipient text NOT NULL," +
                "value real NOT NULL," +
                "parentTxId text NOT NULL" +
                ");");

        stmt.execute("CREATE TABLE transactions(" +
                "id text PRIMARY KEY," +
                "publicKeySender text NOT NULL," +
                "publicKeyRecipient text NOT NULL," +
                "value real NOT NULL," +
                "signature text NOT NULL," +
                "inputs text NOT NULL," +
                "confirmed integer NOT NULL" +
                ");");

        stmt.execute("CREATE TABLE network_keys(" +
                "id integer PRIMARY KEY," +
                "networkPublicKey text NOT NULL," +
                "networkPrivateKey text NOT NULL" +
                ")");

        stmt.execute("INSERT INTO user_count(id, usersNo) VALUES (1, 0)");
        stmt.execute("INSERT INTO network(id, data_json) VALUES (1, 'no data')");
    }

    private static Connection getConnection() {
        String dbFilename = System.getProperty("user.dir") + "/localchain.db";

        try {
            return DriverManager.getConnection(
                    "jdbc:sqlite:" + dbFilename
            );
        } catch (SQLException e) {
            System.out.println("Could not connect to DB in NetworkService. Exiting...");
            System.out.println(e.getMessage());
            System.exit(-1);
        }

        return null;
    }
}
