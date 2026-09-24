package com.meghadri.offlineupi.mesh;

import com.meghadri.offlineupi.mesh.model.MeshPacket;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MeshSimulatorService {

    private final Map<String, VirtualDevice> devices = new LinkedHashMap<>();

    public MeshSimulatorService() {
        devices.put("phone-alice", new VirtualDevice("phone-alice", false));
        devices.put("phone-stranger1", new VirtualDevice("phone-stranger1", false));
        devices.put("phone-stranger2", new VirtualDevice("phone-stranger2", false));
        devices.put("phone-bridge", new VirtualDevice("phone-bridge", true));
    }

    public void inject(MeshPacket packet, String originDeviceId) {
        devices.get(originDeviceId).receive(packet);
    }

    public void runGossipRound() {
        Map<String, List<MeshPacket>> snapshot = new LinkedHashMap<>();
        for (VirtualDevice device : devices.values()) {
            snapshot.put(device.getDeviceId(), List.copyOf(device.getHeldPackets()));
        }

        for (VirtualDevice sourceDevice : devices.values()) {
            for (MeshPacket packet : snapshot.get(sourceDevice.getDeviceId())) {
                MeshPacket hopped = MeshPacket.builder()
                        .packetId(packet.getPacketId())
                        .ttl(packet.getTtl() - 1)
                        .createdAt(packet.getCreatedAt())
                        .ciphertext(packet.getCiphertext())
                        .build();

                for (VirtualDevice targetDevice : devices.values()) {
                    if (targetDevice == sourceDevice) continue;
                    targetDevice.receive(hopped);
                }
            }
        }
    }

    public Map<String, VirtualDevice> getDevices() { return devices; }

    public void reset() {
        devices.values().forEach(VirtualDevice::clear);
    }
}