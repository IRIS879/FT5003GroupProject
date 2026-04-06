package com.dao;

import com.dao.contract.ForumTreasury;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Infrastructure Layer Test: Validating Java connectivity with local Hardhat blockchain.
 */
public class BlockchainConnectionTest {

    // IMPORTANT: Replace with the address logged during deploy.js execution
    private final String CONTRACT_ADDRESS = "0x5FbDB2315678afecb367f032d93F642f64180aa3";
    
    // IMPORTANT: Use the Private Key from your 'npx hardhat node' output (Account #0)
    private final String ADMIN_PRIVATE_KEY = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";

    @Test
    public void testContractConnectivity() throws Exception {
        // 1. Connect to local Hardhat node
        Web3j web3j = Web3j.build(new HttpService("http://127.0.0.1:8545"));

        // 2. Load admin credentials
        Credentials credentials = Credentials.create(ADMIN_PRIVATE_KEY);

        // 3. Load the contract wrapper (Generated via Web3j CLI)
        ForumTreasury treasury = ForumTreasury.load(
                CONTRACT_ADDRESS, 
                web3j, 
                credentials, 
                new DefaultGasProvider()
        );

        // 4. Query on-chain data
        BigInteger balance = treasury.getBalance().send();
        String adminAddr = treasury.admin().send();

        System.out.println("Integration Test Result:");
        System.out.println(" - Treasury Balance: " + balance + " Wei");
        System.out.println(" - Contract Admin: " + adminAddr);

        // Assertions
        assertNotNull(adminAddr, "Admin address should not be null");
        System.out.println("Success: Connection to Smart Contract established.");
    }
}