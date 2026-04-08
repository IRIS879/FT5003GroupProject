import hre from "hardhat";

async function main() {
  const [deployer] = await hre.viem.getWalletClients();

  console.log("Deploying contract with account:", deployer.account.address);

  const treasury = await hre.viem.deployContract("ForumTreasury");

  console.log("--------------------------------------------------");
  console.log("SUCCESS: ForumTreasury deployed to:", treasury.address);
  console.log("Admin Address:", deployer.account.address);
  console.log("--------------------------------------------------");
  console.log("Action Required: Copy the Contract Address and Update your Java config!");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
