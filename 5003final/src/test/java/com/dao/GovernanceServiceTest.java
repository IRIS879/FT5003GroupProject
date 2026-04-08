package com.dao;

import com.dao.model.Member;
import com.dao.model.Proposal;
import com.dao.service.GovernanceService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GovernanceServiceTest {

    @Test
    public void tallyRound_quorumNotReached_noWinners() {
        GovernanceService service = new GovernanceService(List.of(
                new Member("A", "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266", true),
                new Member("B", "0x70997970c51812dc3a010c7d01b50e0d17dc79c8", true),
                new Member("C", "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc", true),
                new Member("D", "0x90f79bf6eb2c4f870365e785982e1f101e93b906", true)
        ));

        Proposal p1 = service.submitProposal("P1", "0x15d34aaf54267db7d7c367839aaf71a00a2c6a65", 1000);

        service.castApprovalVote(new Member("A", "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266", true), p1.getId());
        service.castApprovalVote(new Member("B", "0x70997970c51812dc3a010c7d01b50e0d17dc79c8", true), p1.getId());
        service.castApprovalVote(new Member("C", "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc", true), p1.getId());

        GovernanceService.TallyResult result = service.tallyRound();
        assertFalse(result.isQuorumReached());
        assertEquals(3, result.getUniqueVoterCount());
        assertEquals(1, result.getEligibleProposals().size());
        assertEquals(0, result.getWinningProposals().size());
        assertEquals(GovernanceService.ROUND_BUDGET_USDC, result.getRemainingBudgetUsdc());
    }

    @Test
    public void tallyRound_sortAndBudgetCutoff() {
        Member m0 = new Member("M0", "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266", true);
        Member m1 = new Member("M1", "0x70997970c51812dc3a010c7d01b50e0d17dc79c8", true);
        Member m2 = new Member("M2", "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc", true);
        Member m3 = new Member("M3", "0x90f79bf6eb2c4f870365e785982e1f101e93b906", true);
        Member m4 = new Member("M4", "0x15d34aaf54267db7d7c367839aaf71a00a2c6a65", true);

        GovernanceService service = new GovernanceService(List.of(m0, m1, m2, m3, m4));

        Proposal p1 = service.submitProposal("P1", "0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc", 3000);
        Proposal p2 = service.submitProposal("P2", "0x976ea74026e726554db657fa54763abd0c3a0aa9", 2500);
        Proposal p3 = service.submitProposal("P3", "0x14dc79964da2c08b23698b3d3cc7ca32193d9955", 2000);

        // Votes:
        // p1: 4 approvals
        service.castApprovalVote(m0, p1.getId());
        service.castApprovalVote(m1, p1.getId());
        service.castApprovalVote(m2, p1.getId());
        service.castApprovalVote(m3, p1.getId());

        // p2: 3 approvals (eligible)
        service.castApprovalVote(m0, p2.getId());
        service.castApprovalVote(m1, p2.getId());
        service.castApprovalVote(m4, p2.getId());

        // p3: 3 approvals (eligible) submitted after p2, should rank after p2 on tie
        service.castApprovalVote(m2, p3.getId());
        service.castApprovalVote(m3, p3.getId());
        service.castApprovalVote(m4, p3.getId());

        GovernanceService.TallyResult result = service.tallyRound();
        assertTrue(result.isQuorumReached());
        assertEquals(5, result.getUniqueVoterCount());

        // Eligible ranked: p1 (4), then p2 (3), then p3 (3 but later submission)
        assertEquals(List.of(p1.getId(), p2.getId(), p3.getId()),
                result.getEligibleProposals().stream().map(Proposal::getId).toList());

        // Budget 5000: fund p1 (3000), remaining 2000; p2 (2500) doesn't fit, skip; p3 (2000) fits.
        assertEquals(List.of(p1.getId(), p3.getId()),
                result.getWinningProposals().stream().map(Proposal::getId).toList());
        assertEquals(0, result.getRemainingBudgetUsdc());
    }
}
