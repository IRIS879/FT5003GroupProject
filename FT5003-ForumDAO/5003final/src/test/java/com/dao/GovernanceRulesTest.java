package com.dao;

import com.dao.model.Member;
import com.dao.model.Proposal;
import com.dao.service.GovernanceService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demo-scenario tests: the failure paths we want to show during the recorded walkthrough.
 *
 * Scenarios covered:
 *  - non-whitelisted submitter is rejected
 *  - non-whitelisted voter is rejected
 *  - self-vote (submitter voting on own proposal) is rejected
 *  - duplicate approval vote is rejected
 *  - quorum-not-reached → no winners even if a proposal is otherwise eligible
 *  - budget cap → proposal that doesn't fit the remaining budget is skipped
 */
public class GovernanceRulesTest {

    private static final Member A = new Member("Alice", "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266", true);
    private static final Member B = new Member("Bob",   "0x70997970c51812dc3a010c7d01b50e0d17dc79c8", true);
    private static final Member C = new Member("Carol", "0x3c44cdddb6a900fa2b585dd299e03d12fa4293bc", true);
    private static final Member D = new Member("Dave",  "0x90f79bf6eb2c4f870365e785982e1f101e93b906", true);
    private static final Member E = new Member("Eve",   "0x15d34aaf54267db7d7c367839aaf71a00a2c6a65", true);
    private static final Member P = new Member("Proposer", "0x9965507d1a55bcc2695c58ba16fb37d819b0a4dc", true);
    private static final Member OUTSIDER = new Member("Mallory", "0x1111111111111111111111111111111111111111", false);

    private static final String RECIPIENT_1 = "0x976ea74026e726554db657fa54763abd0c3a0aa9";
    private static final String RECIPIENT_2 = "0x14dc79964da2c08b23698b3d3cc7ca32193d9955";

    private GovernanceService freshService() {
        return new GovernanceService(List.of(A, B, C, D, E, P));
    }

    @Test
    public void submit_nonWhitelistedSubmitter_isRejected() {
        GovernanceService service = freshService();
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitProposal(OUTSIDER, "sneaky", RECIPIENT_1, 1000)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("whitelist"),
                "error should mention whitelist, got: " + ex.getMessage());
    }

    @Test
    public void vote_nonWhitelistedVoter_isRejected() {
        GovernanceService service = freshService();
        Proposal p = service.submitProposal(P, "legit", RECIPIENT_1, 1000);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.castApprovalVote(OUTSIDER, p.getId())
        );
        assertTrue(ex.getMessage().toLowerCase().contains("whitelist"));
    }

    @Test
    public void vote_selfVote_isRejected() {
        GovernanceService service = freshService();
        Proposal p = service.submitProposal(P, "proposer-owned", RECIPIENT_1, 1000);
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.castApprovalVote(P, p.getId())
        );
        assertTrue(ex.getMessage().toLowerCase().contains("self-vote"));
    }

    @Test
    public void vote_duplicateApproval_isRejected() {
        GovernanceService service = freshService();
        Proposal p = service.submitProposal(P, "legit", RECIPIENT_1, 1000);
        service.castApprovalVote(A, p.getId());
        assertThrows(IllegalStateException.class, () -> service.castApprovalVote(A, p.getId()));
    }

    @Test
    public void tally_quorumNotReached_producesNoWinners_evenIfThresholdMet() {
        GovernanceService service = freshService();
        Proposal p = service.submitProposal(P, "funding request", RECIPIENT_1, 1000);
        // 3 approvals -> threshold met, but only 3 unique voters -> quorum (5) NOT reached.
        service.castApprovalVote(A, p.getId());
        service.castApprovalVote(B, p.getId());
        service.castApprovalVote(C, p.getId());

        GovernanceService.TallyResult result = service.tallyRound();
        assertFalse(result.isQuorumReached(), "3 voters must not meet quorum of 5");
        assertEquals(1, result.getEligibleProposals().size(), "proposal is eligible on approvals alone");
        assertEquals(0, result.getWinningProposals().size(), "quorum failure must produce zero winners");
        assertEquals(GovernanceService.ROUND_BUDGET_USDC, result.getRemainingBudgetUsdc());
    }

    @Test
    public void tally_budgetInsufficient_skipsProposalThatDoesNotFit() {
        GovernanceService service = freshService();
        // Round budget is 5000 USDC.
        // p1 takes 4000, leaves 1000 remaining.
        // p2 requests 2000 — should be SKIPPED (doesn't fit), not partially funded.
        // p3 requests  800 — should still be funded after p2 is skipped.
        Proposal p1 = service.submitProposal(P, "big",   RECIPIENT_1, 4000);
        Proposal p2 = service.submitProposal(P, "mid",   RECIPIENT_2, 2000);
        Proposal p3 = service.submitProposal(P, "small", "0x23618e81e3f5cdf7f54c3d65f7fbc0abf5b21e8f", 800);

        // Everyone approves everything so all three are tied on approval count.
        // Rank then falls to submission order: p1 > p2 > p3.
        for (Member m : List.of(A, B, C, D, E)) {
            service.castApprovalVote(m, p1.getId());
            service.castApprovalVote(m, p2.getId());
            service.castApprovalVote(m, p3.getId());
        }

        GovernanceService.TallyResult result = service.tallyRound();
        assertTrue(result.isQuorumReached());
        assertEquals(3, result.getEligibleProposals().size());

        List<String> winnerIds = result.getWinningProposals().stream().map(Proposal::getId).toList();
        assertEquals(List.of(p1.getId(), p3.getId()), winnerIds,
                "p2 must be skipped because it doesn't fit remaining budget; p3 still fits");
        assertEquals(200, result.getRemainingBudgetUsdc());
    }
}
