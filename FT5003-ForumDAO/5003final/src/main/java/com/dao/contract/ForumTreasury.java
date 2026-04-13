package com.dao.contract;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minimal Web3j wrapper for contracts/ForumTreasury.sol.
 *
 * This is intentionally hand-written to avoid requiring Web3j codegen for the course MVP.
 */
public class ForumTreasury extends Contract {

    private static final String BINARY = "0x";

    protected ForumTreasury(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ForumTreasury load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ForumTreasury(contractAddress, web3j, credentials, contractGasProvider);
    }

    public RemoteFunctionCall<String> admin() {
        final Function function = new Function(
                "admin",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Address>() {})
        );
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> getBalance() {
        final Function function = new Function(
                "getBalance",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Uint256>() {})
        );
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> executeTreasury(List<String> recipients, List<BigInteger> amounts) {
        final List<Address> recipientTypes = recipients.stream().map(Address::new).collect(Collectors.toList());
        final List<Uint256> amountTypes = amounts.stream().map(Uint256::new).collect(Collectors.toList());

        final List<Type> inputs = Arrays.asList(
                new DynamicArray<>(Address.class, recipientTypes),
                new DynamicArray<>(Uint256.class, amountTypes)
        );

        final Function function = new Function(
                "executeTreasury",
                inputs,
                Collections.emptyList()
        );

        return executeRemoteCallTransaction(function);
    }
}

