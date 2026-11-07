package com.wzabek.rentengine.rental.repository;

import com.wzabek.rentengine.rental.entity.RentalAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RentalAgreementRepository extends JpaRepository<RentalAgreement, Long>, JpaSpecificationExecutor<RentalAgreement> {
}
