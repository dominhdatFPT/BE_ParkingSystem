package com.swp.parking.service;

import com.swp.parking.dto.ekyc.EkycCccdResult;
import com.swp.parking.dto.ekyc.EkycLicenseResult;
import com.swp.parking.dto.ekyc.EkycValidationResult;
import com.swp.parking.dto.request.AdminReviewRequest;
import com.swp.parking.dto.request.VehicleRegistrationRequest;
import com.swp.parking.dto.response.VehicleRegistrationResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleRegistrationService {

    private final VehicleRegistrationRepository registrationRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EkycService ekycService;

    private LocalDate parseDate(String s) {
        if (s == null) return null;
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    private VehicleRegistrationResponse toResponse(VehicleRegistration reg) {
        return VehicleRegistrationResponse.builder()
                .registrationId(reg.getId())
                .userId(reg.getUser().getId())
                .vehicleTypeId(reg.getVehicleType().getId())
                .vehicleTypeName(reg.getVehicleType().getTypeName())
                .licensePlate(reg.getLicensePlate())
                .brand(reg.getBrand())
                .color(reg.getColor())
                .status(reg.getStatus())
                .rejectReason(reg.getRejectReason())
                .ekycFullName(reg.getEkycFullName())
                .ekycCccdId(reg.getEkycCccdId())
                .ekycLicenseNumber(reg.getEkycLicenseNumber())
                .ekycLicenseClass(reg.getEkycLicenseClass())
                .ekycIsValid(reg.getEkycIsValid())
                .ekycIsFake(reg.getEkycIsFake())
                .ekycConfidenceScore(reg.getEkycConfidenceScore())
                .cccdFrontImage(reg.getCccdFrontImage())
                .cccdBackImage(reg.getCccdBackImage())
                .licenseImage(reg.getLicenseImage())
                .plateImage(reg.getPlateImage())
                .createdAt(reg.getCreatedAt())
                .reviewedAt(reg.getReviewedAt())
                .build();
    }

    @Transactional
    public VehicleRegistrationResponse createRegistration(Long userId, VehicleRegistrationRequest request) {
        log.info("Creating vehicle registration for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Loại xe không tồn tại"));

        if (registrationRepository.existsByUser_IdAndLicensePlate(userId, request.getLicensePlate())) {
            throw new AppException(HttpStatus.CONFLICT, "Biển số đã đăng ký");
        }

        EkycCccdResult cccd = ekycService.ocrCccd(request.getCccdFrontImage());
        EkycLicenseResult license = ekycService.ocrLicense(request.getLicenseImage());
        EkycValidationResult validation = ekycService.validateDocument(request.getCccdFrontImage());

        if (validation != null && Boolean.TRUE.equals(validation.getIsFake())) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Tài liệu giả mạo");
        }

        Double confidenceScore = validation != null ? validation.getConfidenceScore() : null;
        if (confidenceScore == null || confidenceScore < 70) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Ảnh không đủ chất lượng");
        }

        VehicleRegistration registration = VehicleRegistration.builder()
                .user(user)
                .vehicleType(vehicleType)
                .licensePlate(request.getLicensePlate())
                .brand(request.getBrand())
                .color(request.getColor())
                .cccdFrontImage(request.getCccdFrontImage())
                .cccdBackImage(request.getCccdBackImage())
                .licenseImage(request.getLicenseImage())
                .plateImage(request.getPlateImage())
                .ekycCccdId(cccd != null ? cccd.getId() : null)
                .ekycFullName(cccd != null ? cccd.getFullName() : null)
                .ekycDateOfBirth(cccd != null ? parseDate(cccd.getDateOfBirth()) : null)
                .ekycGender(cccd != null ? cccd.getGender() : null)
                .ekycNationality(cccd != null ? cccd.getNationality() : null)
                .ekycPlaceOfOrigin(cccd != null ? cccd.getPlaceOfOrigin() : null)
                .ekycPlaceOfResidence(cccd != null ? cccd.getPlaceOfResidence() : null)
                .ekycCccdIssueDate(cccd != null ? parseDate(cccd.getIssueDate()) : null)
                .ekycCccdExpiryDate(cccd != null ? parseDate(cccd.getExpiryDate()) : null)
                .ekycLicenseNumber(license != null ? license.getLicenseNumber() : null)
                .ekycLicenseClass(license != null ? license.getLicenseClass() : null)
                .ekycLicenseIssueDate(license != null ? parseDate(license.getIssueDate()) : null)
                .ekycLicenseExpiry(license != null ? parseDate(license.getExpiryDate()) : null)
                .ekycIssuingAuthority(license != null ? license.getIssuingAuthority() : null)
                .ekycIsValid(validation != null ? validation.getIsValid() : null)
                .ekycIsFake(validation != null ? validation.getIsFake() : null)
                .ekycConfidenceScore(confidenceScore)
                .ekycDocumentType(validation != null ? validation.getDocumentType() : null)
                .status("PENDING")
                .build();

        return toResponse(registrationRepository.save(registration));
    }

    @Transactional(readOnly = true)
    public List<VehicleRegistrationResponse> getMyRegistrations(Long userId) {
        log.info("Getting registrations for userId: {}", userId);
        return registrationRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<VehicleRegistrationResponse> getPendingRegistrations(int page, int size) {
        log.info("Admin fetching pending page: {}", page);
        return registrationRepository
                .findByStatusOrderByCreatedAtDesc("PENDING", PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<VehicleRegistrationResponse> getRegistrations(String status, int page, int size) {
        log.info("Back-office fetching registrations status: {}, page: {}", status, page);
        PageRequest pageRequest = PageRequest.of(page, size);

        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return registrationRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                    .map(this::toResponse);
        }

        return registrationRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageRequest)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleRegistrationResponse getById(Long registrationId, Long requestingUserId, String role) {
        VehicleRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký"));

        if (!"ADMIN".equals(role) && !"STAFF".equals(role) && !reg.getUser().getId().equals(requestingUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Không có quyền truy cập");
        }

        return toResponse(reg);
    }

    @Transactional
    public VehicleRegistrationResponse adminReview(Long registrationId, Long adminUserId, AdminReviewRequest dto) {
        log.info("Admin {} reviewing registration {}: {}", adminUserId, registrationId, dto.getStatus());

        if (!"APPROVED".equals(dto.getStatus()) && !"REJECTED".equals(dto.getStatus())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Status không hợp lệ");
        }

        if ("REJECTED".equals(dto.getStatus())
                && (dto.getRejectReason() == null || dto.getRejectReason().isBlank())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cần nhập lý do từ chối");
        }

        VehicleRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký"));

        if (!"PENDING".equals(reg.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "Đăng ký đã được xử lý rồi");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Admin không tồn tại"));

        if ("APPROVED".equals(dto.getStatus())) {
            Customer customer = customerRepository.findByUser_Id(reg.getUser().getId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Customer profile chưa tồn tại"));

            Vehicle vehicle = Vehicle.builder()
                    .customer(customer)
                    .vehicleType(reg.getVehicleType())
                    .licensePlate(reg.getLicensePlate())
                    .brand(reg.getBrand())
                    .color(reg.getColor())
                    .build();

            reg.setVehicle(vehicleRepository.save(vehicle));
        }

        reg.setStatus(dto.getStatus());
        reg.setRejectReason(dto.getRejectReason());
        reg.setReviewedBy(admin);
        reg.setReviewedAt(LocalDateTime.now());

        return toResponse(registrationRepository.save(reg));
    }
}
