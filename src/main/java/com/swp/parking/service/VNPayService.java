package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import com.swp.parking.model.VNPayOrder;
import com.swp.parking.model.enums.VNPayOrderStatus;
import com.swp.parking.repository.VNPayOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service tích hợp VNPay Payment Gateway.
 *
 * Flow thanh toán:
 *   1. createPaymentUrl()  – tạo VNPayOrder + sinh link chuyển hướng sang VNPay
 *   2. processIpn()        – xác thực chữ ký + cập nhật trạng thái (IPN server→server)
 *   3. processReturn()     – xác thực chữ ký + trả về status cho FE (user redirect)
 *
 * Chữ ký: HMAC-SHA512 trên chuỗi sorted params (key1=URLencode(val1)&key2=...)
 * Tài liệu: https://sandbox.vnpayment.vn/apis/docs/huong-dan-tich-hop
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayOrderRepository vnPayOrderRepository;

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.pay-url}")
    private String vnpPayUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Value("${vnpay.order-expire-minutes:15}")
    private int expireMinutes;

    private static final DateTimeFormatter VNP_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VNP_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // ─────────────────────────────────────────────────────────────────
    // 1. Tạo link thanh toán VNPay
    // ─────────────────────────────────────────────────────────────────

    /**
     * Sinh VNPay payment URL và lưu đơn hàng vào DB.
     *
     * @param userId         ID người dùng
     * @param subscriptionId ID subscription liên kết
     * @param invoiceId      ID hóa đơn liên kết
     * @param amount         Số tiền VNĐ (ví dụ: 200000)
     * @param orderInfo      Mô tả đơn hàng (hiện trên trang VNPay)
     * @param clientIp       IP của người dùng (bắt buộc theo VNPay)
     * @return VNPayOrder đã được lưu (chứa paymentUrl để FE redirect)
     */
    @Transactional
    public VNPayOrder createPaymentUrl(Long userId, Long subscriptionId, Long invoiceId,
                                       Long amount, String orderInfo, String clientIp) {

        String txnRef = "VNP" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now(VNP_TIME_ZONE);

        // Tham số bắt buộc gửi lên VNPay
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_Amount", String.valueOf(amount * 100));  // VNPay tính đơn vị nhỏ nhất (× 100)
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnpReturnUrl);
        params.put("vnp_IpAddr", clientIp);
        LocalDateTime expireAt = now.plusMinutes(expireMinutes);
        params.put("vnp_CreateDate", now.format(VNP_DATE_FMT));
        params.put("vnp_ExpireDate", expireAt.format(VNP_DATE_FMT));

        // Xây dựng chuỗi hash theo đặc tả VNPay (sorted keys, URL-encode values)
        String hashData = buildHashData(params);
        String secureHash = computeHmacSha512(hashData);

        // Xây dựng query string cho payment URL
        String queryString = buildQueryString(params) + "&vnp_SecureHash=" + secureHash;
        String paymentUrl = vnpPayUrl + "?" + queryString;

        VNPayOrder order = VNPayOrder.builder()
                .txnRef(txnRef)
                .userId(userId)
                .subscriptionId(subscriptionId)
                .invoiceId(invoiceId)
                .amount(amount)
                .orderInfo(orderInfo)
                .paymentUrl(paymentUrl)
                .status(VNPayOrderStatus.PENDING)
                .expiredAt(expireAt)
                .build();

        VNPayOrder saved = vnPayOrderRepository.save(order);
        log.info("[VNPay] Tạo đơn – txnRef={}, subscriptionId={}, amount={}đ", txnRef, subscriptionId, amount);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. Xử lý IPN (Instant Payment Notification) — server-to-server
    // ─────────────────────────────────────────────────────────────────

    /**
     * Xác thực chữ ký và cập nhật trạng thái đơn hàng từ IPN VNPay.
     * Phải trả về {@code {"RspCode":"00","Message":"Confirm Success"}} khi OK.
     *
     * @param params Toàn bộ query params VNPay gửi đến (dạng Map)
     * @return true nếu xác thực chữ ký hợp lệ và cập nhật DB thành công
     */
    @Transactional
    public boolean processIpn(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");

        // Xác thực chữ ký HMAC-SHA512
        Map<String, String> filteredParams = new TreeMap<>(params);
        filteredParams.remove("vnp_SecureHash");
        filteredParams.remove("vnp_SecureHashType");
        String hashData = buildHashData(filteredParams);
        String expectedHash = computeHmacSha512(hashData);

        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            log.warn("[VNPay IPN] Chữ ký không hợp lệ – txnRef={}", txnRef);
            return false;
        }

        VNPayOrder order = vnPayOrderRepository.findById(txnRef).orElse(null);
        if (order == null) {
            log.warn("[VNPay IPN] Không tìm thấy đơn – txnRef={}", txnRef);
            return false;
        }

        // Chỉ xử lý đơn đang PENDING (tránh xử lý lại)
        if (!VNPayOrderStatus.PENDING.equals(order.getStatus())) {
            log.info("[VNPay IPN] Đơn {} đã được xử lý trước đó ({})", txnRef, order.getStatus());
            return true;
        }

        order.setVnpTransactionNo(transactionNo);
        order.setVnpResponseCode(responseCode);
        order.setVnpBankCode(bankCode);

        if ("00".equals(responseCode)) {
            order.setStatus(VNPayOrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now(VNP_TIME_ZONE));
            log.info("[VNPay IPN] THÀNH CÔNG – txnRef={}, transactionNo={}", txnRef, transactionNo);
        } else {
            order.setStatus(VNPayOrderStatus.FAILED);
            log.warn("[VNPay IPN] THẤT BẠI – txnRef={}, responseCode={}", txnRef, responseCode);
        }

        vnPayOrderRepository.save(order);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. Xử lý Return URL — browser redirect sau khi user thanh toán
    // ─────────────────────────────────────────────────────────────────

    /**
     * Xác thực chữ ký từ Return URL params.
     * Không cập nhật DB ở đây (đã có IPN làm việc đó); chỉ xác nhận để FE hiển thị.
     *
     * @param params Query params từ VNPay redirect
     * @return true nếu chữ ký hợp lệ VÀ responseCode = "00"
     */
    public boolean verifyReturn(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        String responseCode = params.get("vnp_ResponseCode");

        Map<String, String> filteredParams = new TreeMap<>(params);
        filteredParams.remove("vnp_SecureHash");
        filteredParams.remove("vnp_SecureHashType");

        String hashData = buildHashData(filteredParams);
        String expectedHash = computeHmacSha512(hashData);

        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            log.warn("[VNPay Return] Chữ ký không hợp lệ – params={}", params);
            return false;
        }

        return "00".equals(responseCode);
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. Lấy đơn hàng (FE polling hoặc truy vấn)
    // ─────────────────────────────────────────────────────────────────

    public VNPayOrder getOrder(String txnRef) {
        return vnPayOrderRepository.findById(txnRef)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn VNPay: " + txnRef));
    }

    public boolean isExpired(VNPayOrder order) {
        return order.getExpiredAt() != null
                && LocalDateTime.now(VNP_TIME_ZONE).isAfter(order.getExpiredAt());
    }

    @Transactional
    public VNPayOrder cancelOrder(String txnRef) {
        VNPayOrder order = getOrder(txnRef);
        if (VNPayOrderStatus.PENDING.equals(order.getStatus())) {
            order.setStatus(VNPayOrderStatus.CANCELLED);
            log.info("[VNPay] Đơn {} → CANCELLED", txnRef);
            return vnPayOrderRepository.save(order);
        }
        return order;
    }

    // ─────────────────────────────────────────────────────────────────
    // 5. Tạo params mock cho test (chỉ dùng trong dev/test)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Sinh bộ params IPN hợp lệ (có chữ ký HMAC-SHA512 thật) để test mà không cần VNPay sandbox.
     * Mock controller dùng bộ params này rồi gọi {@link #processIpn} → đi qua đúng code path thật.
     *
     * @param order    Đơn hàng cần giả lập
     * @param success  true = thanh toán thành công (responseCode "00"), false = thất bại ("11")
     * @param bankCode Tên ngân hàng giả (ví dụ "NCB", "VCB") — có thể để null
     */
    public Map<String, String> buildMockIpnParams(VNPayOrder order, boolean success, String bankCode) {
        String responseCode = success ? "00" : "11";
        String transactionNo = "MOCK" + System.currentTimeMillis();

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_Amount", String.valueOf(order.getAmount() * 100));
        params.put("vnp_BankCode", bankCode != null ? bankCode : "NCB");
        params.put("vnp_BankTranNo", "VNP" + System.currentTimeMillis());
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", order.getOrderInfo() != null ? order.getOrderInfo() : "Test payment");
        params.put("vnp_PayDate", LocalDateTime.now(VNP_TIME_ZONE).format(VNP_DATE_FMT));
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TmnCode", vnpTmnCode);
        params.put("vnp_TransactionNo", transactionNo);
        params.put("vnp_TransactionStatus", responseCode);
        params.put("vnp_TxnRef", order.getTxnRef());

        // Ký HMAC-SHA512 giống VNPay thật → processIpn() sẽ verify đúng
        String hashData = buildHashData(params);
        String secureHash = computeHmacSha512(hashData);
        params.put("vnp_SecureHash", secureHash);

        return params;
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers: xây dựng chuỗi hash + query string
    // ─────────────────────────────────────────────────────────────────

    /**
     * Xây dựng chuỗi dữ liệu để ký HMAC-SHA512.
     * Đặc tả VNPay: key=URLencode(value) nối bằng '&', sắp xếp theo key tăng dần.
     */
    private String buildHashData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                if (sb.length() > 0) sb.append('&');
                sb.append(entry.getKey())
                  .append('=')
                  .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    /**
     * Xây dựng query string cho payment URL (URL-encode cả key lẫn value).
     */
    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                if (sb.length() > 0) sb.append('&');
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                  .append('=')
                  .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    /**
     * Tính HMAC-SHA512 với vnpHashSecret.
     */
    private String computeHmacSha512(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(
                    vnpHashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            log.error("[VNPay] Lỗi tính HMAC-SHA512: {}", e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi xử lý chữ ký thanh toán");
        }
    }
}
