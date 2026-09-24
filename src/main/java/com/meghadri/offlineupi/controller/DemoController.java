package com.meghadri.offlineupi.controller;

import com.meghadri.offlineupi.mesh.DemoSenderService;
import com.meghadri.offlineupi.mesh.MeshSimulatorService;
import com.meghadri.offlineupi.mesh.dto.SendOfflinePaymentRequest;
import com.meghadri.offlineupi.mesh.model.MeshPacket;
import com.meghadri.offlineupi.settlement.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final DemoSenderService demoSenderService;
    private final MeshSimulatorService meshSimulatorService;

    public DemoController(DemoSenderService demoSenderService, MeshSimulatorService meshSimulatorService) {
        this.demoSenderService = demoSenderService;
        this.meshSimulatorService = meshSimulatorService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody SendOfflinePaymentRequest request) {
        try {
            MeshPacket packet = demoSenderService.createOfflinePacket(
                    request.senderId(), request.receiverId(), request.amount());
            meshSimulatorService.inject(packet, "phone-alice");
            return ResponseEntity.ok(packet);
        } catch (SettlementService.SettlementException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}