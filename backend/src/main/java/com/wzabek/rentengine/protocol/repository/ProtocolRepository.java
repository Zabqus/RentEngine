package com.wzabek.rentengine.protocol.repository;

import com.wzabek.rentengine.protocol.entity.Protocol;
import com.wzabek.rentengine.protocol.entity.ProtocolType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProtocolRepository extends JpaRepository<Protocol, Long> {

    @EntityGraph(attributePaths = {"employee", "damages"})
    List<Protocol> findByAgreementIdOrderByCreatedAtAsc(Long agreementId);

    Optional<Protocol> findByAgreementIdAndType(Long agreementId, ProtocolType type);
}
