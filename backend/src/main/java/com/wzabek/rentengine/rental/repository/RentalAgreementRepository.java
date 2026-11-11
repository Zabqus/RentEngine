package com.wzabek.rentengine.rental.repository;

import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RentalAgreementRepository extends JpaRepository<RentalAgreement, Long>, JpaSpecificationExecutor<RentalAgreement> {

    boolean existsByUnitId(Long unitId);

    boolean existsByClientId(Long clientId);

    @EntityGraph(attributePaths = {"client", "unit", "unit.equipment"})
    List<RentalAgreement> findByClientIdOrderByStartDateDesc(Long clientId);

    @EntityGraph(attributePaths = {"client", "unit", "unit.equipment"})
    List<RentalAgreement> findAllByOrderByStartDateAsc();

    @EntityGraph(attributePaths = {"client", "unit", "unit.equipment"})
    List<RentalAgreement> findByStatusOrderByStartDateAsc(AgreementStatus status);

    boolean existsByUnitIdAndStatusIn(Long unitId, Collection<AgreementStatus> statuses);

    boolean existsByUnitIdAndStatusInAndIdNot(Long unitId, Collection<AgreementStatus> statuses, Long id);

    // R1: przedziały [start, end) nachodzą na siebie. OVERDUE blokuje bez końca, bo sprzęt jeszcze nie wrócił.
    @Query("""
            select count(a) > 0 from RentalAgreement a
            where a.unit.id = :unitId
              and a.startDate < :end
              and (
                    (a.status in (com.wzabek.rentengine.rental.entity.AgreementStatus.RESERVED,
                                  com.wzabek.rentengine.rental.entity.AgreementStatus.ACTIVE)
                     and a.endDate > :start)
                 or a.status = com.wzabek.rentengine.rental.entity.AgreementStatus.OVERDUE
              )
            """)
    boolean existsOverlapping(Long unitId, LocalDateTime start, LocalDateTime end);

    @Override
    @EntityGraph(attributePaths = {"client", "unit", "unit.equipment"})
    Optional<RentalAgreement> findById(Long id);
}
