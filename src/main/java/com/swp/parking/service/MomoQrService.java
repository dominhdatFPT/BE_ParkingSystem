package com.swp.parking.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.MomoOrder;
import com.swp.parking.model.enums.MomoOrderStatus;
import com.swp.parking.repository.MomoOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tạo QR code động cho chuyển khoản MoMo cá nhân.
 *
 * Flow:
 *   1. createOrder()  – tạo MomoOrder + render QR Base64 cho tài khoản 0982094533
 *   2. getOrder()     – lấy thông tin đơn hàng (polling từ FE)
 *   3. confirmPaid()  – admin xác nhận đã nhận tiền → PAID
 *   4. cancelOrder()  – hủy đơn PENDING (hết hạn / user hủy)
 *
 * Định dạng QR MoMo P2P:
 *   2|99|{phone}|{name}||0|0|{amount}|{orderId}
 * Khi quét bằng app MoMo, màn hình chuyển tiền sẽ tự điền sẵn số tài khoản + số tiền.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MomoQrService {

    private final MomoOrderRepository momoOrderRepository;

    @Value("${momo.personal.phone:0982094533}")
    private String momoPhone;

    @Value("${momo.personal.name:PHAM NGUYEN QUYEN}")
    private String momoName;

    @Value("${momo.personal.order-expire-minutes:10}")
    private int expireMinutes;

    // ─────────────────────────────────────────────────────────────────
    // 1. Tạo đơn hàng + sinh QR
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public MomoOrder createOrder(Long userId, Long subscriptionId, Long invoiceId,
                                  BigDecimal amount, String description) {
        String orderId = "ORDER_" + System.currentTimeMillis();

        // Định dạng QR MoMo P2P: 2|99|phone|name||0|0|amount|orderId
        String qrContent = String.format("2|99|%s|%s||0|0|%d|%s",
                momoPhone, momoName, amount.longValue(), orderId);

        String qrBase64 = generateQrBase64(qrContent);

        MomoOrder order = MomoOrder.builder()
                .orderId(orderId)
                .userId(userId)
                .subscriptionId(subscriptionId)
                .invoiceId(invoiceId)
                .amount(amount)
                .description(description)
                .momoAccount(momoPhone)
                .momoName(momoName)
                .qrCodeData(qrBase64)
                .paymentStatus(MomoOrderStatus.PENDING)
                .expiredAt(LocalDateTime.now().plusMinutes(expireMinutes))
                .build();

        MomoOrder saved = momoOrderRepository.save(order);
        log.info("MomoOrder created – orderId={}, subscriptionId={}, amount={}", orderId, subscriptionId, amount);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. Lấy đơn hàng (FE polling)
    // ─────────────────────────────────────────────────────────────────

    public MomoOrder getOrder(String orderId) {
        return momoOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng: " + orderId));
    }

    public List<MomoOrder> getPendingOrders() {
        return momoOrderRepository.findByPaymentStatusOrderByCreatedAtDesc(MomoOrderStatus.PENDING);
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. Admin xác nhận đã nhận tiền
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public MomoOrder confirmPaid(String orderId, String transactionId, String notes) {
        MomoOrder order = getOrder(orderId);
        if (!MomoOrderStatus.PENDING.equals(order.getPaymentStatus())) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Đơn hàng đang ở trạng thái " + order.getPaymentStatus() + ", không thể xác nhận");
        }
        order.setPaymentStatus(MomoOrderStatus.PAID);
        order.setTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        order.setNotes(notes);
        log.info("MomoOrder {} → PAID, transactionId={}", orderId, transactionId);
        return momoOrderRepository.save(order);
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. Hủy đơn hàng PENDING
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public MomoOrder cancelOrder(String orderId) {
        MomoOrder order = getOrder(orderId);
        if (MomoOrderStatus.PENDING.equals(order.getPaymentStatus())) {
            order.setPaymentStatus(MomoOrderStatus.CANCELLED);
            log.info("MomoOrder {} → CANCELLED", orderId);
            return momoOrderRepository.save(order);
        }
        return order;
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    public boolean isExpired(MomoOrder order) {
        return order.getExpiredAt() != null
                && LocalDateTime.now().isAfter(order.getExpiredAt());
    }

    /**
     * Tạo ảnh QR 300×300 từ nội dung text, trả về chuỗi Base64 PNG.
     * Frontend dùng trực tiếp: <img src="{qrCodeData}" />
     */
    private String generateQrBase64(String content) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 700, 700, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException e) {
            log.error("QR generation failed: {}", e.getMessage(), e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo mã QR thanh toán");
        }
    }
}
