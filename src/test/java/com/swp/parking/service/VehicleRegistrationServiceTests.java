package com.swp.parking.service;

import com.swp.parking.config.EkycProperties;
import com.swp.parking.dto.request.AdminReviewRequest;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.Customer;
import com.swp.parking.model.User;
import com.swp.parking.model.Vehicle;
import com.swp.parking.model.VehicleRegistration;
import com.swp.parking.model.VehicleType;
import com.swp.parking.repository.CustomerRepository;
import com.swp.parking.repository.UserRepository;
import com.swp.parking.repository.VehicleRegistrationRepository;
import com.swp.parking.repository.VehicleRepository;
import com.swp.parking.repository.VehicleTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleRegistrationServiceTests {

    @Mock VehicleRegistrationRepository registrationRepository;
    @Mock VehicleTypeRepository vehicleTypeRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock UserRepository userRepository;
    @Mock CustomerRepository customerRepository;
    @Mock EkycService ekycService;
    @Mock EkycProperties ekycProperties;

    @InjectMocks VehicleRegistrationService service;

    @Test
    void approveReusesVehicleWhenPlateAlreadyBelongsToUser() {
        ReviewFixture fixture = fixture(10L);
        stubReview(fixture);

        var response = service.adminReview(15L, 20L, new AdminReviewRequest("APPROVED", null));

        assertSame(fixture.vehicle, fixture.registration.getVehicle());
        assertEquals("APPROVED", fixture.registration.getStatus());
        assertEquals("Trần Minh Đạt", response.getUserFullName());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void approveRejectsPlateOwnedByAnotherUser() {
        ReviewFixture fixture = fixture(11L);
        stubReview(fixture);

        AppException exception = assertThrows(AppException.class,
                () -> service.adminReview(15L, 20L, new AdminReviewRequest("APPROVED", null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
        verify(registrationRepository, never()).save(any(VehicleRegistration.class));
    }

    private void stubReview(ReviewFixture fixture) {
        when(registrationRepository.findById(15L)).thenReturn(Optional.of(fixture.registration));
        when(userRepository.findById(20L)).thenReturn(Optional.of(fixture.admin));
        when(customerRepository.findByUser_Id(10L)).thenReturn(Optional.of(fixture.applicantCustomer));
        when(vehicleRepository.findByLicensePlate("51F-12345")).thenReturn(Optional.of(fixture.vehicle));
        lenient().when(registrationRepository.save(fixture.registration)).thenReturn(fixture.registration);
    }

    private ReviewFixture fixture(Long existingVehicleOwnerId) {
        User applicant = User.builder().id(10L).fullName("Trần Minh Đạt").build();
        User admin = User.builder().id(20L).build();
        Customer applicantCustomer = Customer.builder().id(30L).user(applicant).build();
        Customer vehicleCustomer = Customer.builder().id(31L)
                .user(User.builder().id(existingVehicleOwnerId).build())
                .build();
        VehicleType type = VehicleType.builder().id(1L).typeName("MOTORBIKE").build();
        Vehicle vehicle = Vehicle.builder().id(40L).customer(vehicleCustomer)
                .vehicleType(type).licensePlate("51F-12345").build();
        VehicleRegistration registration = VehicleRegistration.builder().id(15L).user(applicant)
                .vehicleType(type).licensePlate("51F-12345").status("PENDING").build();
        return new ReviewFixture(admin, applicantCustomer, vehicle, registration);
    }

    private record ReviewFixture(
            User admin,
            Customer applicantCustomer,
            Vehicle vehicle,
            VehicleRegistration registration) {
    }
}
