package com.dao.service;

import com.dao.model.Member;
import com.dao.model.Proposal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Pure off-chain governance engine.
 *
 * Hard rules (FT5003):
 * - Only whitelisted members can vote.
 * - Quorum: at least 5 unique voters participated in the round.
 * - Threshold: proposal needs at least 3 approvals to be eligible.
 * - Ranking: approvals desc, then submittedAt asc.
 * - Budget cap: within a single round, total funded amount must not exceed 5000 USDC.
 */
public class GovernanceService {

    public static final int QUORUM_UNIQUE_VOTERS = 5;
    public static final int APPROVAL_THRESHOLD = 3;
    public static final int ROUND_BUDGET_USDC = 5000;

    private final Set<String> whitelistedWalletAddresses = new HashSet<>();
    private final Map<String, Proposal> proposalsById = new LinkedHashMap<>();
    private long submissionCounter = 0;

    public GovernanceService(List<Member> initialWhitelist) {
        Objects.requireNonNull(initialWhitelist, "initialWhitelist");
        for (Member member : initialWhitelist) {
            if (member.isWhitelisted()) {
                whitelistedWalletAddresses.add(normalizeAddress(member.getWalletAddress()));
            }
        }
    }

    public Proposal submitProposal(String title, String recipientWalletAddress, int requestedUsdc) {
        validateProposal(title, recipientWalletAddress, requestedUsdc);
        String id = UUID.randomUUID().toString();
        long order = ++submissionCounter;
        Proposal proposal = new Proposal(
                id,
                title.trim(),
                normalizeAddress(recipientWalletAddress),
                requestedUsdc,
                Instant.now(),
                order
        );
        proposalsById.put(id, proposal);
        return proposal;
    }

    public Optional<Proposal> getProposal(String proposalId) {
        return Optional.ofNullable(proposalsById.get(proposalId));
    }

    public List<Proposal> listProposals() {
        return new ArrayList<>(proposalsById.values());
    }

    public void castApprovalVote(Member member, String proposalId) {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(proposalId, "proposalId");

        String voterAddr = normalizeAddress(member.getWalletAddress());
        if (!whitelistedWalletAddresses.contains(voterAddr)) {
            throw new IllegalArgumentException("Voter is not whitelisted: " + voterAddr);
        }

        Proposal proposal = proposalsById.get(proposalId);
        if (proposal == null) {
            throw new IllegalArgumentException("Unknown proposalId: " + proposalId);
        }

        boolean added = proposal.addApprovalVoterAddress(voterAddr);
        if (!added) {
            throw new IllegalStateException("Duplicate approval vote: voter=" + voterAddr + " proposalId=" + proposalId);
        }
    }

    public TallyResult tallyRound() {
        Set<String> uniqueVoters = new HashSet<>();
        for (Proposal proposal : proposalsById.values()) {
            uniqueVoters.addAll(proposal.getApprovalVoterAddresses());
        }

        boolean quorumReached = uniqueVoters.size() >= QUORUM_UNIQUE_VOTERS;

        List<Proposal> eligible = new ArrayList<>();
        for (Proposal proposal : proposalsById.values()) {
            if (proposal.getApprovalCount() >= APPROVAL_THRESHOLD) {
                eligible.add(proposal);
            }
        }

        eligible.sort(
                Comparator.comparingInt(Proposal::getApprovalCount).reversed()
                        .thenComparing(Proposal::getSubmittedAt)
                        .thenComparingLong(Proposal::getSubmissionOrder)
                        .thenComparing(Proposal::getId)
        );

        int remainingBudget = ROUND_BUDGET_USDC;
        List<Proposal> winners = new ArrayList<>();
        if (quorumReached) {
            for (Proposal proposal : eligible) {
                if (proposal.getRequestedUsdc() <= remainingBudget) {
                    winners.add(proposal);
                    remainingBudget -= proposal.getRequestedUsdc();
                }
            }
        }

        return new TallyResult(
                uniqueVoters.size(),
                quorumReached,
                Collections.unmodifiableList(eligible),
                Collections.unmodifiableList(winners),
                remainingBudget
        );
    }

    private static void validateProposal(String title, String recipientWalletAddress, int requestedUsdc) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (!isValidAddress(recipientWalletAddress)) {
            throw new IllegalArgumentException("Invalid recipient wallet address: " + recipientWalletAddress);
        }
        if (requestedUsdc <= 0) {
            throw new IllegalArgumentException("Requested USDC must be > 0");
        }
        if (requestedUsdc > ROUND_BUDGET_USDC) {
            throw new IllegalArgumentException("Requested USDC exceeds round budget (" + ROUND_BUDGET_USDC + "): " + requestedUsdc);
        }
    }

    private static boolean isValidAddress(String address) {
        if (address == null) {
            return false;
        }
        String a = address.trim();
        if (!a.startsWith("0x") || a.length() != 42) {
            return false;
        }
        for (int i = 2; i < a.length(); i++) {
            char c = a.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException("Address must not be null");
        }
        return address.trim().toLowerCase();
    }

    public static class TallyResult {
        private final int uniqueVoterCount;
        private final boolean quorumReached;
        private final List<Proposal> eligibleProposals;
        private final List<Proposal> winningProposals;
        private final int remainingBudgetUsdc;

        public TallyResult(
                int uniqueVoterCount,
                boolean quorumReached,
                List<Proposal> eligibleProposals,
                List<Proposal> winningProposals,
                int remainingBudgetUsdc
        ) {
            this.uniqueVoterCount = uniqueVoterCount;
            this.quorumReached = quorumReached;
            this.eligibleProposals = eligibleProposals;
            this.winningProposals = winningProposals;
            this.remainingBudgetUsdc = remainingBudgetUsdc;
        }

        public int getUniqueVoterCount() {
            return uniqueVoterCount;
        }

        public boolean isQuorumReached() {
            return quorumReached;
        }

        public List<Proposal> getEligibleProposals() {
            return eligibleProposals;
        }

        public List<Proposal> getWinningProposals() {
            return winningProposals;
        }

        public int getRemainingBudgetUsdc() {
            return remainingBudgetUsdc;
        }
    }
}
