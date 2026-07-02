package com.swp.parking.service;

import com.swp.parking.config.EkycProperties;
import com.swp.parking.dto.ekyc.EkycCccdResult;
import com.swp.parking.dto.ekyc.EkycLicenseResult;
import com.swp.parking.dto.ekyc.EkycValidationResult;
import com.swp.parking.dto.request.AdminReviewRequest;
import com.swp.parking.dto.request.SubscriptionRegisterRequest;
import com.swp.parking.dto.request.VehicleRegistrationRequest;
import com.swp.parking.dto.response.VehicleRegistrationResponse;
import com.swp.parking.exception.AlreadyDeletedException;
import com.swp.parking.exception.AppException;
import com.swp.parking.exception.DuplicateLicensePlateException;
import com.swp.parking.exception.NotFoundException;
import com.swp.parking.model.Customer;
import com.swp.parking.model.FeePackage;
import com.swp.parking.model.FeeSubscription;
import com.swp.parking.model.User;
import com.swp.parking.model.Vehicle;
import com.swp.parking.model.VehicleRegistration;
import com.swp.parking.model.VehicleType;
import com.swp.parking.model.enums.SubscriptionStatus;
import com.swp.parking.repository.CustomerRepository;
import com.swp.parking.repository.FeePackageRepository;
import com.swp.parking.repository.FeeSubscriptionRepository;
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
    private final FeePackageRepository feePackageRepository;
    private final FeeSubscriptionRepository feeSubscriptionRepository;
    private final SubscriptionService subscriptionService;

    private LocalDate parseDate(String s) {
        if (s == null) return null;
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    private VehicleRegistrationResponse toResponse(VehicleRegistration reg) {
        Long vehicleId = reg.getVehicle() != null ? reg.getVehicle().getId() : null;
        Long requestedPackageId = reg.getRequestedFeePackage() != null
                ? reg.getRequestedFeePackage().getId() : null;
        FeeSubscription subscription = findLatestSubscription(vehicleId);
        PaymentStatusInfo paymentStatus = resolvePaymentStatus(requestedPackageId, subscription);

        return VehicleRegistrationResponse.builder()
                .registrationId(reg.getId())
                .userId(reg.getUser().getId())
                .userFullName(reg.getUser().getFullName())
                .vehicleTypeId(reg.getVehicleType().getId())
                .vehicleTypeName(reg.getVehicleType().getTypeName())
                .licensePlate(reg.getLicensePlate())
                .contactPhone(reg.getContactPhone())
                .requestedFeePackageId(requestedPackageId)
                .requestedFeePackageName(reg.getRequestedFeePackage() != null
                        ? reg.getRequestedFeePackage().getName() : null)
                .vehicleId(vehicleId)
                .subscriptionId(subscription != null ? subscription.getId() : null)
                .subscriptionStatus(subscription != null && subscription.getStatus() != null
                        ? subscription.getStatus().name() : null)
                .paymentStatus(paymentStatus.status())
                .paymentStatusLabel(paymentStatus.label())
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
        FeeSubscription subscription = findLatestSubscription(reg.getVehicleId());
        PaymentStatusInfo paymentStatus = resolvePaymentStatus(reg.getRequestedFeePackageId(), subscription);

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
                .vehicleId(reg.getVehicleId())
                .subscriptionId(subscription != null ? subscription.getId() : null)
                .subscriptionStatus(subscription != null && subscription.getStatus() != null
                        ? subscription.getStatus().name() : null)
                .paymentStatus(paymentStatus.status())
                .paymentStatusLabel(paymentStatus.label())
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

    private FeeSubscription findLatestSubscription(Long vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        return feeSubscriptionRepository.findFirstByVehicle_IdOrderByCreatedAtDesc(vehicleId)
                .orElse(null);
    }

    private PaymentStatusInfo resolvePaymentStatus(Long requestedPackageId, FeeSubscription subscription) {
        if (subscription != null && SubscriptionStatus.ACTIVE.equals(subscription.getStatus())) {
            return new PaymentStatusInfo("PAID", "Đã thanh toán");
        }
        if (requestedPackageId != null
                || (subscription != null && SubscriptionStatus.PENDING_PAYMENT.equals(subscription.getStatus()))) {
            return new PaymentStatusInfo("UNPAID", "Chưa thanh toán");
        }
        return new PaymentStatusInfo("PACKAGE_NOT_REGISTERED", "Chưa đăng ký gói");
    }

    private record PaymentStatusInfo(String status, String label) {
    }

    @Transactional
    public VehicleRegistrationResponse createRegistration(Long userId, VehicleRegistrationRequest request) {
        log.info("Creating vehicle registration for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User không tồn tại"));
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Loại xe không tồn tại"));

        FeePackage requestedFeePackage = null;
        if (request.getRequestedFeePackageId() != null) {
            requestedFeePackage = feePackageRepository.findById(request.getRequestedFeePackageId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Goi phi khong ton tai"));
            if (!requestedFeePackage.getVehicleType().getId().equals(vehicleType.getId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Goi phi khong phu hop voi loai xe");
            }
            if (!Boolean.TRUE.equals(requestedFeePackage.getIsActive())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Goi phi hien khong kha dung");
            }
        }

        String submittedPlate = normalizeLicensePlate(request.getLicensePlate());
        if (submittedPlate.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Vui long nhap bien so xe");
        }

        if (registrationRepository.existsByLicensePlateAndIsDeletedFalse(submittedPlate)) {
            throw new DuplicateLicensePlateException("Biển số " + submittedPlate + " đã tồn tại trong hệ thống");
        }

        EkycCccdResult cccd = readCccdBestEffort(request.getCccdFrontImage());
        EkycLicenseResult license = readLicenseBestEffort(request.getLicenseImage());
        String vehicleDocumentText = readVehicleDocumentBestEffort(request.getVehicleDocumentImage());
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
            validation = validateDocumentIfPresent("CCCD mặt trước", request.getCccdFrontImage());
            validateDocumentIfPresent("CCCD mặt sau", request.getCccdBackImage());
            validateDocumentIfPresent("bằng lái xe", request.getLicenseImage());
            validateDocumentIfPresent("giấy đăng ký xe", request.getVehicleDocumentImage());
            confidenceScore = validation != null ? validation.getConfidenceScore() : null;

            if (cccd != null && hasText(cccd.getFullName())
                    && license != null && hasText(license.getFullName())
                    && !samePersonName(cccd.getFullName(), license.getFullName())) {
                throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Họ tên trên CCCD và bằng lái xe không khớp");
            }
            if (hasText(vehicleDocumentText) && !containsPlate(vehicleDocumentText, submittedPlate)) {
                log.warn("Vehicle document OCR did not confirm submitted plate {} for userId {}. "
                                + "Keeping registration pending for staff manual review.",
                        submittedPlate, userId);
            }
        } else {
            log.warn("eKYC validation đang TẮT (ekyc.validation-enabled=false) - bỏ qua đối chiếu "
                    + "chất lượng/độ tin cậy; biển số do người dùng nhập vẫn được kiểm tra trùng. "
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
                .licensePlate(submittedPlate)
                .brand(detectedBrand)
                .color(detectedColor)
                .cccdFrontImage(request.getCccdFrontImage())
                .cccdBackImage(request.getCccdBackImage())
                .licenseImage(request.getLicenseImage())
                .vehicleDocumentImage(request.getVehicleDocumentImage())
                .plateImage(request.getPlateImage())
                .requestedFeePackage(requestedFeePackage)
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
                .registrationSource(hasAnyDocument(request) ? "VERIFIED_DOCUMENTS" : "FORM")
                .status("PENDING")
                .build();

        return toResponse(registrationRepository.save(registration));
    }

    @Transactional
    public VehicleRegistrationResponse createRegistrationForUser(
            Long targetUserId,
            Long operatorUserId,
            VehicleRegistrationRequest request) {
        log.info("Back-office user {} creating vehicle registration for userId: {}", operatorUserId, targetUserId);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User khong ton tai"));
        if (targetUser.getRole() != null && !"USER".equals(targetUser.getRole().name())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Chi co the dang ky xe cho tai khoan USER");
        }
        User operator = userRepository.findById(operatorUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Nguoi thao tac khong ton tai"));

        String submittedPlate = normalizeLicensePlate(request.getLicensePlate());
        VehicleRegistration registration = registrationRepository
                .findFirstByUser_IdAndLicensePlateAndIsDeletedFalseOrderByCreatedAtDesc(targetUserId, submittedPlate)
                .map(existing -> {
                    if (!existing.getVehicleType().getId().equals(request.getVehicleTypeId())) {
                        throw new AppException(HttpStatus.CONFLICT,
                                "Bien so nay da co ho so voi mot loai xe khac");
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    VehicleRegistrationResponse created = createRegistration(targetUserId, request);
                    return registrationRepository.findByIdAndNotDeleted(created.getRegistrationId())
                            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Khong tim thay dang ky vua tao"));
                });

        approveRegistration(registration, operator);
        return toResponse(registrationRepository.save(registration));
    }

    private EkycValidationResult validateDocumentIfPresent(String label, String image) {
        if (!hasText(image)) {
            return null;
        }
        return requireValidDocument(label, image);
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

    private EkycCccdResult readCccdBestEffort(String image) {
        if (!hasText(image)) {
            return null;
        }
        try {
            return ekycService.ocrCccd(image);
        } catch (AppException ex) {
            if (ekycProperties.isValidationEnabled()) {
                throw ex;
            }
            log.warn("Skipping CCCD OCR because eKYC validation is disabled: {}", ex.getMessage());
            return null;
        }
    }

    private EkycLicenseResult readLicenseBestEffort(String image) {
        if (!hasText(image)) {
            return null;
        }
        try {
            return ekycService.ocrLicense(image);
        } catch (AppException ex) {
            if (ekycProperties.isValidationEnabled()) {
                throw ex;
            }
            log.warn("Skipping driving license OCR because eKYC validation is disabled: {}", ex.getMessage());
            return null;
        }
    }

    private String readVehicleDocumentBestEffort(String image) {
        if (!hasText(image)) {
            return "";
        }
        try {
            return ekycService.ocrVehicleDocument(image);
        } catch (AppException ex) {
            if (ekycProperties.isValidationEnabled()) {
                throw ex;
            }
            log.warn("Skipping vehicle document OCR because eKYC validation is disabled: {}", ex.getMessage());
            return "";
        }
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

    private boolean isGoogleVisionProvider() {
        return "google-vision".equalsIgnoreCase(ekycProperties.getProvider());
    }

    private String normalizeLicensePlate(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasAnyDocument(VehicleRegistrationRequest request) {
        return hasText(request.getCccdFrontImage())
                || hasText(request.getCccdBackImage())
                || hasText(request.getLicenseImage())
                || hasText(request.getVehicleDocumentImage())
                || hasText(request.getPlateImage());
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
            approveRegistration(reg, admin);
        }

        reg.setStatus(dto.getStatus());
        reg.setRejectReason(dto.getRejectReason());
        reg.setReviewedBy(admin);
        reg.setReviewedAt(LocalDateTime.now());

        return toResponse(registrationRepository.save(reg));
    }

    private void approveRegistration(VehicleRegistration reg, User reviewer) {
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
                    existingVehicle.setBrand(firstNonBlank(reg.getBrand(), existingVehicle.getBrand()));
                    existingVehicle.setColor(firstNonBlank(reg.getColor(), existingVehicle.getColor()));
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
        reg.setStatus("APPROVED");
        reg.setRejectReason(null);
        reg.setReviewedBy(reviewer);
        reg.setReviewedAt(LocalDateTime.now());

        if (reg.getRequestedFeePackage() != null) {
            SubscriptionRegisterRequest subscriptionRequest = new SubscriptionRegisterRequest();
            subscriptionRequest.setVehicleId(vehicle.getId());
            subscriptionRequest.setPlanId(reg.getRequestedFeePackage().getId());
            subscriptionRequest.setAutoRenew(false);
            subscriptionService.registerSubscription(reg.getUser().getId(), subscriptionRequest, "127.0.0.1");
        }
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
