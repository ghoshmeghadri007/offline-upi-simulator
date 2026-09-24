package com.meghadri.offlineupi.controller;

import com.meghadri.offlineupi.settlement.SettlementService;
import com.meghadri.offlineupi.transaction.dto.TransferRequest;
import com.meghadri.offlineupi.transaction.entity.Transaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransferController {

    private final SettlementService settlementService;

    public TransferController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request) {
        try {
            Transaction txn = settlementService.settle(
                    request.senderId(), request.receiverId(), request.amount(), null);
            return ResponseEntity.ok(txn);
        } catch (SettlementService.SettlementException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}