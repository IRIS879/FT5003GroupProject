package com.dao;

import com.dao.service.BlockchainService;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlockchainServiceTest {

    @Test
    public void usdcToWei_usesFixedDemoRatio() {
        assertEquals(new BigInteger("1000000000000"), BlockchainService.usdcToWei(1));
        assertEquals(new BigInteger("5000000000000000"), BlockchainService.usdcToWei(5000));
    }
}

