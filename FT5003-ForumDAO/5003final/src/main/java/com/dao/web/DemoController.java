package com.dao.web;

import com.dao.model.Member;
import com.dao.model.Proposal;
import com.dao.service.DemoService;
import com.dao.service.GovernanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    private final DemoService demo;

    public DemoController(DemoService demo) {
        this.demo = demo;
    }

    @GetMapping("/state")
    public Map<String, Object> state() {
        GovernanceService.TallyResult tally = demo.tally();
        return Map.of(
                "constants", Map.of(
                        "quorum", GovernanceService.QUORUM_UNIQUE_VOTERS,
                        "threshold", GovernanceService.APPROVAL_THRESHOLD,
                        "budgetUsdc", GovernanceService.ROUND_BUDGET_USDC
                ),
                "members", demo.listMembers().stream().map(DemoController::memberJson).toList(),
                "proposals", demo.listProposals().stream().map(DemoController::proposalJson).toList(),
                "tally", tallyJson(tally),
                "treasury", treasuryJson(demo.snapshotTreasury())
        );
    }

    @PostMapping("/proposals")
    public Map<String, Object> submit(@RequestBody SubmitRequest req) {
        Proposal p = demo.submit(req.submitterAddress, req.title, req.recipient, req.amountUsdc);
        return proposalJson(p);
    }

    @PostMapping("/proposals/{id}/votes")
    public ResponseEntity<Void> vote(@PathVariable String id, @RequestBody VoteRequest req) {
        demo.vote(req.voterAddress, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/execute")
    public Map<String, Object> execute() throws Exception {
        DemoService.ExecuteSummary summary = demo.execute();
        return Map.of(
                "txHash", summary.txHash(),
                "seedTxHash", summary.seedTxHash() == null ? "" : summary.seedTxHash(),
                "balanceBeforeWei", summary.balanceBeforeWei().toString(),
                "balanceAfterWei", summary.balanceAfterWei().toString(),
                "winners", summary.winningProposals().stream().map(DemoController::proposalJson).toList()
        );
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        demo.resetRound();
        return ResponseEntity.noContent().build();
    }

    private static Map<String, Object> memberJson(Member m) {
        return Map.of(
                "name", m.getName(),
                "address", m.getWalletAddress(),
                "whitelisted", m.isWhitelisted()
        );
    }

    private static Map<String, Object> proposalJson(Proposal p) {
        return Map.of(
                "id", p.getId(),
                "title", p.getTitle(),
                "submitter", p.getSubmitterWalletAddress(),
                "recipient", p.getRecipientWalletAddress(),
                "amountUsdc", p.getRequestedUsdc(),
                "approvalCount", p.getApprovalCount(),
                "voters", List.copyOf(p.getApprovalVoterAddresses())
        );
    }

    private static Map<String, Object> tallyJson(GovernanceService.TallyResult t) {
        return Map.of(
                "uniqueVoterCount", t.getUniqueVoterCount(),
                "quorumReached", t.isQuorumReached(),
                "eligibleIds", t.getEligibleProposals().stream().map(Proposal::getId).toList(),
                "winnerIds", t.getWinningProposals().stream().map(Proposal::getId).toList(),
                "remainingBudgetUsdc", t.getRemainingBudgetUsdc()
        );
    }

    private static Map<String, Object> treasuryJson(DemoService.TreasurySnapshot snap) {
        java.util.HashMap<String, Object> m = new java.util.HashMap<>();
        m.put("available", snap.available());
        m.put("contractAddress", snap.contractAddress());
        m.put("adminAddress", snap.adminAddress());
        BigInteger bal = snap.balanceWei();
        m.put("balanceWei", bal == null ? null : bal.toString());
        m.put("error", snap.error());
        return m;
    }

    public static class SubmitRequest {
        public String submitterAddress;
        public String title;
        public String recipient;
        public int amountUsdc;
    }

    public static class VoteRequest {
        public String voterAddress;
    }
}
