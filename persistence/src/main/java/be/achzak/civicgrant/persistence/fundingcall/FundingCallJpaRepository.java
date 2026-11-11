package be.achzak.civicgrant.persistence.fundingcall;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface FundingCallJpaRepository extends Repository<FundingCallJpaEntity, UUID> {

    Optional<FundingCallJpaEntity> findById(UUID id);

    @Query("select fundingCall from FundingCallJpaEntity fundingCall"
            + " where fundingCall.opensAt <= :moment and fundingCall.closesAt > :moment")
    List<FundingCallJpaEntity> findOpenAt(Instant moment);
}
