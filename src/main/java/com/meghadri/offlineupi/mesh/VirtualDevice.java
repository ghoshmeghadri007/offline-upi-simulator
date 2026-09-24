package com.meghadri.offlineupi.mesh;

import com.meghadri.offlineupi.mesh.model.MeshPacket;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class VirtualDevice {

    private final String deviceId;
    private final boolean hasInternet;
    private final List<MeshPacket> heldPackets = new ArrayList<>();
    private final Set<String> seenPacketIds = ConcurrentHashMap.newKeySet();

    public VirtualDevice(String deviceId, boolean hasInternet) {
        this.deviceId = deviceId;
        this.hasInternet = hasInternet;
    }

    public void receive(MeshPacket packet) {
        if (packet.getTtl() <= 0) return;
        if (!seenPacketIds.add(packet.getPacketId())) return;
        heldPackets.add(packet);
    }

    public void clear() {
        heldPackets.clear();
        seenPacketIds.clear();
    }
}