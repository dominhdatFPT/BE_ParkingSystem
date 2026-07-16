package com.swp.parking.repository;

import com.swp.parking.model.StripeOrder;
import com.swp.parking.model.enums.StripeOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StripeOrderRepository extends JpaRepository<StripeOrder, String> {

    Optional<StripeOrder> findByPaymentIntentId(String paymentIntentId);

    Optional<StripeOrder> findFirstByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    Optional<StripeOrder> findFirstByParkingOrderIdOrderByCreatedAtDesc(Long parkingOrderId);

    /** Lấy tất cả đơn PENDING đã hết hạn để cleanup */
    List<StripeOrder> findAllByStatusAndExpiredAtBefore(StripeOrderStatus status, LocalDateTime now);
}
