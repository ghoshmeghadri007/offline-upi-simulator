package com.meghadri.offlineupi.settlement;

public record IngestResult(IngestOutcome outcome, String packetHash, String reason, Long transactionId) {
}