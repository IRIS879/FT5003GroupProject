package com.dao.service;

import com.dao.contract.ForumTreasury;
import com.dao.model.Proposal;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/**
 * On-chain execution layer: sends the GovernanceService results to the ForumTreasury contract.
 *
 * Notes for this course MVP:
 * - The contract transfers native ETH; the "USDC" amounts are mapped to Wei using a fixed demo ratio.
 */
public class BlockchainService {

    public static final String DEFAULT_RPC_URL = "http://127.0.0.1:8545";

    /**
     * Demo conversion ratio so that a 5,000 "USDC" budget fits into a small seeded treasury.
     * 1 USDC -> 1e12 Wei, so 5000 -> 5e15 Wei (0.005 ETH).
     */
    public static final BigInteger WEI_PER_USDC = BigInteger.TEN.pow(12);

    private final Web3j web3j;
    private final Credentials credentials;
    private final ForumTreasury treasury;

    public BlockchainService(String rpcUrl, String contractAddress, String adminPrivateKey) {
        Objects.requireNonNull(rpcUrl, "rpcUrl");
        Objects.requireNonNull(contractAddress, "contractAddress");
        Objects.requireNonNull(adminPrivateKey, "adminPrivateKey");

        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.credentials = Credentials.create(adminPrivateKey);
        this.treasury = ForumTreasury.load(contractAddress, web3j, credentials, new DefaultGasProvider());
    }

    public BigInteger getTreasuryBalanceWei() throws Exception {
        return treasury.getBalance().send();
    }

    public String getContractAdminAddress() throws Exception {
        return treasury.admin().send();
    }

    public String getCallerAddress() {
        return credentials.getAddress();
    }

    public String getContractAddress() {
        return treasury.getContractAddress();
    }

    public String seedTreasuryEth(BigDecimal ethAmount) throws Exception {
        Objects.requireNonNull(ethAmount, "ethAmount");
        return Transfer.sendFunds(web3j, credentials, treasury.getContractAddress(), ethAmount, Convert.Unit.ETHER)
                .send()
                .getTransactionHash();
    }

    /**
     * Execute a payout transaction based on winning proposals.
     *
     * @return transaction hash
     */
    public String executePayouts(List<Proposal> winningProposals) throws Exception {
        Objects.requireNonNull(winningProposals, "winningProposals");

        List<String> recipients = winningProposals.stream()
                .map(Proposal::getRecipientWalletAddress)
                .toList();

        List<BigInteger> amountsWei = winningProposals.stream()
                .map(p -> usdcToWei(p.getRequestedUsdc()))
                .toList();

        return treasury.executeTreasury(recipients, amountsWei).send().getTransactionHash();
    }

    public static BigInteger usdcToWei(int usdcAmount) {
        if (usdcAmount <= 0) {
            throw new IllegalArgumentException("usdcAmount must be > 0");
        }
        return WEI_PER_USDC.multiply(BigInteger.valueOf(usdcAmount));
    }
}
