package com.dao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Web entry point. The demo UI is served from {@code src/main/resources/static/index.html}
 * and talks to {@link com.dao.web.DemoController} over REST.
 *
 * Start order for the full demo:
 *   1. npx hardhat node
 *   2. npm run -s deploy:raw     (prints the contract address)
 *   3. export DAO_CONTRACT_ADDRESS=0x...
 *   4. ./mvnw spring-boot:run
 *   5. open http://localhost:8080
 */
@SpringBootApplication
public class DaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaoApplication.class, args);
    }
}
