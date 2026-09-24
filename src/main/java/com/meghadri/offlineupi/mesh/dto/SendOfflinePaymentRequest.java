package com.meghadri.offlineupi.mesh.dto;

import java.math.BigDecimal;

public record SendOfflinePaymentRequest(Long senderId, Long receiverId, BigDecimal amount) {
}