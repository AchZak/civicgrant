package be.achzak.civicgrant.persistence.grantapplication;

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
@Table(name = "assessment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AssessmentJpaEntity {

    @Id
    private UUID grantApplicationId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grant_application_id")
    private GrantApplicationJpaEntity grantApplication;

    private String assessorOid;
    private boolean eligible;

    @Enumerated(EnumType.STRING)
    private AssessmentRecommendation recommendation;

    private BigDecimal proposedAmount;
    private String comment;
    private Instant assessedAt;

    AssessmentJpaEntity(
            GrantApplicationJpaEntity grantApplication,
            String assessorOid,
            boolean eligible,
            AssessmentRecommendation recommendation,
            BigDecimal proposedAmount,
            String comment,
            Instant assessedAt) {
        this.grantApplication = grantApplication;
        this.assessorOid = assessorOid;
        this.eligible = eligible;
        this.recommendation = recommendation;
        this.proposedAmount = proposedAmount;
        this.comment = comment;
        this.assessedAt = assessedAt;
    }
}
