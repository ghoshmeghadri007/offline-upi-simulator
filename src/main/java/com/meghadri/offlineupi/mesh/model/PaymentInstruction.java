package com.meghadri.offlineupi.mesh.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInstruction {
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String nonce;
    private Long signedAt;
}
