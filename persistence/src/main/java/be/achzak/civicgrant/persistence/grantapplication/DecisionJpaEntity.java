package be.achzak.civicgrant.persistence.grantapplication;

import be.achzak.civicgrant.domain.grantapplication.model.RejectionGround;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "decision")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DecisionJpaEntity {

    @Id
    private UUID grantApplicationId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grant_application_id")
    private GrantApplicationJpaEntity grantApplication;

    private String decisionMakerOid;

    @Enumerated(EnumType.STRING)
    private DecisionOutcome outcome;

    private BigDecimal grantedAmount;

    @Enumerated(EnumType.STRING)
    private RejectionGround rejectionGround;

    private String motivation;
    private Instant decidedAt;

    DecisionJpaEntity(
            GrantApplicationJpaEntity grantApplication,
            String decisionMakerOid,
            DecisionOutcome outcome,
            BigDecimal grantedAmount,
            RejectionGround rejectionGround,
            String motivation,
            Instant decidedAt) {
        this.grantApplication = grantApplication;
        this.decisionMakerOid = decisionMakerOid;
        this.outcome = outcome;
        this.grantedAmount = grantedAmount;
        this.rejectionGround = rejectionGround;
        this.motivation = motivation;
        this.decidedAt = decidedAt;
    }
}
