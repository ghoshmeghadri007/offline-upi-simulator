package com.meghadri.offlineupi.settlement;

import com.meghadri.offlineupi.common.enums.TransactionStatus;
import com.meghadri.offlineupi.common.enums.WalletStatus;
import com.meghadri.offlineupi.transaction.entity.Transaction;
import com.meghadri.offlineupi.transaction.repository.TransactionRepository;
import com.meghadri.offlineupi.wallet.entity.Wallet;
import com.meghadri.offlineupi.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class SettlementService {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public SettlementService(WalletRepository walletRepository,
                             TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public static class SettlementException extends RuntimeException {
        public SettlementException(String message) { super(message); }
    }

    @Transactional
    public Transaction settle(Long senderId, Long receiverId, BigDecimal amount, String packetHash){
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SettlementException("Amount must be positive");
        }
        if (senderId.equals(receiverId)) {
            throw new SettlementException("Sender and receiver must differ");
        }

        Wallet sender = walletRepository.findByUser_Id(senderId)
                .orElseThrow(() -> new SettlementException("Sender wallet not found"));
        Wallet receiver = walletRepository.findByUser_Id(receiverId)
                .orElseThrow(() -> new SettlementException("Receiver wallet not found"));

        Transaction txn = Transaction.builder()
                .senderId(senderId).receiverId(receiverId).amount(amount)
                .packetHash(packetHash).status(TransactionStatus.PENDING)
                .build();

        if (sender.getStatus() != WalletStatus.ACTIVE || receiver.getStatus() != WalletStatus.ACTIVE) {
            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);
            throw new SettlementException("One or both wallets are not ACTIVE");
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);
            throw new SettlementException("Insufficient funds");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        walletRepository.save(sender);
        walletRepository.save(receiver);

        txn.setStatus(TransactionStatus.SUCCESS);
        txn.setSettledAt(LocalDateTime.now());
        return transactionRepository.save(txn);

    }

}
