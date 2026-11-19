package be.achzak.civicgrant.persistence.fundingcall;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "funding_call")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// A builder, not a constructor: nine columns never change, more than Sonar's seven parameters.
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
class FundingCallJpaEntity {

    @Id
    private UUID id;

    private String reference;
    private String titleNl;
    private String titleFr;
    private Instant opensAt;
    private Instant closesAt;
    private BigDecimal totalBudget;

    @Setter(AccessLevel.PACKAGE)
    private BigDecimal committedAmount;

    private BigDecimal maxAmountPerApplication;

    @Version
    private long version;

    private Instant createdAt;
}
