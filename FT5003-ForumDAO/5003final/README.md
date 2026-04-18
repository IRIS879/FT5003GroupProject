# Forum Builder Fund DAO

Off-chain governance engine + on-chain treasury execution for a community
"forum builder" grants program.

## What it does

Members submit grant proposals; whitelisted members approve them; at the end
of a round the engine ranks the eligible proposals and dispatches a single
batch payout on-chain from a shared treasury contract.

## Rules

The governance engine ([GovernanceService.java](src/main/java/com/dao/service/GovernanceService.java))
enforces the following hard rules:

| Rule                  | Value / behaviour                                                  |
|-----------------------|--------------------------------------------------------------------|
| Submitter whitelist   | Only whitelisted addresses may submit proposals                    |
| Voter whitelist       | Only whitelisted addresses may cast approval votes                 |
| No self-vote          | A submitter cannot vote on their own proposal                      |
| No duplicate votes    | Same voter cannot approve the same proposal twice                  |
| Quorum                | Round needs at least **5** unique voters, otherwise zero winners   |
| Approval threshold    | Proposal needs at least **3** approvals to be eligible             |
| Ranking               | Approvals desc → submission time asc → submission order → id       |
| Round budget          | **5000 USDC** cap per round; proposals that don't fit are skipped  |

## Tech stack

- **Contract:** Solidity 0.8.20, deployed via Hardhat or a raw viem script
- **Governance engine:** Java 17, pure in-memory
- **Chain client:** Web3j (Spring Boot `CommandLineRunner` entry point)
- **Tests:** JUnit 5

## Layout

```
contracts/            ForumTreasury.sol — batch payout contract
scripts/              Hardhat / viem deploy scripts
src/main/java/com/dao
    DaoApplication.java              end-to-end demo runner
    service/GovernanceService.java   rules engine
    service/BlockchainService.java   Web3j wrapper for ForumTreasury
    model/{Member,Proposal}.java
src/test/java/com/dao
    GovernanceServiceTest.java       happy-path tally (quorum, ranking, budget)
    GovernanceRulesTest.java         demo failure scenarios (see below)
    BlockchainServiceTest.java       USDC → Wei conversion
    BlockchainConnectionTest.java    hits a running Hardhat node
```

## Running it

**Easiest — one command** (starts Hardhat + deploys + launches the web UI):
```bash
./run-frontend.sh
# → open http://localhost:8080
```

**Manual** (if you want the pieces in separate panes):
```bash
# 1. Start a local chain
npx hardhat node

# 2. In a second terminal, deploy + seed the treasury
npm run -s deploy:raw
# → the first deploy on a clean chain yields the default address

# 3. Run the Spring Boot web app
./mvnw spring-boot:run
# → open http://localhost:8080
```

**Just the unit tests** (no chain needed for the pure-Java suites):
```bash
./mvnw test -Dtest='GovernanceServiceTest,GovernanceRulesTest,BlockchainServiceTest'
```

**Full chain + all tests** (what CI-equivalent looks like):
```bash
./run-full-chain.sh
```

## Frontend

The web UI lives at `src/main/resources/static/index.html` and is a single
vanilla-JS page talking to `/api/*` on the same Spring Boot process. Three panels:

1. **Members** — pick the "acting as" identity; whitelist badge is visible.
   Includes `Mallory` as a non-whitelisted outsider for failure demos.
2. **Proposals & Voting** — submit form (executes as the selected member),
   per-proposal approve button, live approval count. Eligible/winner state is
   highlighted.
3. **Round & Treasury** — live quorum progress, winner list, remaining budget,
   and an Execute button that calls `executeTreasury` on-chain.

Every backend rejection (non-whitelisted submit, self-vote, quorum failure,
budget skip, etc.) surfaces as a red toast showing the exact error message,
which makes the failure-path demos easy to present.

Environment overrides (all optional):

| Variable                | Default                                                  |
|-------------------------|----------------------------------------------------------|
| `DAO_RPC_URL`           | `http://127.0.0.1:8545`                                  |
| `DAO_CONTRACT_ADDRESS`  | `0x5FbDB2315678afecb367f032d93F642f64180aa3`             |
| `DAO_ADMIN_PRIVATE_KEY` | Hardhat account #0 key                                   |

## Test scenarios ([GovernanceRulesTest.java](src/test/java/com/dao/GovernanceRulesTest.java))

These are the failure paths demonstrated in the recorded walkthrough:

1. `submit_nonWhitelistedSubmitter_isRejected` — outsider tries to submit
2. `vote_nonWhitelistedVoter_isRejected` — outsider tries to vote
3. `vote_selfVote_isRejected` — submitter votes on own proposal
4. `vote_duplicateApproval_isRejected` — same member approves twice
5. `tally_quorumNotReached_producesNoWinners_evenIfThresholdMet` — proposal has 3 approvals but only 3 voters overall
6. `tally_budgetInsufficient_skipsProposalThatDoesNotFit` — middle-ranked proposal skipped so a smaller later one can still be funded

Happy-path ranking + budget cutoff is covered by [GovernanceServiceTest.java](src/test/java/com/dao/GovernanceServiceTest.java).

## Demo video walkthrough

See [DEMO.md](DEMO.md) for the scene-by-scene script used to record the 8–15 minute walkthrough.
