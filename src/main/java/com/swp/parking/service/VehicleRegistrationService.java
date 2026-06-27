package com.swp.parking.service;

import com.swp.parking.config.EkycProperties;
import com.swp.parking.dto.ekyc.EkycCccdResult;
import com.swp.parking.dto.ekyc.EkycLicenseResult;
import com.swp.parking.dto.ekyc.EkycValidationResult;
import com.swp.parking.dto.request.AdminReviewRequest;
import com.swp.parking.dto.request.VehicleRegistrationRequest;
import com.swp.parking.dto.response.VehicleRegistrationResponse;
import com.swp.parking.exception.AlreadyDeletedException;
import com.swp.parking.exception.AppException;
import com.swp.parking.exception.DuplicateLicensePlateException;
import com.swp.parking.exception.NotFoundException;
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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final EkycProperties ekycProperties;

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
                .userFullName(reg.getUser().getFullName())
                .vehicleTypeId(reg.getVehicleType().getId())
                .vehicleTypeName(reg.getVehicleType().getTypeName())
                .licensePlate(reg.getLicensePlate())
                .contactPhone(reg.getContactPhone())
                .requestedFeePackageId(reg.getRequestedFeePackage() != null
                        ? reg.getRequestedFeePackage().getId() : null)
                .requestedFeePackageName(reg.getRequestedFeePackage() != null
                        ? reg.getRequestedFeePackage().getName() : null)
                .registrationSource(reg.getRegistrationSource())
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
                .vehicleDocumentImage(reg.getVehicleDocumentImage())
                .plateImage(reg.getPlateImage())
                .createdAt(reg.getCreatedAt())
                .reviewedAt(reg.getReviewedAt())
                .build();
    }

    private VehicleRegistrationResponse toSummaryResponse(
            VehicleRegistrationRepository.VehicleRegistrationSummary reg) {
        return VehicleRegistrationResponse.builder()
                .registrationId(reg.getRegistrationId())
                .userId(reg.getUserId())
                .userFullName(reg.getUserFullName())
                .vehicleTypeId(reg.getVehicleTypeId())
                .vehicleTypeName(reg.getVehicleTypeName())
                .licensePlate(reg.getLicensePlate())
                .contactPhone(reg.getContactPhone())
                .requestedFeePackageId(reg.getRequestedFeePackageId())
                .requestedFeePackageName(reg.getRequestedFeePackageName())
                .registrationSource(reg.getRegistrationSource())
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

        String submittedPlate = normalizeLicensePlate(request.getLicensePlate());
        String ocrPlate = normalizeLicensePlate(ekycService.ocrLicensePlate(request.getPlateImage()));
        String detectedPlate = ocrPlate;
        if (detectedPlate.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Khong doc duoc bien so xe tu anh. Vui long chup ro bien so va thu lai");
        }

        if (ekycProperties.isValidationEnabled()
                && !submittedPlate.isBlank()
                && !containsPlate(ocrPlate, submittedPlate)) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Biển số nhập vào không khớp với ảnh biển số");
        }

        if (registrationRepository.existsByLicensePlateAndIsDeletedFalse(detectedPlate)) {
            throw new DuplicateLicensePlateException("Biển số " + detectedPlate + " đã tồn tại trong hệ thống");
        }

        EkycCccdResult cccd = ekycService.ocrCccd(request.getCccdFrontImage());
        EkycLicenseResult license = ekycService.ocrLicense(request.getLicenseImage());
        String vehicleDocumentText = ekycService.ocrVehicleDocument(request.getVehicleDocumentImage());
        String detectedBrand = firstNonBlank(
                extractVehicleDocumentField(vehicleDocumentText,
                        "(?:NHÃN HIỆU|NHAN HIEU)(?:\\s*/\\s*(?:BRAND|MARK))?|BRAND|MARK"),
                request.getBrand());
        String detectedColor = firstNonBlank(
                extractVehicleDocumentField(vehicleDocumentText,
                        "(?:MÀU SƠN|MAU SON)(?:\\s*/\\s*COLOR)?|COLOR"),
                request.getColor());
        EkycValidationResult validation = null;
        Double confidenceScore = null;
        if (ekycProperties.isValidationEnabled()) {
            validation = requireValidDocument("CCCD mặt trước", request.getCccdFrontImage());
            requireValidDocument("CCCD mặt sau", request.getCccdBackImage());
            requireValidDocument("bằng lái xe", request.getLicenseImage());
            requireValidDocument("giấy đăng ký xe", request.getVehicleDocumentImage());
            confidenceScore = validation != null ? validation.getConfidenceScore() : null;

            if (cccd == null || cccd.getFullName() == null || cccd.getFullName().isBlank()) {
                throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Không đọc được họ tên trên CCCD");
            }
            if (license == null || license.getFullName() == null || license.getFullName().isBlank()) {
                throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Không đọc được họ tên trên bằng lái xe");
            }
            if (!samePersonName(cccd.getFullName(), license.getFullName())) {
                throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Họ tên trên CCCD và bằng lái xe không khớp");
            }
            if (!containsPlate(vehicleDocumentText, detectedPlate)) {
                throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Biển số trên giấy đăng ký xe không khớp với ảnh biển số");
            }
        } else {
            log.warn("eKYC validation đang TẮT (ekyc.validation-enabled=false) - bỏ qua đối chiếu "
                    + "chất lượng/độ tin cậy; dữ liệu xe vẫn được OCR từ ảnh và kiểm tra trùng biển số. "
                    + "CHỈ dùng để test, nhớ bật lại trước khi demo/nghiệm thu.");
        }
        if (ekycProperties.isValidationEnabled()
                && cccd != null && cccd.getId() != null
                && registrationRepository.existsByEkycCccdIdAndUser_IdNot(cccd.getId(), userId)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "CCCD này đã được sử dụng cho một tài khoản khác");
        }
        if (ekycProperties.isValidationEnabled()
                && license != null && license.getLicenseNumber() != null
                && registrationRepository.existsByEkycLicenseNumberAndUser_IdNot(license.getLicenseNumber(), userId)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Bằng lái này đã được sử dụng cho một tài khoản khác");
        }

        VehicleRegistration registration = VehicleRegistration.builder()
                .user(user)
                .vehicleType(vehicleType)
                .licensePlate(detectedPlate)
                .brand(detectedBrand)
                .color(detectedColor)
                .cccdFrontImage(request.getCccdFrontImage())
                .cccdBackImage(request.getCccdBackImage())
                .licenseImage(request.getLicenseImage())
                .vehicleDocumentImage(request.getVehicleDocumentImage())
                .plateImage(request.getPlateImage())
                .ekycCccdId(limit(cccd != null ? cccd.getId() : null, 255))
                .ekycFullName(limit(cccd != null ? cccd.getFullName() : null, 255))
                .ekycDateOfBirth(cccd != null ? parseDate(cccd.getDateOfBirth()) : null)
                .ekycGender(limit(cccd != null ? cccd.getGender() : null, 255))
                .ekycNationality(limit(cccd != null ? cccd.getNationality() : null, 255))
                .ekycPlaceOfOrigin(cccd != null ? cccd.getPlaceOfOrigin() : null)
                .ekycPlaceOfResidence(cccd != null ? cccd.getPlaceOfResidence() : null)
                .ekycCccdIssueDate(cccd != null ? parseDate(cccd.getIssueDate()) : null)
                .ekycCccdExpiryDate(cccd != null ? parseDate(cccd.getExpiryDate()) : null)
                .ekycLicenseNumber(limit(license != null ? license.getLicenseNumber() : null, 255))
                .ekycLicenseClass(limit(license != null ? license.getLicenseClass() : null, 255))
                .ekycLicenseIssueDate(license != null ? parseDate(license.getIssueDate()) : null)
                .ekycLicenseExpiry(license != null ? parseDate(license.getExpiryDate()) : null)
                .ekycIssuingAuthority(license != null ? license.getIssuingAuthority() : null)
                .ekycIsValid(validation != null ? validation.getIsValid() : null)
                .ekycIsFake(validation != null ? validation.getIsFake() : null)
                .ekycConfidenceScore(confidenceScore)
                .ekycDocumentType(limit(validation != null ? validation.getDocumentType() : null, 255))
                .registrationSource("VERIFIED_DOCUMENTS")
                .status("PENDING")
                .build();

        return toResponse(registrationRepository.save(registration));
    }

    private EkycValidationResult requireValidDocument(String label, String image) {
        EkycValidationResult result = ekycService.validateDocument(image);
        if (result == null || Boolean.TRUE.equals(result.getIsFake())) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, label + " không hợp lệ hoặc có dấu hiệu giả mạo");
        }
        if (!Boolean.TRUE.equals(result.getIsValid())
                || result.getConfidenceScore() == null
                || result.getConfidenceScore() < 70) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                    label + " bị mờ, thiếu nội dung hoặc không đủ chất lượng");
        }
        return result;
    }

    private boolean samePersonName(String first, String second) {
        String normalizedFirst = normalizePersonName(first);
        String normalizedSecond = normalizePersonName(second);
        if (normalizedFirst.equals(normalizedSecond)) {
            return true;
        }
        java.util.Set<String> firstTokens = new java.util.HashSet<>(java.util.List.of(normalizedFirst.split(" ")));
        java.util.Set<String> secondTokens = new java.util.HashSet<>(java.util.List.of(normalizedSecond.split(" ")));
        return firstTokens.equals(secondTokens);
    }

    private boolean containsPlate(String documentText, String plate) {
        String normalizedDocument = documentText == null
                ? ""
                : documentText.toUpperCase().replaceAll("[^A-Z0-9]", "");
        String normalizedPlate = plate == null
                ? ""
                : plate.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return !normalizedPlate.isBlank() && normalizedDocument.contains(normalizedPlate);
    }

    private String normalizeLicensePlate(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String extractVehicleDocumentField(String text, String labelPattern) {
        if (text == null || text.isBlank()) return null;
        Matcher matcher = Pattern.compile(
                "(?iu)(?:^|[\\r\\n])\\s*(?:" + labelPattern + ")\\s*[:\\-]?\\s*([^\\r\\n]+)")
                .matcher(text);
        if (!matcher.find()) return null;
        String value = matcher.group(1).trim().replaceAll("\\s+", " ");
        return value.isBlank() ? null : value;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second == null || second.isBlank() ? null : second.trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String normalizePersonName(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();
    }

    @Transactional(readOnly = true)
    public List<VehicleRegistrationResponse> getMyRegistrations(Long userId) {
        log.info("Getting registrations for userId: {}", userId);
        return registrationRepository.findSummariesByUserId(userId)
                .stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<VehicleRegistrationResponse> getPendingRegistrations(int page, int size) {
        log.info("Admin fetching pending page: {}", page);
        return registrationRepository.findSummaries("PENDING", PageRequest.of(page, size))
                .map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<VehicleRegistrationResponse> getRegistrations(String status, int page, int size) {
        log.info("Back-office fetching registrations status: {}, page: {}", status, page);
        PageRequest pageRequest = PageRequest.of(page, size);

        String normalizedStatus = (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status))
                ? null
                : status.toUpperCase();
        return registrationRepository.findSummaries(normalizedStatus, pageRequest)
                .map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public VehicleRegistrationResponse getById(Long registrationId, Long requestingUserId, String role) {
        VehicleRegistration reg = registrationRepository.findByIdAndNotDeleted(registrationId)
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

        VehicleRegistration reg = registrationRepository.findByIdAndNotDeleted(registrationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký"));

        if (!"PENDING".equals(reg.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "Đăng ký đã được xử lý rồi");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Admin không tồn tại"));

        if ("APPROVED".equals(dto.getStatus())) {
            Customer customer = customerRepository.findByUser_Id(reg.getUser().getId())
                    .orElseGet(() -> customerRepository.save(Customer.builder()
                            .user(reg.getUser())
                            .build()));

            Vehicle vehicle = vehicleRepository.findByLicensePlate(reg.getLicensePlate())
                    .map(existingVehicle -> {
                        Long vehicleOwnerId = existingVehicle.getCustomer().getUser().getId();
                        if (!vehicleOwnerId.equals(reg.getUser().getId())) {
                            throw new AppException(HttpStatus.CONFLICT,
                                    "Biển số đã thuộc một tài khoản khác");
                        }
                        if (!existingVehicle.getVehicleType().getId().equals(reg.getVehicleType().getId())) {
                            throw new AppException(HttpStatus.CONFLICT,
                                    "Biển số đã được đăng ký với một loại xe khác");
                        }
                        return existingVehicle;
                    })
                    .orElseGet(() -> vehicleRepository.save(Vehicle.builder()
                            .customer(customer)
                            .vehicleType(reg.getVehicleType())
                            .licensePlate(reg.getLicensePlate())
                            .brand(reg.getBrand())
                            .color(reg.getColor())
                            .build()));

            reg.setVehicle(vehicle);
        }

        reg.setStatus(dto.getStatus());
        reg.setRejectReason(dto.getRejectReason());
        reg.setReviewedBy(admin);
        reg.setReviewedAt(LocalDateTime.now());

        return toResponse(registrationRepository.save(reg));
    }

    @Transactional
    public VehicleRegistrationResponse softDelete(Long registrationId, Long deletedByUserId) {
        log.info("Soft-deleting registration {} by userId {}", registrationId, deletedByUserId);

        VehicleRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đăng ký xe với ID: " + registrationId));

        if (Boolean.TRUE.equals(reg.getIsDeleted())) {
            throw new AlreadyDeletedException("Xe này đã bị xóa trước đó");
        }

        User reviewer = userRepository.findById(deletedByUserId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        reg.setIsDeleted(true);
        reg.setDeletedAt(LocalDateTime.now());
        reg.setReviewedBy(reviewer);
        reg.setReviewedAt(LocalDateTime.now());

        return toResponse(registrationRepository.save(reg));
    }
}
