import fs from "node:fs";
import path from "node:path";
import solc from "solc";
import {
  createPublicClient,
  createWalletClient,
  http,
  parseEther,
} from "viem";
import { privateKeyToAccount } from "viem/accounts";

const RPC_URL = process.env.DAO_RPC_URL ?? "http://127.0.0.1:8545";
const ADMIN_PRIVATE_KEY =
  process.env.DAO_ADMIN_PRIVATE_KEY ??
  "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";

const chain = {
  id: 31337,
  name: "hardhat",
  nativeCurrency: { name: "Ether", symbol: "ETH", decimals: 18 },
  rpcUrls: { default: { http: [RPC_URL] } },
};

function compileForumTreasury() {
  const contractPath = path.resolve("contracts", "ForumTreasury.sol");
  const source = fs.readFileSync(contractPath, "utf8");

  const input = {
    language: "Solidity",
    sources: {
      "ForumTreasury.sol": { content: source },
    },
    settings: {
      optimizer: { enabled: false, runs: 200 },
      outputSelection: {
        "*": {
          "*": ["abi", "evm.bytecode.object"],
        },
      },
    },
  };

  const output = JSON.parse(solc.compile(JSON.stringify(input)));

  const errors = output.errors ?? [];
  const fatalErrors = errors.filter((e) => e.severity === "error");
  if (fatalErrors.length > 0) {
    for (const e of fatalErrors) {
      console.error(e.formattedMessage ?? e.message ?? String(e));
    }
    process.exit(1);
  }

  const contract = output.contracts?.["ForumTreasury.sol"]?.ForumTreasury;
  if (!contract) {
    throw new Error("Compilation output missing ForumTreasury contract");
  }

  const abi = contract.abi;
  const bytecode = `0x${contract.evm.bytecode.object}`;
  return { abi, bytecode };
}

async function main() {
  const { abi, bytecode } = compileForumTreasury();

  const account = privateKeyToAccount(ADMIN_PRIVATE_KEY);
  const walletClient = createWalletClient({ chain, transport: http(RPC_URL) });
  const publicClient = createPublicClient({ chain, transport: http(RPC_URL) });

  console.log("Deploying contract with account:", account.address);

  const hash = await walletClient.deployContract({
    abi,
    bytecode,
    account,
    args: [],
    value: 0n,
    gas: 3_000_000n,
  });

  const receipt = await publicClient.waitForTransactionReceipt({ hash });

  console.log("--------------------------------------------------");
  console.log("SUCCESS: ForumTreasury deployed to:", receipt.contractAddress);
  console.log("Admin Address:", account.address);
  console.log("Deploy Tx Hash:", hash);
  console.log("--------------------------------------------------");

  // Optional: fund the treasury a bit for later demos (0.01 ETH)
  if ((process.env.DAO_SEED_TREASURY ?? "true") === "true") {
    const fundHash = await walletClient.sendTransaction({
      account,
      to: receipt.contractAddress,
      value: parseEther("0.01"),
    });
    await publicClient.waitForTransactionReceipt({ hash: fundHash });
    console.log("Seeded treasury with 0.01 ETH. Tx Hash:", fundHash);
  }
}

main().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});

