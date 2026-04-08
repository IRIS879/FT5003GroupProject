package com.dao;

import com.dao.model.Member;
import com.dao.model.Proposal;
import com.dao.service.BlockchainService;
import com.dao.service.GovernanceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@SpringBootApplication
public class DaoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DaoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==================================================");
        System.out.println("Forum Builder Fund DAO - MVP Demo");
        System.out.println("Flow: Submit -> Vote -> Tally -> Execute On-Chain");
        System.out.println("==================================================");

        String rpcUrl = env("DAO_RPC_URL", BlockchainService.DEFAULT_RPC_URL);
        String contractAddress = env("DAO_CONTRACT_ADDRESS", "0x5FbDB2315678afecb367f032d93F642f64180aa3");
        String adminPrivateKey = env("DAO_ADMIN_PRIVATE_KEY",
                "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80");

        System.out.println("[Config]");
        System.out.println(" - RPC: " + rpcUrl);
        System.out.println(" - Treasury: " + contractAddress);

        Member m0 = new Member("Alice", "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266", true);
        Member m1 = new Member("Bob", "0x70997970c51812dc3a010c7d01b50e0d17dc79c8", true);
        Member m2 = new Member("Carol", "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc", true);
        Member m3 = new Member("Dave", "0x90f79bf6eb2c4f870365e785982e1f101e93b906", true);
        Member m4 = new Member("Eve", "0x15d34aaf54267db7d7c367839aaf71a00a2c6a65", true);

        GovernanceService governance = new GovernanceService(List.of(m0, m1, m2, m3, m4));

        System.out.println();
        System.out.println("[1] Submit Proposals");
        Proposal p1 = governance.submitProposal("Build high-quality forum moderation tools",
                "0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc", 3000);
        Proposal p2 = governance.submitProposal("Community education workshop series",
                "0x976ea74026e726554db657fa54763abd0c3a0aa9", 2500);
        Proposal p3 = governance.submitProposal("Security review for key backend modules",
                "0x14dc79964da2c08b23698b3d3cc7ca32193d9955", 2000);

        printProposal(p1);
        printProposal(p2);
        printProposal(p3);

        System.out.println();
        System.out.println("[2] Cast Approval Votes (Whitelist Only)");
        // quorum = 5 unique voters, threshold = 3 approvals
        governance.castApprovalVote(m0, p1.getId());
        governance.castApprovalVote(m1, p1.getId());
        governance.castApprovalVote(m2, p1.getId());
        governance.castApprovalVote(m3, p1.getId());

        governance.castApprovalVote(m0, p2.getId());
        governance.castApprovalVote(m1, p2.getId());
        governance.castApprovalVote(m4, p2.getId());

        governance.castApprovalVote(m2, p3.getId());
        governance.castApprovalVote(m3, p3.getId());
        governance.castApprovalVote(m4, p3.getId());

        System.out.println(" - Votes recorded.");

        System.out.println();
        System.out.println("[3] Tally Round (Quorum + Threshold + Budget Cutoff)");
        GovernanceService.TallyResult tally = governance.tallyRound();
        System.out.println(" - Unique voters participated: " + tally.getUniqueVoterCount()
                + " (quorum=" + GovernanceService.QUORUM_UNIQUE_VOTERS + ")");
        System.out.println(" - Quorum reached: " + tally.isQuorumReached());
        System.out.println(" - Eligible proposals (>= " + GovernanceService.APPROVAL_THRESHOLD + " approvals):");
        for (Proposal p : tally.getEligibleProposals()) {
            System.out.println("   * " + p.getTitle() + " | approvals=" + p.getApprovalCount()
                    + " | requested=" + p.getRequestedUsdc() + " USDC");
        }

        System.out.println(" - Winners within budget " + GovernanceService.ROUND_BUDGET_USDC + " USDC:");
        for (Proposal p : tally.getWinningProposals()) {
            System.out.println("   * " + p.getTitle() + " -> " + p.getRecipientWalletAddress()
                    + " | amount=" + p.getRequestedUsdc() + " USDC");
        }
        System.out.println(" - Remaining budget: " + tally.getRemainingBudgetUsdc() + " USDC");

        System.out.println();
        System.out.println("[4] Execute On-Chain Treasury Payouts");
        BlockchainService chain = new BlockchainService(rpcUrl, contractAddress, adminPrivateKey);
        BigInteger beforeWei = chain.getTreasuryBalanceWei();
        System.out.println(" - Contract admin: " + chain.getContractAdminAddress());
        System.out.println(" - Caller (admin key): " + chain.getCallerAddress());
        System.out.println(" - Treasury balance before: " + beforeWei + " Wei");

        BigInteger totalWei = tally.getWinningProposals().stream()
                .map(p -> BlockchainService.usdcToWei(p.getRequestedUsdc()))
                .reduce(BigInteger.ZERO, BigInteger::add);

        if (beforeWei.compareTo(totalWei) < 0) {
            System.out.println(" - WARNING: Treasury balance < total payout amount (" + totalWei + " Wei).");
            System.out.println("   Seeding treasury with 0.01 ETH from admin...");
            String fundTx = chain.seedTreasuryEth(new BigDecimal("0.01"));
            System.out.println("   Seed txHash = " + fundTx);
            beforeWei = chain.getTreasuryBalanceWei();
            System.out.println(" - Treasury balance after seed: " + beforeWei + " Wei");
        }

        if (tally.getWinningProposals().isEmpty()) {
            System.out.println(" - No winners. Nothing to execute.");
            return;
        }

        String txHash = chain.executePayouts(tally.getWinningProposals());
        System.out.println(" - SUCCESS: executeTreasury txHash = " + txHash);

        BigInteger afterWei = chain.getTreasuryBalanceWei();
        System.out.println(" - Treasury balance after: " + afterWei + " Wei");
        System.out.println("==================================================");
    }

    private static void printProposal(Proposal p) {
        System.out.println(" - [" + p.getId() + "] " + p.getTitle()
                + " -> recipient=" + p.getRecipientWalletAddress()
                + " amount=" + p.getRequestedUsdc() + " USDC");
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v.trim();
    }
}
