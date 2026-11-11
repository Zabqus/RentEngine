package com.wzabek.rentengine.protocol.repository;

import com.wzabek.rentengine.protocol.entity.Protocol;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProtocolRepository extends JpaRepository<Protocol, Long> {

    @EntityGraph(attributePaths = {"employee", "damages"})
    List<Protocol> findByAgreementIdOrderByCreatedAtAsc(Long agreementId);
}
