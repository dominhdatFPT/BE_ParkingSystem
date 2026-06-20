package com.swp.parking.service;

import com.swp.parking.dto.response.AiChatResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.FeePackage;
import com.swp.parking.model.User;
import com.swp.parking.model.VehicleRegistration;
import com.swp.parking.model.VehicleType;
import com.swp.parking.repository.FeePackageRepository;
import com.swp.parking.repository.UserRepository;
import com.swp.parking.repository.VehicleRegistrationRepository;
import com.swp.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AiRegistrationService {

    private final UserRepository userRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final FeePackageRepository feePackageRepository;
    private final VehicleRegistrationRepository registrationRepository;

    @Transactional(readOnly = true)
    public String canonicalVehicleType(String input) {
        return findVehicleType(input).getTypeName();
    }

    @Transactional(readOnly = true)
    public String canonicalPackage(String vehicleTypeInput, String packageInput) {
        VehicleType vehicleType = findVehicleType(vehicleTypeInput);
        return findFeePackage(vehicleType.getId(), packageInput).getName();
    }

    @Transactional
    public Long createPendingRegistration(Long userId, AiChatResponse.CollectedData data) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        VehicleType vehicleType = findVehicleType(data.getVehicleType());
        FeePackage feePackage = findFeePackage(vehicleType.getId(), data.getPackageType());
        String licensePlate = data.getLicensePlate().trim().toUpperCase(Locale.ROOT);

        if (registrationRepository.existsByUser_IdAndLicensePlate(userId, licensePlate)) {
            throw new AppException(HttpStatus.CONFLICT, "Biển số này đã có hồ sơ đăng ký");
        }

        VehicleRegistration registration = VehicleRegistration.builder()
                .user(user)
                .vehicleType(vehicleType)
                .licensePlate(licensePlate)
                .ekycFullName(data.getOwnerName())
                .contactPhone(data.getPhone())
                .requestedFeePackage(feePackage)
                .registrationSource("AI_CHAT")
                .status("PENDING")
                .build();

        return registrationRepository.save(registration).getId();
    }

    private VehicleType findVehicleType(String input) {
        String normalized = normalize(input);
        List<VehicleType> types = vehicleTypeRepository.findAll();

        return types.stream()
                .filter(type -> vehicleTypeMatches(type, normalized))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST,
                        "Loại xe chưa hợp lệ. Vui lòng chọn xe máy hoặc ô tô"));
    }

    private boolean vehicleTypeMatches(VehicleType type, String normalizedInput) {
        String code = normalize(type.getTypeCode());
        String name = normalize(type.getTypeName());
        if (normalizedInput.equals(code) || normalizedInput.equals(name)
                || name.contains(normalizedInput) || normalizedInput.contains(name)) {
            return true;
        }
        if (normalizedInput.contains("xe may") || normalizedInput.contains("motorbike")
                || normalizedInput.contains("moto")) {
            return code.contains("motorbike");
        }
        if (normalizedInput.contains("o to") || normalizedInput.contains("oto")
                || normalizedInput.contains("car")) {
            return code.contains("car");
        }
        return false;
    }

    private FeePackage findFeePackage(Long vehicleTypeId, String input) {
        String normalized = normalize(input);
        List<FeePackage> packages = feePackageRepository.findByVehicleType_IdAndIsActiveTrue(vehicleTypeId);

        return packages.stream()
                .filter(feePackage -> packageMatches(feePackage, normalized))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST,
                        "Gói đăng ký chưa hợp lệ. Vui lòng chọn gói tháng, quý, nửa năm hoặc năm"));
    }

    private boolean packageMatches(FeePackage feePackage, String normalizedInput) {
        String name = normalize(feePackage.getName());
        if (normalizedInput.equals(name) || normalizedInput.contains(name) || name.contains(normalizedInput)) {
            return true;
        }
        return switch (feePackage.getDurationMonths()) {
            case 1 -> normalizedInput.contains("thang") && !normalizedInput.contains("6");
            case 3 -> normalizedInput.contains("quy") || normalizedInput.contains("3 thang");
            case 6 -> normalizedInput.contains("nua nam") || normalizedInput.contains("6 thang");
            case 12 -> normalizedInput.equals("nam") || normalizedInput.contains("1 nam")
                    || normalizedInput.contains("12 thang");
            default -> false;
        };
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
