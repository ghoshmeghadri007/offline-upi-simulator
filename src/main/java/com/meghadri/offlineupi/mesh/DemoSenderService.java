package com.meghadri.offlineupi.mesh;

import com.meghadri.offlineupi.crypto.HybridCryptoService;
import com.meghadri.offlineupi.mesh.model.MeshPacket;
import com.meghadri.offlineupi.mesh.model.PaymentInstruction;
import com.meghadri.offlineupi.settlement.SettlementService;
import com.meghadri.offlineupi.wallet.entity.Wallet;
import com.meghadri.offlineupi.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class DemoSenderService {

    private final HybridCryptoService cryptoService;
    private final WalletRepository walletRepository;

    public DemoSenderService(HybridCryptoService cryptoService, WalletRepository walletRepository) {
        this.cryptoService = cryptoService;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public MeshPacket createOfflinePacket(Long senderId, Long receiverId, BigDecimal amount) {
        Wallet senderWallet = walletRepository.findByUser_Id(senderId)
                .orElseThrow(() -> new SettlementService.SettlementException("Sender wallet not found"));

        if (senderWallet.getOfflineLimit().compareTo(amount) < 0) {
            throw new SettlementService.SettlementException(
                    "Amount exceeds offline spending limit (" + senderWallet.getOfflineLimit() + ")");
        }

        senderWallet.setOfflineLimit(senderWallet.getOfflineLimit().subtract(amount));
        walletRepository.save(senderWallet);

        PaymentInstruction instruction = PaymentInstruction.builder()
                .senderId(senderId).receiverId(receiverId).amount(amount)
                .nonce(UUID.randomUUID().toString())
                .signedAt(Instant.now().toEpochMilli())
                .build();

        String ciphertext = cryptoService.encrypt(instruction);

        return MeshPacket.builder()
                .packetId(UUID.randomUUID().toString())
                .ttl(5)
                .createdAt(Instant.now().toEpochMilli())
                .ciphertext(ciphertext)
                .build();
    }
}