package com.meghadri.offlineupi.settlement;

import com.meghadri.offlineupi.crypto.HybridCryptoService;
import com.meghadri.offlineupi.mesh.model.MeshPacket;
import com.meghadri.offlineupi.mesh.model.PaymentInstruction;
import com.meghadri.offlineupi.transaction.entity.Transaction;
import com.meghadri.offlineupi.transaction.repository.TransactionRepository;
import com.meghadri.offlineupi.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;

@Service
public class BridgeIngestionService {

    private static final long FRESHNESS_WINDOW_HOURS = 24;

    private final HybridCryptoService cryptoService;
    private final IdempotencyService idempotencyService;
    private final SettlementService settlementService;
    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public BridgeIngestionService(HybridCryptoService cryptoService,
                                  IdempotencyService idempotencyService,
                                  SettlementService settlementService,
                                  TransactionRepository transactionRepository,
                                  WalletRepository walletRepository) {
        this.cryptoService = cryptoService;
        this.idempotencyService = idempotencyService;
        this.settlementService = settlementService;
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    public IngestResult ingest(MeshPacket packet) {
        String packetHash = cryptoService.hashCiphertext(packet.getCiphertext());

        if (!idempotencyService.claim(packetHash)) {
            return new IngestResult(IngestOutcome.DUPLICATE_DROPPED, packetHash, "Already processed", null);
        }
        if (transactionRepository.findByPacketHash(packetHash).isPresent()) {
            return new IngestResult(IngestOutcome.DUPLICATE_DROPPED, packetHash, "Already settled (DB)", null);
        }

        PaymentInstruction instruction;
        try {
            instruction = cryptoService.decrypt(packet.getCiphertext());
        } catch (HybridCryptoService.DecryptionException e) {
            return new IngestResult(IngestOutcome.INVALID, packetHash, "Tampered or corrupt packet", null);
        }

        Instant signedAt = Instant.ofEpochMilli(instruction.getSignedAt());
        if (Duration.between(signedAt, Instant.now()).toHours() > FRESHNESS_WINDOW_HOURS) {
            return new IngestResult(IngestOutcome.INVALID, packetHash, "Packet expired (older than 24h)", null);
        }

        try {
            Transaction txn = settlementService.settle(
                    instruction.getSenderId(), instruction.getReceiverId(), instruction.getAmount(), packetHash);
            return new IngestResult(IngestOutcome.SETTLED, packetHash, null, txn.getId());
        } catch (SettlementService.SettlementException e) {
            restoreOfflineLimit(instruction);
            return new IngestResult(IngestOutcome.INVALID, packetHash, e.getMessage(), null);
        }
    }

    private void restoreOfflineLimit(PaymentInstruction instruction) {
        walletRepository.findByUser_Id(instruction.getSenderId()).ifPresent(wallet -> {
            wallet.setOfflineLimit(wallet.getOfflineLimit().add(instruction.getAmount()));
            walletRepository.save(wallet);
        });
    }
}