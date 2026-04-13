package com.dao.model;

import java.util.Objects;

public class Member {
    private final String name;
    private final String walletAddress;
    private final boolean whitelisted;

    public Member(String name, String walletAddress, boolean whitelisted) {
        this.name = Objects.requireNonNull(name, "name");
        this.walletAddress = Objects.requireNonNull(walletAddress, "walletAddress");
        this.whitelisted = whitelisted;
    }

    public String getName() {
        return name;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public boolean isWhitelisted() {
        return whitelisted;
    }
}

