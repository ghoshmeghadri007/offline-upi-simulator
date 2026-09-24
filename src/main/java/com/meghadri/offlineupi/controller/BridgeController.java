package com.meghadri.offlineupi.controller;

import com.meghadri.offlineupi.mesh.model.MeshPacket;
import com.meghadri.offlineupi.settlement.BridgeIngestionService;
import com.meghadri.offlineupi.settlement.IngestResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bridge")
public class BridgeController {

    private final BridgeIngestionService bridgeIngestionService;

    public BridgeController(BridgeIngestionService bridgeIngestionService) {
        this.bridgeIngestionService = bridgeIngestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResult> ingest(@RequestBody MeshPacket packet) {
        return ResponseEntity.ok(bridgeIngestionService.ingest(packet));
    }
}