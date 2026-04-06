const hre = require("hardhat");

async function main() {
  // Get the first account from the local node as the deployer
  const [deployer] = await hre.ethers.getSigners();

  console.log("Deploying contract with account:", deployer.address);

  // Deploy the contract
  const ForumTreasury = await hre.ethers.getContractFactory("ForumTreasury");
  const treasury = await ForumTreasury.deploy();

  // Wait for the deployment to be confirmed
  await treasury.waitForDeployment();

  const contractAddress = await treasury.getAddress();

  console.log("--------------------------------------------------");
  console.log("SUCCESS: ForumTreasury deployed to:", contractAddress);
  console.log("Admin Address:", deployer.address);
  console.log("--------------------------------------------------");
  console.log("Action Required: Copy the Contract Address and Update your Java config!");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});