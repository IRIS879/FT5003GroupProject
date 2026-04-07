package com.dao.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Member {
    private String walletAddress;
    private boolean isWhitelisted;
    private boolean hasVotedThisRound;
}
