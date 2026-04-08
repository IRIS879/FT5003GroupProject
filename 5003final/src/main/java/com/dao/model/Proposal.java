package com.dao.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class Proposal {
    private final String id;
    private final String title;
    private final String recipientWalletAddress;
    private final int requestedUsdc;
    private final Instant submittedAt;
    private final long submissionOrder;
    private final Set<String> approvalVoterAddresses = new LinkedHashSet<>();

    public Proposal(
            String id,
            String title,
            String recipientWalletAddress,
            int requestedUsdc,
            Instant submittedAt,
            long submissionOrder
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.recipientWalletAddress = Objects.requireNonNull(recipientWalletAddress, "recipientWalletAddress");
        this.requestedUsdc = requestedUsdc;
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
        this.submissionOrder = submissionOrder;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getRecipientWalletAddress() {
        return recipientWalletAddress;
    }

    public int getRequestedUsdc() {
        return requestedUsdc;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public long getSubmissionOrder() {
        return submissionOrder;
    }

    public int getApprovalCount() {
        return approvalVoterAddresses.size();
    }

    public Set<String> getApprovalVoterAddresses() {
        return Collections.unmodifiableSet(approvalVoterAddresses);
    }

    public boolean addApprovalVoterAddress(String voterWalletAddress) {
        return approvalVoterAddresses.add(voterWalletAddress);
    }
}
