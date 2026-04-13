import hardhatToolboxViem from "@nomicfoundation/hardhat-toolbox-viem";

export default {
  plugins: [hardhatToolboxViem],
  solidity: "0.8.20",
  networks: {
    localhost: {
      url: "http://127.0.0.1:8545",
    },
  },
};

