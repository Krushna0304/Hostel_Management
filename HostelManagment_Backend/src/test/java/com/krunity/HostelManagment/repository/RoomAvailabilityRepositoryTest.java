package com.krunity.HostelManagment.repository;

import com.krunity.HostelManagment.dto.RoomAvailabilityResponse;
import com.krunity.HostelManagment.enums.PaymentFrequency;
import com.krunity.HostelManagment.enums.RoomAllotmentStatus;
import com.krunity.HostelManagment.enums.RoomType;
import com.krunity.HostelManagment.enums.TransactionMode;
import com.krunity.HostelManagment.enums.TransactionStatus;
import com.krunity.HostelManagment.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RoomAvailabilityRepositoryTest {

    @Autowired
    private RoomAvailabilityRepository roomAvailabilityRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    @Autowired
    private HostelRepository hostelRepository;

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TenantPaymentPlanRepository tenantPaymentPlanRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private RoomAllotmentRepository roomAllotmentRepository;

    private UUID floorId;
    private Room room;
    private PaymentType paymentType;
    private User owner;

    @BeforeEach
    void setUp() {
        Role ownerRole = roleRepository.save(Role.builder().name("OWNER").build());
        owner = userRepository.save(User.builder()
                .displayName("Owner")
                .username("owner-" + UUID.randomUUID())
                .passwordHash("hash")
                .phoneNumber("9000000000")
                .role(ownerRole)
                .isActive(true)
                .build());

        paymentType = paymentTypeRepository.save(PaymentType.builder().typeName("MONTHLY").build());

        Hostel hostel = hostelRepository.save(Hostel.builder()
                .hostelName("Test Hostel")
                .hostelAddress("Test Address")
                .owner(owner)
                .build());

        Floor floor = floorRepository.save(Floor.builder()
                .hostel(hostel)
                .floorNumber(1)
                .totalRooms(1)
                .build());
        floorId = floor.getFloorId();

        room = roomRepository.save(Room.builder()
                .hostel(hostel)
                .floor(floor)
                .roomNumber("A-101")
                .totalBeds(10)
                .availableBeds(0)
                .isActive(true)
                .roomType(RoomType.PG_ROOM)
                .build());

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate settlingTenantEndDate = LocalDate.of(2026, 6, 28);

        for (int i = 0; i < 10; i++) {
            LocalDate allotmentEndDate = (i == 0) ? settlingTenantEndDate : null;
            Role tenantRole = roleRepository.save(Role.builder().name("TENANT-" + i).build());
            User tenant = userRepository.save(User.builder()
                    .displayName("Tenant " + i)
                    .username("tenant-" + i + "-" + UUID.randomUUID())
                    .passwordHash("hash")
                    .phoneNumber("910000000" + i)
                    .role(tenantRole)
                    .isActive(true)
                    .build());

            TenantPaymentPlan plan = tenantPaymentPlanRepository.save(TenantPaymentPlan.builder()
                    .tenant(tenant)
                    .paymentType(paymentType)
                    .TenantPlanId("TPP-" + tenant.getUserId())
                    .agreementId("AGR-" + i)
                    .paymentFrequency(PaymentFrequency.MONTHLY)
                    .depositAmount(1000L)
                    .installmentAmount(5000L)
                    .startDate(startDate)
                    .endDate(allotmentEndDate)
                    .pendingInstallments(12)
                    .isActive(true)
                    .build());

            Transaction transaction = transactionRepository.save(Transaction.builder()
                    .planId(plan)
                    .fromUser(tenant)
                    .toUser(owner)
                    .amount(1000L)
                    .mode(TransactionMode.CASH)
                    .status(TransactionStatus.COMPLETED)
                    .reason("deposit")
                    .build());

            roomAllotmentRepository.save(RoomAllotment.builder()
                    .room(room)
                    .tenant(tenant)
                    .agreementId("AGR-" + i)
                    .paymentPlanId(plan)
                    .depositTransactionId(transaction)
                    .startDate(startDate)
                    .endDate(allotmentEndDate)
                    .roomAllotmentStatus(RoomAllotmentStatus.ACTIVE)
                    .build());
        }
    }

    @Test
    void findAvailableRooms_excludesFullyOccupiedRoomBeforeTenantEndDate() {
        List<RoomAvailabilityResponse> results = roomAvailabilityRepository.findAvailableRoomsByFloor(
                floorId,
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 15),
                RoomAllotmentStatus.occupyingStatuses(),
                null);

        assertTrue(results.isEmpty());
    }

    @Test
    void findAvailableRooms_includesRoomWithOneBedAfterTenantEndDate() {
        List<RoomAvailabilityResponse> results = roomAvailabilityRepository.findAvailableRoomsByFloor(
                floorId,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 1),
                RoomAllotmentStatus.occupyingStatuses(),
                null);

        assertEquals(1, results.size());
        RoomAvailabilityResponse availableRoom = results.get(0);
        assertEquals(room.getRoomId(), availableRoom.getRoomId());
        assertEquals("A-101", availableRoom.getRoomName());
        assertEquals(10, availableRoom.getTotalBeds());
        assertEquals(1, availableRoom.getAvailableBeds());
    }
}
