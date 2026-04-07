package com.dao.model;

import lombok.Data;
import java.math.BigInteger;

@Data
public class Proposal {
    private String id;
    private String proposerAddress;
    private String recipientAddress;
    private BigInteger requestAmount;
    private long submissionTimestamp;
    private int approvalVotes;
    private ProposalStatus status;

    public enum ProposalStatus {
        SUBMITTED, VOTING, APPROVED, REJECTED, EXECUTED
    }

    public Proposal(String id, String proposerAddress, String recipientAddress, BigInteger requestAmount) {
        this.id = id;
        this.proposerAddress = proposerAddress;
        this.recipientAddress = recipientAddress;
        this.requestAmount = requestAmount;
        this.submissionTimestamp = System.currentTimeMillis();
        this.approvalVotes = 0;
        this.status = ProposalStatus.SUBMITTED;
    }
}
