package com.wzabek.rentengine.protocol.repository;

import com.wzabek.rentengine.protocol.entity.Damage;
import com.wzabek.rentengine.protocol.entity.ProtocolType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface DamageRepository extends JpaRepository<Damage, Long> {

    @Query("""
            select coalesce(sum(d.cost), 0) from Damage d
            where d.protocol.agreement.id = :agreementId and d.protocol.type = :type
            """)
    BigDecimal sumCost(Long agreementId, ProtocolType type);
}
