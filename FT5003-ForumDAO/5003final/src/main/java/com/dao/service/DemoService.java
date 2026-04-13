package com.dao.service;

import com.dao.model.Member;
import com.dao.model.Proposal;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stateful demo facade: holds the in-memory round and proxies to {@link BlockchainService}
 * for on-chain execution. Exposes coarse actions matching the frontend buttons.
 */
@Service
public class DemoService {

    public static final List<Member> SEED_MEMBERS = List.of(
            new Member("Alice",    "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266", true),
            new Member("Bob",      "0x70997970c51812dc3a010c7d01b50e0d17dc79c8", true),
            new Member("Carol",    "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc", true),
            new Member("Dave",     "0x90f79bf6eb2c4f870365e785982e1f101e93b906", true),
            new Member("Eve",      "0x15d34aaf54267db7d7c367839aaf71a00a2c6a65", true),
            new Member("Proposer", "0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc", true),
            // Mallory is intentionally NOT whitelisted — used to show the failure path.
            new Member("Mallory",  "0x1111111111111111111111111111111111111111", false)
    );

    private final Map<String, Member> membersByAddress = new LinkedHashMap<>();
    private GovernanceService governance;

    private final String rpcUrl;
    private final String contractAddress;
    private final String adminPrivateKey;

    public DemoService() {
        for (Member m : SEED_MEMBERS) {
            membersByAddress.put(m.getWalletAddress().toLowerCase(Locale.ROOT), m);
        }
        this.governance = new GovernanceService(SEED_MEMBERS);

        this.rpcUrl = env("DAO_RPC_URL", BlockchainService.DEFAULT_RPC_URL);
        this.contractAddress = env("DAO_CONTRACT_ADDRESS", "0x5FbDB2315678afecb367f032d93F642f64180aa3");
        this.adminPrivateKey = env("DAO_ADMIN_PRIVATE_KEY",
                "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80");
    }

    public synchronized List<Member> listMembers() {
        return List.copyOf(membersByAddress.values());
    }

    public synchronized List<Proposal> listProposals() {
        return governance.listProposals();
    }

    public synchronized GovernanceService.TallyResult tally() {
        return governance.tallyRound();
    }

    public synchronized Proposal submit(String submitterAddress, String title, String recipient, int amountUsdc) {
        Member submitter = requireMember(submitterAddress);
        return governance.submitProposal(submitter, title, recipient, amountUsdc);
    }

    public synchronized void vote(String voterAddress, String proposalId) {
        Member voter = requireMember(voterAddress);
        governance.castApprovalVote(voter, proposalId);
    }

    public synchronized void resetRound() {
        this.governance = new GovernanceService(SEED_MEMBERS);
    }

    /**
     * Executes the winning proposals on-chain via ForumTreasury. Returns a summary so the UI
     * can show before/after balances and the tx hash.
     */
    public synchronized ExecuteSummary execute() throws Exception {
        GovernanceService.TallyResult result = governance.tallyRound();
        if (!result.isQuorumReached()) {
            throw new IllegalStateException("Quorum not reached: " + result.getUniqueVoterCount()
                    + " of " + GovernanceService.QUORUM_UNIQUE_VOTERS + " required voters");
        }
        if (result.getWinningProposals().isEmpty()) {
            throw new IllegalStateException("No winning proposals to execute");
        }

        BlockchainService chain = new BlockchainService(rpcUrl, contractAddress, adminPrivateKey);
        BigInteger before = chain.getTreasuryBalanceWei();

        BigInteger totalWei = BigInteger.ZERO;
        for (Proposal p : result.getWinningProposals()) {
            totalWei = totalWei.add(BlockchainService.usdcToWei(p.getRequestedUsdc()));
        }

        String seedTxHash = null;
        if (before.compareTo(totalWei) < 0) {
            seedTxHash = chain.seedTreasuryEth(new BigDecimal("0.01"));
            before = chain.getTreasuryBalanceWei();
        }

        String txHash = chain.executePayouts(result.getWinningProposals());
        BigInteger after = chain.getTreasuryBalanceWei();
        return new ExecuteSummary(txHash, seedTxHash, before, after, result.getWinningProposals());
    }

    public synchronized TreasurySnapshot snapshotTreasury() {
        try {
            BlockchainService chain = new BlockchainService(rpcUrl, contractAddress, adminPrivateKey);
            BigInteger balance = chain.getTreasuryBalanceWei();
            String adminAddr = chain.getContractAdminAddress();
            return new TreasurySnapshot(true, contractAddress, adminAddr, balance, null);
        } catch (Exception e) {
            return new TreasurySnapshot(false, contractAddress, null, null, rootMessage(e));
        }
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    private Member requireMember(String address) {
        Objects.requireNonNull(address, "address");
        Member m = membersByAddress.get(address.trim().toLowerCase(Locale.ROOT));
        if (m == null) {
            throw new IllegalArgumentException("Unknown member address: " + address);
        }
        return m;
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v.trim();
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return Optional.ofNullable(cur.getMessage()).orElse(cur.getClass().getSimpleName());
    }

    public record ExecuteSummary(
            String txHash,
            String seedTxHash,
            BigInteger balanceBeforeWei,
            BigInteger balanceAfterWei,
            List<Proposal> winningProposals
    ) {}

    public record TreasurySnapshot(
            boolean available,
            String contractAddress,
            String adminAddress,
            BigInteger balanceWei,
            String error
    ) {}
}
