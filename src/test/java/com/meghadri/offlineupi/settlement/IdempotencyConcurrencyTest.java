package com.meghadri.offlineupi.settlement;

import com.meghadri.offlineupi.crypto.HybridCryptoService;
import com.meghadri.offlineupi.mesh.model.MeshPacket;
import com.meghadri.offlineupi.mesh.model.PaymentInstruction;
import com.meghadri.offlineupi.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class IdempotencyConcurrencyTest {

    @Autowired private HybridCryptoService cryptoService;
    @Autowired private BridgeIngestionService bridgeIngestionService;
    @Autowired private WalletRepository walletRepository;

    @Test
    void singlePacketDeliveredByThreeBridgesSettlesExactlyOnce() throws Exception {
        var senderBefore = walletRepository.findByUser_Id(1L).orElseThrow();
        BigDecimal balanceBefore = senderBefore.getBalance();
        BigDecimal amount = new BigDecimal("50.00");

        PaymentInstruction instruction = PaymentInstruction.builder()
                .senderId(1L).receiverId(2L).amount(amount)
                .nonce(UUID.randomUUID().toString())
                .signedAt(Instant.now().toEpochMilli())
                .build();

        String ciphertext = cryptoService.encrypt(instruction);
        MeshPacket packet = MeshPacket.builder()
                .packetId(UUID.randomUUID().toString())
                .ttl(3).createdAt(Instant.now().toEpochMilli())
                .ciphertext(ciphertext)
                .build();

        int bridges = 3;
        ExecutorService pool = Executors.newFixedThreadPool(bridges);
        CountDownLatch latch = new CountDownLatch(bridges);
        List<Future<IngestResult>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < bridges; i++) {
            futures.add(pool.submit(() -> {
                try {
                    return bridgeIngestionService.ingest(packet);
                } finally {
                    latch.countDown();
                }
            }));
        }
        latch.await();
        pool.shutdown();

        AtomicInteger settled = new AtomicInteger();
        AtomicInteger duplicates = new AtomicInteger();
        for (Future<IngestResult> f : futures) {
            IngestResult r = f.get();
            if (r.outcome() == IngestOutcome.SETTLED) settled.incrementAndGet();
            if (r.outcome() == IngestOutcome.DUPLICATE_DROPPED) duplicates.incrementAndGet();
        }

        assertEquals(1, settled.get());
        assertEquals(2, duplicates.get());

        var senderAfter = walletRepository.findByUser_Id(1L).orElseThrow();
        assertEquals(balanceBefore.subtract(amount), senderAfter.getBalance());
    }

    @Test
    void tamperedCiphertextIsRejected() {
        PaymentInstruction instruction = PaymentInstruction.builder()
                .senderId(1L).receiverId(2L).amount(new BigDecimal("5.00"))
                .nonce(UUID.randomUUID().toString())
                .signedAt(Instant.now().toEpochMilli())
                .build();

        String ciphertext = cryptoService.encrypt(instruction);
        char[] chars = ciphertext.toCharArray();
        chars[chars.length / 2] = chars[chars.length / 2] == 'A' ? 'B' : 'A';
        String tampered = new String(chars);

        MeshPacket packet = MeshPacket.builder()
                .packetId(UUID.randomUUID().toString())
                .ttl(3).createdAt(Instant.now().toEpochMilli())
                .ciphertext(tampered)
                .build();

        IngestResult result = bridgeIngestionService.ingest(packet);
        assertEquals(IngestOutcome.INVALID, result.outcome());
    }
}