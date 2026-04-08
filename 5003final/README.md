# Forum Builder Fund DAO

An MVP for a forum-based community fund governance system.

## Tech Stack
- **Blockchain:** Solidity, Hardhat
- **Backend:** Java (Spring Boot / Maven), Web3j
- **Governance:** Approval Voting with Credit-based Whitelist

## Getting Started
> Run all commands from this folder: `C:\Users\Try\Documents\GitHub\FT5003GroupProject\5003final`

1. Install Hardhat deps: `npm install`.
2. Start local chain: `npx hardhat node`.
3. Deploy & seed treasury (new terminal): `npm run -s deploy:raw`.
4. Run Java connectivity test:
   - If you already have Java/Maven installed: run `mvn test`
   - Otherwise use the included Maven Wrapper: `mvnw.cmd test` (requires `JAVA_HOME` to be set)

## One-click Demo
1. Start local chain: `npx hardhat node`
2. Deploy & seed treasury (recommended): `npm run -s deploy:raw`
3. Run the scripted CLI demo: `run-demo.cmd`
