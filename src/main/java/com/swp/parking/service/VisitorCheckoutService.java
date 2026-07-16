package com.swp.parking.service;

import com.swp.parking.dto.response.ParkingExitResponse;
import com.swp.parking.dto.response.VisitorCheckoutResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.StripeOrder;
import com.swp.parking.model.enums.StripeOrderStatus;
import com.swp.parking.repository.StripeOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VisitorCheckoutService {

    private final ParkingExitService parkingExitService;
    private final StripeService stripeService;
    private final StripeOrderRepository stripeOrderRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public VisitorCheckoutResponse lookup(String orderCode) {
        return toCheckoutResponse(parkingExitService.checkVisitorOrderCode(orderCode), null);
    }

    @Transactional
    public VisitorCheckoutResponse createPaymentIntent(String orderCode) {
        ParkingExitResponse exit = parkingExitService.checkVisitorOrderCode(orderCode);
        if (exit.getPayment() != null && "PAID".equalsIgnoreCase(exit.getPayment().getStatus())) {
            return toCheckoutResponse(exit, null);
        }

        StripeOrder stripeOrder = stripeOrderRepository
                .findFirstByParkingOrderIdOrderByCreatedAtDesc(exit.getOrderId())
                .filter(order -> order.getStatus() == StripeOrderStatus.PENDING
                        && order.getExpiredAt() != null
                        && order.getExpiredAt().isAfter(LocalDateTime.now()))
                .orElseGet(() -> stripeService.createParkingPaymentIntent(
                        exit.getOrderId(),
                        toStripeAmount(exit.getFee().getAmount()),
                        "Parking exit " + exit.getOrderCode() + " - " + exit.getLicensePlate()));

        return toCheckoutResponse(exit, stripeOrder);
    }

    @Transactional
    public VisitorCheckoutResponse confirmPayment(String paymentIntentId) {
        StripeOrder order = stripeService.confirmIfSucceeded(paymentIntentId);
        if (order.getParkingOrderId() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "PaymentIntent nay khong thuoc phien gui xe vang lai");
        }
        if (order.getStatus() == StripeOrderStatus.SUCCEEDED) {
            markParkingOrderPaid(order);
        }
        ParkingExitResponse exit = parkingExitService.checkVisitorOrderCode(orderCodeById(order.getParkingOrderId()));
        order.setClientSecret(null);
        return toCheckoutResponse(exit, order);
    }

    @Transactional
    public void markParkingOrderPaid(StripeOrder order) {
        if (order.getParkingOrderId() == null) {
            return;
        }

        BigDecimal amount = BigDecimal.valueOf(order.getAmount()).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime paidAt = order.getPaidAt() == null ? LocalDateTime.now() : order.getPaidAt();

        jdbcTemplate.update("""
                UPDATE parking_orders
                   SET calculated_fee = COALESCE(calculated_fee, ?),
                       payment_status = 'PAID',
                       payment_method = 'STRIPE',
                       fee_breakdown = COALESCE(
                           fee_breakdown,
                           jsonb_build_object('total', ?, 'paidOnline', true, 'paymentIntentId', ?)
                       ),
                       updated_at = now()
                 WHERE order_id = ?
                   AND parking_status = 'ACTIVE'
                """, amount, amount, order.getPaymentIntentId(), order.getParkingOrderId());

        jdbcTemplate.update("""
                INSERT INTO parking_order_payments (
                    order_id, amount, payment_method, payment_status,
                    paid_at, transaction_reference, notes, created_at, updated_at
                )
                VALUES (?, ?, 'STRIPE', 'PAID', ?, ?, ?, now(), now())
                ON CONFLICT (order_id) DO UPDATE SET
                    amount = EXCLUDED.amount,
                    payment_method = EXCLUDED.payment_method,
                    payment_status = EXCLUDED.payment_status,
                    paid_at = EXCLUDED.paid_at,
                    transaction_reference = EXCLUDED.transaction_reference,
                    notes = EXCLUDED.notes,
                    updated_at = now()
                """,
                order.getParkingOrderId(),
                amount,
                paidAt,
                order.getPaymentIntentId(),
                "Stripe charge: " + order.getStripeChargeId());
    }

    private VisitorCheckoutResponse toCheckoutResponse(ParkingExitResponse exit, StripeOrder order) {
        ParkingExitResponse.PaymentInfo payment = exit.getPayment();
        return VisitorCheckoutResponse.builder()
                .orderId(exit.getOrderId())
                .orderCode(exit.getOrderCode())
                .licensePlate(exit.getLicensePlate())
                .vehicleType(exit.getVehicleType())
                .visitorCardCode(exit.getVisitorCardCode())
                .entryTime(exit.getEntryTime())
                .durationMinutes(exit.getDurationMinutes())
                .amount(exit.getFee().getAmount())
                .currency(exit.getFee().getCurrency())
                .paymentStatus(payment == null ? "UNPAID" : payment.getStatus())
                .paymentMethod(payment == null ? null : payment.getMethod())
                .paidAmount(payment == null ? null : payment.getPaidAmount())
                .paidAt(payment == null ? null : payment.getPaidAt())
                .paymentIntentId(order == null ? null : order.getPaymentIntentId())
                .clientSecret(order == null ? null : order.getClientSecret())
                .message(exit.getMessage())
                .build();
    }

    private Long toStripeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(HttpStatus.CONFLICT, "So tien thanh toan khong hop le");
        }
        return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String orderCodeById(Long orderId) {
        return jdbcTemplate.query("""
                SELECT order_code
                  FROM parking_orders
                 WHERE order_id = ?
                """, (rs, rowNum) -> rs.getString("order_code"), orderId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Khong tim thay phien gui xe"));
    }
}
