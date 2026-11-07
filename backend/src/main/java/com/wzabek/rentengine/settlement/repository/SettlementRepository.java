package com.wzabek.rentengine.settlement.repository;

import com.wzabek.rentengine.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByAgreementId(Long agreementId);
}
