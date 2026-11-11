package com.wzabek.rentengine.protocol.service;

import com.wzabek.rentengine.catalog.entity.Category;
import com.wzabek.rentengine.catalog.entity.CategoryType;
import com.wzabek.rentengine.catalog.entity.Equipment;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.catalog.entity.UnitStatus;
import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.common.exception.ForbiddenOperationException;
import com.wzabek.rentengine.common.exception.InvalidRequestException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import com.wzabek.rentengine.protocol.dto.DamageRequest;
import com.wzabek.rentengine.protocol.dto.DamageResponse;
import com.wzabek.rentengine.protocol.dto.ProtocolRequest;
import com.wzabek.rentengine.protocol.dto.ProtocolResponse;
import com.wzabek.rentengine.protocol.entity.Damage;
import com.wzabek.rentengine.protocol.entity.DamageSeverity;
import com.wzabek.rentengine.protocol.entity.Protocol;
import com.wzabek.rentengine.protocol.entity.ProtocolType;
import com.wzabek.rentengine.protocol.mapper.ProtocolMapper;
import com.wzabek.rentengine.protocol.repository.DamageRepository;
import com.wzabek.rentengine.protocol.repository.ProtocolRepository;
import com.wzabek.rentengine.protocol.storage.PhotoStorage;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.rental.service.RentalService;
import com.wzabek.rentengine.settlement.repository.SettlementRepository;
import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.repository.UserRepository;
import com.wzabek.rentengine.user.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtocolServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime START = LocalDateTime.of(2030, 6, 1, 10, 0);
    private static final LocalDateTime NOW = START.minusHours(1);
    private static final Long AGREEMENT_ID = 10L;
    private static final Long EMPLOYEE_ID = 3L;

    @Mock
    private ProtocolRepository protocolRepository;
    @Mock
    private DamageRepository damageRepository;
    @Mock
    private RentalAgreementRepository agreementRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RentalService rentalService;
    @Mock
    private PhotoStorage photoStorage;

    private Equipment equipment;
    private EquipmentUnit unit;
    private RentalAgreement agreement;
    private User employee;

    @BeforeEach
    void setUp() {
        Category category = new Category("Minikoparki", CategoryType.EXCAVATOR, List.of());
        equipment = new Equipment("Kubota U17-3", category, new BigDecimal("450.00"),
                new BigDecimal("3000.00"), new BigDecimal("20.00"), Map.of());
        unit = new EquipmentUnit(equipment, "KUB-1", 100);
        ReflectionTestUtils.setField(unit, "id", 5L);
        User client = new User("jan@test.pl", "hash", "Jan", "Kowalski", Role.CLIENT);
        agreement = new RentalAgreement(client, unit, START, START.plusDays(2),
                new BigDecimal("900.00"), new BigDecimal("3000.00"));
        ReflectionTestUtils.setField(agreement, "id", AGREEMENT_ID);
        employee = new User("emp@test.pl", "hash", "Piotr", "Pracownik", Role.EMPLOYEE);
        ReflectionTestUtils.setField(employee, "id", EMPLOYEE_ID);
    }

    private ProtocolService serviceAt(LocalDateTime now) {
        Clock clock = Clock.fixed(now.atZone(ZONE).toInstant(), ZONE);
        return new ProtocolService(protocolRepository, damageRepository, agreementRepository, settlementRepository,
                userRepository, rentalService, new ProtocolMapper(), photoStorage, clock);
    }

    private void givenAgreementAndEmployee() {
        when(rentalService.getAgreement(AGREEMENT_ID)).thenReturn(agreement);
        when(userRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        lenient().when(protocolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ProtocolRequest request(ProtocolType type, String fuel, int mileage) {
        return new ProtocolRequest(type, fuel == null ? null : new BigDecimal(fuel), mileage, "  ");
    }

    @Test
    void issueActivatesAgreementAndUpdatesUnitReading() {
        givenAgreementAndEmployee();

        ProtocolResponse response = serviceAt(NOW).create(AGREEMENT_ID, request(ProtocolType.ISSUE, "20.00", 120), EMPLOYEE_ID);

        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.ACTIVE);
        assertThat(unit.getMileageOrHours()).isEqualTo(120);
        assertThat(response.type()).isEqualTo(ProtocolType.ISSUE);
        assertThat(response.employeeName()).isEqualTo("Piotr Pracownik");
        assertThat(response.notes()).isNull();
    }

    @Test
    void issueOfUnitInServiceIsRejected() {
        givenAgreementAndEmployee();
        unit.setStatus(UnitStatus.IN_SERVICE);

        assertThatThrownBy(() -> serviceAt(NOW).create(AGREEMENT_ID, request(ProtocolType.ISSUE, "20.00", 100), EMPLOYEE_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("UNIT_IN_SERVICE");
        verify(protocolRepository, never()).save(any());
    }

    @Test
    void issueOfUnitStillRentedByAnotherClientIsRejected() {
        givenAgreementAndEmployee();
        when(agreementRepository.existsByUnitIdAndStatusInAndIdNot(eq(5L), any(), eq(AGREEMENT_ID))).thenReturn(true);

        assertThatThrownBy(() -> serviceAt(NOW).create(AGREEMENT_ID, request(ProtocolType.ISSUE, "20.00", 100), EMPLOYEE_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("UNIT_RENTED_OUT");
    }

    @Test
    void issueBeforeStartDayIsRejected() {
        givenAgreementAndEmployee();

        assertThatThrownBy(() -> serviceAt(START.minusDays(1)).create(AGREEMENT_ID, request(ProtocolType.ISSUE, "20.00", 100), EMPLOYEE_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("TOO_EARLY_TO_ISSUE");
    }

    @Test
    void issueAfterRentalPeriodIsRejected() {
        givenAgreementAndEmployee();

        assertThatThrownBy(() -> serviceAt(START.plusDays(2)).create(AGREEMENT_ID, request(ProtocolType.ISSUE, "20.00", 100), EMPLOYEE_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("RENTAL_PERIOD_ENDED");
    }

    @Test
    void invalidReadingsAreRejectedWithFieldErrors() {
        givenAgreementAndEmployee();

        assertThatThrownBy(() -> serviceAt(NOW).create(AGREEMENT_ID, request(ProtocolType.ISSUE, "25.00", 99), EMPLOYEE_ID))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("errors").asInstanceOf(MAP)
                .containsOnlyKeys("fuelLevel", "mileageOrHours");
    }

    @Test
    void fuelLevelIsRequiredForEquipmentWithTank() {
        givenAgreementAndEmployee();

        assertThatThrownBy(() -> serviceAt(NOW).create(AGREEMENT_ID, request(ProtocolType.ISSUE, null, 100), EMPLOYEE_ID))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("errors").asInstanceOf(MAP)
                .containsEntry("fuelLevel", "must not be null");
    }

    @Test
    void fuelLevelIsNotAllowedForEquipmentWithoutTank() {
        givenAgreementAndEmployee();
        equipment.setTankCapacity(null);

        assertThatThrownBy(() -> serviceAt(NOW).create(AGREEMENT_ID, request(ProtocolType.ISSUE, "5.00", 100), EMPLOYEE_ID))
                .isInstanceOf(InvalidRequestException.class)
                .extracting("errors").asInstanceOf(MAP)
                .containsKey("fuelLevel");
    }

    @Test
    void returnMarksAgreementReturnedAndUpdatesUnitReading() {
        givenAgreementAndEmployee();
        agreement.changeStatus(AgreementStatus.ACTIVE);

        serviceAt(START.plusDays(3)).create(AGREEMENT_ID, request(ProtocolType.RETURN, "12.50", 180), EMPLOYEE_ID);

        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.RETURNED);
        assertThat(unit.getMileageOrHours()).isEqualTo(180);
    }

    @Test
    void returnOfNotIssuedAgreementIsRejected() {
        givenAgreementAndEmployee();

        assertThatThrownBy(() -> serviceAt(NOW).create(AGREEMENT_ID, request(ProtocolType.RETURN, "20.00", 100), EMPLOYEE_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");
        verify(protocolRepository, never()).save(any());
    }

    private Protocol givenProtocol(ProtocolType type) {
        Protocol protocol = new Protocol(agreement, type, new BigDecimal("10.00"), 150, null, employee);
        ReflectionTestUtils.setField(protocol, "id", 7L);
        when(protocolRepository.findById(7L)).thenReturn(Optional.of(protocol));
        return protocol;
    }

    @Test
    void majorDamageSendsUnitToService() {
        Protocol protocol = givenProtocol(ProtocolType.RETURN);

        serviceAt(NOW).addDamage(7L, new DamageRequest("Pęknięte ramię", DamageSeverity.MAJOR, new BigDecimal("5000.00")));

        assertThat(unit.getStatus()).isEqualTo(UnitStatus.IN_SERVICE);
        assertThat(protocol.getDamages()).hasSize(1);
        verify(damageRepository).save(any());
    }

    @Test
    void minorDamageKeepsUnitAvailable() {
        givenProtocol(ProtocolType.RETURN);

        serviceAt(NOW).addDamage(7L, new DamageRequest("Rysa", DamageSeverity.MINOR, new BigDecimal("200.00")));

        assertThat(unit.getStatus()).isEqualTo(UnitStatus.AVAILABLE);
    }

    @Test
    void majorDamageOnIssueProtocolIsRejected() {
        givenProtocol(ProtocolType.ISSUE);

        assertThatThrownBy(() -> serviceAt(NOW).addDamage(7L,
                new DamageRequest("Pęknięte ramię", DamageSeverity.MAJOR, new BigDecimal("5000.00"))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("MAJOR_DAMAGE_ON_ISSUE");
        assertThat(unit.getStatus()).isEqualTo(UnitStatus.AVAILABLE);
    }

    @Test
    void damageAfterSettlementIsRejected() {
        givenProtocol(ProtocolType.RETURN);
        when(settlementRepository.existsByAgreementId(anyLong())).thenReturn(true);

        assertThatThrownBy(() -> serviceAt(NOW).addDamage(7L,
                new DamageRequest("Rysa", DamageSeverity.MINOR, new BigDecimal("200.00"))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("AGREEMENT_SETTLED");
        verify(damageRepository, never()).save(any());
    }

    private Damage givenDamage(String photoPath) {
        Protocol protocol = new Protocol(agreement, ProtocolType.RETURN, new BigDecimal("10.00"), 150, null, employee);
        Damage damage = new Damage("Rysa", DamageSeverity.MINOR, new BigDecimal("200.00"));
        protocol.addDamage(damage);
        ReflectionTestUtils.setField(damage, "id", 20L);
        damage.setPhotoPath(photoPath);
        when(damageRepository.findById(20L)).thenReturn(Optional.of(damage));
        return damage;
    }

    @Test
    void uploadPhotoReplacesPreviousOne() {
        Damage damage = givenDamage("damages/old.jpg");
        MockMultipartFile file = new MockMultipartFile("photo", new byte[]{1});
        when(photoStorage.save(file)).thenReturn("damages/new.jpg");

        DamageResponse response = serviceAt(NOW).uploadPhoto(20L, file);

        assertThat(damage.getPhotoPath()).isEqualTo("damages/new.jpg");
        assertThat(response.photoUrl()).isEqualTo("/api/damages/20/photo");
        verify(photoStorage).delete("damages/old.jpg");
    }

    @Test
    void uploadPhotoAfterSettlementIsRejected() {
        givenDamage(null);
        when(settlementRepository.existsByAgreementId(AGREEMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> serviceAt(NOW).uploadPhoto(20L, new MockMultipartFile("photo", new byte[]{1})))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("AGREEMENT_SETTLED");
        verify(photoStorage, never()).save(any());
    }

    @Test
    void photoOfForeignAgreementIsForbidden() {
        givenDamage("damages/a.jpg");
        AuthenticatedUser stranger = new AuthenticatedUser(99L, "x@test.pl", Role.CLIENT);
        when(rentalService.getAccessibleAgreement(AGREEMENT_ID, stranger))
                .thenThrow(new ForbiddenOperationException("nope"));

        assertThatThrownBy(() -> serviceAt(NOW).loadPhoto(20L, stranger))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(photoStorage, never()).load(any());
    }

    @Test
    void missingPhotoReturnsNotFound() {
        givenDamage(null);
        AuthenticatedUser staff = new AuthenticatedUser(EMPLOYEE_ID, "emp@test.pl", Role.EMPLOYEE);

        assertThatThrownBy(() -> serviceAt(NOW).loadPhoto(20L, staff))
                .isInstanceOf(NotFoundException.class);
    }
}
