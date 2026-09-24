package com.meghadri.offlineupi.settlement;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final ConcurrentHashMap<String, Instant> seen = new ConcurrentHashMap<>();

    public boolean claim(String packetHash) {
        Instant previous = seen.putIfAbsent(packetHash, Instant.now());
        return previous == null;
    }

    public void reset() { seen.clear(); }
}