package com.wzabek.rentengine.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipment_units")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(name = "serial_or_plate", nullable = false, unique = true)
    private String serialOrPlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitStatus status = UnitStatus.AVAILABLE;

    // Kilometry dla aut, motogodziny dla maszyn
    @Column(name = "mileage_or_hours", nullable = false)
    private int mileageOrHours;

    @Version
    private Long version;

    public EquipmentUnit(Equipment equipment, String serialOrPlate, int mileageOrHours) {
        this.equipment = equipment;
        this.serialOrPlate = serialOrPlate;
        this.mileageOrHours = mileageOrHours;
    }
}
