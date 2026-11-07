package com.wzabek.rentengine.protocol.repository;

import com.wzabek.rentengine.protocol.entity.Damage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DamageRepository extends JpaRepository<Damage, Long> {
}
