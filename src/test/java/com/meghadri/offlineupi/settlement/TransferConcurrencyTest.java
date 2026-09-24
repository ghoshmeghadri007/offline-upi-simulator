package com.meghadri.offlineupi.settlement;

import com.meghadri.offlineupi.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TransferConcurrencyTest {

    @Autowired private SettlementService settlementService;
    @Autowired private WalletRepository walletRepository;

    @Test
    void concurrentTransfersNeverCorruptBalance() throws InterruptedException {
        int threads = 10;
        BigDecimal amountEach = new BigDecimal("10.00");
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    boolean done = false;
                    while (!done) {
                        try {
                            settlementService.settle(1L, 2L, amountEach, null);
                            done = true;
                        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                            // retry
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        var alice = walletRepository.findByUser_Id(1L).orElseThrow();
        var bob = walletRepository.findByUser_Id(2L).orElseThrow();

        assertEquals(new BigDecimal("900.00"), alice.getBalance());
        assertEquals(new BigDecimal("600.00"), bob.getBalance());
    }
}