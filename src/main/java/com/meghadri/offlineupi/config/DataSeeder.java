package com.meghadri.offlineupi.config;

import com.meghadri.offlineupi.common.enums.WalletStatus;
import com.meghadri.offlineupi.user.entity.User;
import com.meghadri.offlineupi.user.repository.UserRepository;
import com.meghadri.offlineupi.wallet.entity.Wallet;
import com.meghadri.offlineupi.wallet.repository.WalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, WalletRepository walletRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if(userRepository.count()>0) return;

        createUserWithWallet("Alan", "alan@example.com", "9999900001", new BigDecimal("1000.00"));
        createUserWithWallet("Bob", "bob@example.com", "9999900002", new BigDecimal("500.00"));
        createUserWithWallet("Candy", "candy@example.com", "9999900003", new BigDecimal("250.00"));
    }

    private void createUserWithWallet(String name, String email, String phone, BigDecimal balance) {
        User user=User.builder()
                .fullName(name).email(email).phoneNumber(phone)
                .password(passwordEncoder.encode("user123"))
                .role("USER")
                .build();
        user=userRepository.save(user);

        Wallet wallet= Wallet.builder()
                .balance(balance)
                .offlineLimit(balance.multiply(new BigDecimal("0.20")))
                .status(WalletStatus.ACTIVE)
                .user(user)
                .build();
        walletRepository.save(wallet);
    }
}
