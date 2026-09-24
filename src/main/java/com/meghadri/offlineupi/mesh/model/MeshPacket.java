package com.meghadri.offlineupi.mesh.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeshPacket {
    private String packetId;
    private int ttl;
    private long createdAt;
    private String ciphertext;
}
