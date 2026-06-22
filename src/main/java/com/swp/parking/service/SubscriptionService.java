package com.swp.parking.service;

import com.swp.parking.dto.request.MomoIpnRequest;
import com.swp.parking.dto.request.SubscriptionRegisterRequest;
import com.swp.parking.dto.response.MyVehicleResponse;
import com.swp.parking.dto.response.RegisterSubscriptionResponse;
import com.swp.parking.dto.response.SubscriptionInvoiceResponse;
import com.swp.parking.dto.response.SubscriptionResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.exception.ResourceNotFoundException;
import com.swp.parking.model.FeePackage;
import com.swp.parking.model.FeePackagePriceHistory;
import com.swp.parking.model.FeeSubscription;
import com.swp.parking.model.FeeSubscriptionInvoice;
import com.swp.parking.model.MomoOrder;
import com.swp.parking.model.Vehicle;
import com.swp.parking.model.enums.InvoiceStatus;
import com.swp.parking.model.enums.SubscriptionStatus;
import com.swp.parking.repository.FeePackagePriceHistoryRepository;
import com.swp.parking.repository.FeePackageRepository;
import com.swp.parking.repository.FeeSubscriptionInvoiceRepository;
import com.swp.parking.repository.FeeSubscriptionRepository;
import com.swp.parking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service duy nhất quản lý toàn bộ vòng đời thẻ tháng:
 *   1. Danh sách xe của user (để chọn khi đăng ký)
 *   2. Đăng ký thẻ tháng → tạo link thanh toán MoMo
 *   3. Xử lý IPN callback từ MoMo → kích hoạt / từ chối subscription
 *   4. Hủy subscription theo ID
 *   5. Hủy tự động gia hạn (cancel auto-renew)
 *   6. Xem lịch sử thẻ tháng của user
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final FeeSubscriptionRepository subscriptionRepository;
    private final FeeSubscriptionInvoiceRepository invoiceRepository;
    private final FeePackageRepository packageRepository;
    private final FeePackagePriceHistoryRepository priceHistoryRepository;
    private final VehicleRepository vehicleRepository;
    private final CryptoService cryptoService;
    private final MomoQrService momoQrService;

    // ─────────────────────────────────────────────────────────────────
    // 1. Danh sách xe của user
    // ─────────────────────────────────────────────────────────────────

    /**
     * Trả danh sách phương tiện của user.
     * Lọc theo {@code vehicleTypeId} để chỉ hiển thị xe phù hợp với gói đang xem.
     */
    @Transactional(readOnly = true)
    public List<MyVehicleResponse> getMyVehicles(Long userId, Long vehicleTypeId) {
        List<Vehicle> vehicles = (vehicleTypeId != null)
                ? vehicleRepository.findByUserIdAndVehicleTypeId(userId, vehicleTypeId)
                : vehicleRepository.findByUserId(userId);

        return vehicles.stream()
                .map(v -> MyVehicleResponse.builder()
                        .vehicleId(v.getId())
                        .licensePlate(v.getLicensePlate())
                        .brand(v.getBrand())
                        .color(v.getColor())
                        .vehicleTypeId(v.getVehicleType().getId())
                        .vehicleTypeName(v.getVehicleType().getTypeName())
                        .vehicleTypeCode(v.getVehicleType().getTypeCode())
                        .createdAt(v.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. Đăng ký thẻ tháng
    // ─────────────────────────────────────────────────────────────────

    /**
     * Tạo subscription {@code PENDING_PAYMENT} rồi gọi MoMo lấy link thanh toán.
     * <ol>
     *   <li>Xác minh xe thuộc user.</li>
     *   <li>Kiểm tra xe chưa có subscription ACTIVE.</li>
     *   <li>Lấy gói + giá hiện hành.</li>
     *   <li>Lưu subscription + invoice kỳ đầu.</li>
     *   <li>Gọi MoMo {@code payAndSendToken} → trả payUrl/deeplink/qrCodeUrl.</li>
     * </ol>
     */
    @Transactional
    public RegisterSubscriptionResponse registerSubscription(Long userId,
                                                             SubscriptionRegisterRequest request) {
        // Xác minh quyền sở hữu xe
        if (!vehicleRepository.existsByIdAndCustomer_User_Id(request.getVehicleId(), userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Phương tiện không thuộc về tài khoản của bạn");
        }
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phương tiện"));

        // Tự động hủy TẤT CẢ giao dịch PENDING_PAYMENT cũ của xe này trước khi tạo mới
        List<FeeSubscription> pendingList = subscriptionRepository
                .findAllByVehicle_IdAndStatus(request.getVehicleId(), SubscriptionStatus.PENDING_PAYMENT);
        pendingList.forEach(pending -> {
            pending.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(pending);
            log.info("Auto-cancelled pending subscription {} trước khi tạo mới", pending.getId());
        });

        // Chặn đăng ký trùng khi xe đã có thẻ tháng đang ACTIVE
        subscriptionRepository.findByVehicle_IdAndStatus(request.getVehicleId(), SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new AppException(HttpStatus.CONFLICT,
                            "Phương tiện đã có thẻ tháng đang hoạt động, hết hạn: " + existing.getEndDate());
                });

        // Lấy gói user đã chọn
        FeePackage feePackage = packageRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Gói đăng ký không tồn tại"));
        if (!Boolean.TRUE.equals(feePackage.getIsActive())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Gói đăng ký này hiện không khả dụng");
        }

        // Lấy giá hiện hành (bản ghi chưa đóng: effectiveTo IS NULL)
        FeePackagePriceHistory priceHistory = priceHistoryRepository
                .findFirstByFeePackage_IdAndEffectiveToIsNullOrderByEffectiveFromDesc(feePackage.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy giá hiện hành cho gói: " + feePackage.getId()));

        // Tạo subscription ở trạng thái chờ thanh toán
        FeeSubscription subscription = FeeSubscription.builder()
                .vehicle(vehicle)
                .feePackage(feePackage)
                .priceHistory(priceHistory)
                .amountToPay(priceHistory.getPrice())
                .isAutoRenew(request.isAutoRenew())
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .build();
        subscription = subscriptionRepository.save(subscription);

        String orderInfo = feePackage.getName() + " – Thẻ tháng bãi đỗ xe";

        // Tạo hóa đơn kỳ đầu
        FeeSubscriptionInvoice invoice = FeeSubscriptionInvoice.builder()
                .feeSubscription(subscription)
                .amount(priceHistory.getPrice())
                .status(InvoiceStatus.PENDING)
                .type("INITIAL")
                .build();
        invoice = invoiceRepository.save(invoice);

        // Sinh QR code động cho tài khoản MoMo cá nhân
        MomoOrder momoOrder = momoQrService.createOrder(
                userId,
                subscription.getId(),
                invoice.getId(),
                priceHistory.getPrice(),
                orderInfo);

        // Lưu orderId vào invoice để trace sau này
        invoice.setMomoOrderId(momoOrder.getOrderId());
        invoiceRepository.save(invoice);

        log.info("Subscription {} (gói: {}) → PENDING_PAYMENT, momoOrderId={}",
                subscription.getId(), feePackage.getName(), momoOrder.getOrderId());

        return RegisterSubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .invoiceId(invoice.getId())
                .momoOrderId(momoOrder.getOrderId())
                .momoAccount(momoOrder.getMomoAccount())
                .momoName(momoOrder.getMomoName())
                .qrCodeData(momoOrder.getQrCodeData())
                .expiredAt(momoOrder.getExpiredAt())
                .message("Quét mã QR bằng app MoMo để chuyển tiền đến " + momoOrder.getMomoName()
                        + " (" + momoOrder.getMomoAccount() + ")")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. Xử lý IPN callback từ MoMo
    // ─────────────────────────────────────────────────────────────────

    /**
     * Xử lý IPN do MoMo gửi sau khi user hoàn tất thanh toán.
     * <ul>
     *   <li>resultCode == 0: ACTIVE, lưu partnerToken (nếu autoRenew), set startDate/endDate.</li>
     *   <li>resultCode != 0: EXPIRED, đánh dấu invoice FAILED.</li>
     * </ul>
     */
    @Transactional
    public void processIpnCallback(MomoIpnRequest ipn, Long subscriptionId, String callbackToken) {
        FeeSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy subscription: " + subscriptionId));

        FeeSubscriptionInvoice invoice = invoiceRepository
                .findByFeeSubscriptionIdOrderByCreatedAtDesc(subscriptionId)
                .stream()
                .filter(inv -> ipn.getOrderId().equals(inv.getMomoOrderId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hóa đơn với orderId: " + ipn.getOrderId()));

        if (ipn.getResultCode() == 0) {
            // Lưu partnerToken (mã hóa AES) để dùng cho các lần auto-renew tiếp theo
            if (Boolean.TRUE.equals(subscription.getIsAutoRenew())
                    && callbackToken != null && !callbackToken.isBlank()) {
                subscription.setMomoPartnerToken(cryptoService.encrypt(callbackToken));
                log.info("partnerToken lưu (AES) cho subscription {}", subscriptionId);
            }

            LocalDateTime now = LocalDateTime.now();
            int months = subscription.getFeePackage().getDurationMonths();
            subscription.setStartDate(now);
            subscription.setEndDate(now.plusMonths(months));
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(subscription);

            invoice.setStatus(InvoiceStatus.SUCCESS);
            invoice.setMomoTransId(ipn.getTransId());
            invoice.setMessage(ipn.getMessage());
            invoiceRepository.save(invoice);

            log.info("Subscription {} → ACTIVE ({} tháng), hết hạn: {}",
                    subscriptionId, months, subscription.getEndDate());
        } else {
            // Thanh toán kỳ đầu thất bại → subscription chưa bao giờ ACTIVE → CANCELLED
            // (EXPIRED chỉ dùng khi subscription đã active nhưng auto-renew thất bại)
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(subscription);

            invoice.setStatus(InvoiceStatus.FAILED);
            invoice.setMessage(ipn.getMessage());
            invoiceRepository.save(invoice);

            log.warn("Subscription {} → CANCELLED do thanh toán thất bại (resultCode={}): {}",
                    subscriptionId, ipn.getResultCode(), ipn.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. Kích hoạt subscription sau khi admin xác nhận thanh toán MoMo
    // ─────────────────────────────────────────────────────────────────

    /**
     * Được gọi từ MomoOrderController khi admin xác nhận đã nhận tiền.
     * Chuyển subscription PENDING_PAYMENT → ACTIVE và invoice PENDING → SUCCESS.
     */
    @Transactional
    public void activateSubscription(Long subscriptionId, String transactionId) {
        FeeSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy subscription: " + subscriptionId));

        if (!SubscriptionStatus.PENDING_PAYMENT.equals(subscription.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Subscription không ở trạng thái chờ thanh toán (hiện tại: " + subscription.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        int months = subscription.getFeePackage().getDurationMonths();
        subscription.setStartDate(now);
        subscription.setEndDate(now.plusMonths(months));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // Cập nhật hóa đơn PENDING đầu tiên → SUCCESS
        invoiceRepository.findByFeeSubscriptionIdOrderByCreatedAtDesc(subscriptionId)
                .stream()
                .filter(inv -> InvoiceStatus.PENDING.equals(inv.getStatus()))
                .findFirst()
                .ifPresent(inv -> {
                    inv.setStatus(InvoiceStatus.SUCCESS);
                    if (transactionId != null && !transactionId.isBlank()) {
                        try {
                            inv.setMomoTransId(Long.parseLong(transactionId));
                        } catch (NumberFormatException ignored) {
                            // transactionId không phải số, bỏ qua
                        }
                    }
                    invoiceRepository.save(inv);
                });

        log.info("Subscription {} → ACTIVE ({} tháng), hết hạn: {}",
                subscriptionId, months, subscription.getEndDate());
    }

    // ─────────────────────────────────────────────────────────────────
    // 5. Hủy subscription theo ID
    // ─────────────────────────────────────────────────────────────────

    /**
     * Hủy subscription đang ACTIVE theo ID.
     * Đồng thời xóa partnerToken để ngăn auto-renew tiếp theo.
     * Chỉ chủ sở hữu xe mới được hủy.
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId, Long subscriptionId) {
        FeeSubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy subscription: " + subscriptionId));

        if (!vehicleRepository.existsByIdAndCustomer_User_Id(subscription.getVehicle().getId(), userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy gói này");
        }

        if (!SubscriptionStatus.ACTIVE.equals(subscription.getStatus())
                && !SubscriptionStatus.PENDING_PAYMENT.equals(subscription.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "Chỉ có thể hủy gói đang hoạt động hoặc chờ thanh toán");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setIsAutoRenew(false);
        subscription.setMomoPartnerToken(null);
        subscriptionRepository.save(subscription);

        log.info("User {} hủy subscription {}", userId, subscriptionId);
        return mapToResponse(subscription);
    }

    // ─────────────────────────────────────────────────────────────────
    // 4b. Hủy subscription PENDING_PAYMENT (dùng khi MoMo return thất bại)
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public void cancelPendingSubscription(Long subscriptionId) {
        subscriptionRepository.findById(subscriptionId).ifPresent(sub -> {
            if (SubscriptionStatus.PENDING_PAYMENT.equals(sub.getStatus())) {
                sub.setStatus(SubscriptionStatus.CANCELLED);
                subscriptionRepository.save(sub);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // 5. Hủy tự động gia hạn
    // ─────────────────────────────────────────────────────────────────

    /**
     * Tắt tự động gia hạn cho gói ACTIVE đang bật auto-renew.
     * Subscription vẫn còn hiệu lực đến {@code endDate}, chỉ không gia hạn thêm.
     */
    @Transactional
    public SubscriptionResponse cancelAutoRenew(Long userId) {
        FeeSubscription active = subscriptionRepository.findAllByUserId(userId).stream()
                .filter(s -> SubscriptionStatus.ACTIVE.equals(s.getStatus())
                          && Boolean.TRUE.equals(s.getIsAutoRenew()))
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy gói đang hoạt động với tính năng tự động gia hạn"));

        active.setIsAutoRenew(false);
        active.setMomoPartnerToken(null);
        subscriptionRepository.save(active);

        log.info("User {} tắt auto-renew cho subscription {}", userId, active.getId());
        return mapToResponse(active);
    }

    // ─────────────────────────────────────────────────────────────────
    // 6. Lịch sử thẻ tháng của user
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findAllByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // 7. Lịch sử hoá đơn subscription của user
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SubscriptionInvoiceResponse> getUserInvoices(Long userId) {
        return invoiceRepository.findAllByUserId(userId).stream()
                .map(inv -> SubscriptionInvoiceResponse.builder()
                        .id(inv.getId())
                        .momoOrderId(inv.getMomoOrderId())
                        .momoTransId(inv.getMomoTransId())
                        .amount(inv.getAmount())
                        .status(inv.getStatus().name())
                        .type(inv.getType())
                        .message(inv.getMessage())
                        .licensePlate(inv.getFeeSubscription().getVehicle().getLicensePlate())
                        .planName(inv.getFeeSubscription().getFeePackage().getName())
                        .createdAt(inv.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // Mapper
    // ─────────────────────────────────────────────────────────────────

    private SubscriptionResponse mapToResponse(FeeSubscription s) {
        FeePackage pkg = s.getFeePackage();
        return SubscriptionResponse.builder()
                .id(s.getId())
                .vehicleId(s.getVehicle() != null ? s.getVehicle().getId() : null)
                .licensePlate(s.getVehicle() != null ? s.getVehicle().getLicensePlate() : null)
                .planId(pkg != null ? pkg.getId() : null)
                .planName(pkg != null ? pkg.getName() : null)
                .durationMonths(pkg != null ? pkg.getDurationMonths() : null)
                .amountToPay(s.getAmountToPay())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .isAutoRenew(s.getIsAutoRenew())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
