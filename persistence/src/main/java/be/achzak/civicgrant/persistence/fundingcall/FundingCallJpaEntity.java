package be.achzak.civicgrant.persistence.fundingcall;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "funding_call")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class FundingCallJpaEntity {

    @Id
    private UUID id;

    private String reference;
    private String titleNl;
    private String titleFr;
    private Instant opensAt;
    private Instant closesAt;
    private BigDecimal totalBudget;
    private BigDecimal committedAmount;
    private BigDecimal maxAmountPerApplication;

    @Version
    private long version;

    private Instant createdAt;
}
