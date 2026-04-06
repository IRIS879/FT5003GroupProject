// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title ForumTreasury
 * @dev Manages community funds and executes batch payouts based on DAO governance results.
 */
contract ForumTreasury {
    address public admin;

    event FundsReceived(address indexed from, uint256 amount);
    event PayoutExecuted(address[] recipients, uint256[] amounts);

    constructor() {
        // The account that deploys the contract becomes the admin
        admin = msg.sender;
    }

    modifier onlyAdmin() {
        require(msg.sender == admin, "Access Denied: Only admin can execute");
        _;
    }

    /**
     * @dev Fallback function to receive ETH/Native tokens into the treasury.
     */
    receive() external payable {
        emit FundsReceived(msg.sender, msg.value);
    }

    /**
     * @dev Executes batch transfers to winning proposals.
     * @param recipients Array of wallet addresses to receive funds.
     * @param amounts Array of amounts corresponding to each recipient.
     */
    function executeTreasury(address[] calldata recipients, uint256[] calldata amounts) 
        external 
        onlyAdmin 
    {
        require(recipients.length == amounts.length, "Input Error: Array length mismatch");
        
        for (uint256 i = 0; i < recipients.length; i++) {
            require(address(this).balance >= amounts[i], "Treasury Error: Insufficient balance");
            
            (bool success, ) = recipients[i].call{value: amounts[i]}("");
            require(success, "Treasury Error: Transfer failed");
        }
        
        emit PayoutExecuted(recipients, amounts);
    }

    /**
     * @dev Returns the current balance of the treasury.
     */
    function getBalance() public view returns (uint256) {
        return address(this).balance;
    }
}