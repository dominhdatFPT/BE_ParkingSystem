package com.swp.parking.service;

import com.swp.parking.dto.response.AiChatResponse;
import com.swp.parking.model.VehicleRegistration;
import com.swp.parking.repository.UserRepository;
import com.swp.parking.repository.VehicleRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AiRegistrationServiceIntegrationTests {

    @Autowired
    private AiRegistrationService aiRegistrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRegistrationRepository registrationRepository;

    @Test
    void savesAllCollectedFieldsAsPendingRegistration() {
        Long userId = userRepository.findAll().stream()
                .findFirst()
                .orElseThrow()
                .getId();
        String licensePlate = "AI-TEST-" + System.currentTimeMillis();

        AiChatResponse.CollectedData data = AiChatResponse.CollectedData.builder()
                .ownerName("Nguyen Van An")
                .phone("0901234567")
                .vehicleType("Xe máy (2 bánh)")
                .licensePlate(licensePlate)
                .packageType("Gói tháng")
                .build();

        Long registrationId = aiRegistrationService.createPendingRegistration(userId, data);
        VehicleRegistration saved = registrationRepository.findById(registrationId).orElseThrow();

        assertThat(saved.getUser().getId()).isEqualTo(userId);
        assertThat(saved.getEkycFullName()).isEqualTo("Nguyen Van An");
        assertThat(saved.getContactPhone()).isEqualTo("0901234567");
        assertThat(saved.getVehicleType().getTypeCode()).isEqualTo("MOTORBIKE");
        assertThat(saved.getLicensePlate()).isEqualTo(licensePlate);
        assertThat(saved.getRequestedFeePackage().getName()).isEqualTo("Gói tháng");
        assertThat(saved.getRegistrationSource()).isEqualTo("AI_CHAT");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
    }
}
