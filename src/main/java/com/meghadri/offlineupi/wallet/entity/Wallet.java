package com.meghadri.offlineupi.wallet.entity;

import com.meghadri.offlineupi.common.enums.WalletStatus;
import com.meghadri.offlineupi.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal balance;

    // Amount this wallet may spend while offline, decremented at packet-creation time
    @Column(name = "offline_limit", nullable = false)
    @Builder.Default
    private BigDecimal offlineLimit = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_wallet_user")
    )
    private User user;

    // Optimistic locking — prevents concurrent settlements from corrupting balance
    @Version
    private Long version;
}