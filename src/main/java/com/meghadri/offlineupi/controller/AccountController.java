package com.meghadri.offlineupi.controller;

import com.meghadri.offlineupi.wallet.entity.Wallet;
import com.meghadri.offlineupi.wallet.repository.WalletRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final WalletRepository walletRepository;

    public AccountController(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @GetMapping
    public List<Wallet> all() {
        return walletRepository.findAll();
    }
}