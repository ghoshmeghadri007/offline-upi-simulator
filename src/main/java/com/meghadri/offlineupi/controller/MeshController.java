package com.meghadri.offlineupi.controller;

import com.meghadri.offlineupi.mesh.MeshSimulatorService;
import com.meghadri.offlineupi.mesh.VirtualDevice;
import com.meghadri.offlineupi.mesh.model.MeshPacket;
import com.meghadri.offlineupi.settlement.BridgeIngestionService;
import com.meghadri.offlineupi.settlement.IdempotencyService;
import com.meghadri.offlineupi.settlement.IngestResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mesh")
public class MeshController {

    private final MeshSimulatorService meshSimulatorService;
    private final BridgeIngestionService bridgeIngestionService;
    private final IdempotencyService idempotencyService;

    public MeshController(MeshSimulatorService meshSimulatorService,
                          BridgeIngestionService bridgeIngestionService,
                          IdempotencyService idempotencyService) {
        this.meshSimulatorService = meshSimulatorService;
        this.bridgeIngestionService = bridgeIngestionService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/gossip")
    public ResponseEntity<?> gossip() {
        meshSimulatorService.runGossipRound();
        return ResponseEntity.ok(state());
    }

    @PostMapping("/flush")
    public ResponseEntity<List<IngestResult>> flush() {
        List<IngestResult> results = meshSimulatorService.getDevices().values().stream()
                .filter(VirtualDevice::isHasInternet)
                .flatMap(device -> device.getHeldPackets().stream())
                .map(bridgeIngestionService::ingest)
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/state")
    public ResponseEntity<Map<String, Object>> state() {
        Map<String, Object> result = meshSimulatorService.getDevices().values().stream()
                .collect(Collectors.toMap(
                        VirtualDevice::getDeviceId,
                        d -> Map.of(
                                "hasInternet", d.isHasInternet(),
                                "packetCount", d.getHeldPackets().size(),
                                "packetIds", d.getHeldPackets().stream().map(MeshPacket::getPacketId).toList()
                        )
                ));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset() {
        meshSimulatorService.reset();
        idempotencyService.reset();
        return ResponseEntity.ok().build();
    }
}