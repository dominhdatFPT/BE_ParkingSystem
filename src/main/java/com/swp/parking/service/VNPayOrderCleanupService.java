package com.swp.parking.service;

import com.swp.parking.model.FeeSubscription;
import com.swp.parking.model.FeeSubscriptionInvoice;
import com.swp.parking.model.VNPayOrder;
import com.swp.parking.model.enums.InvoiceStatus;
import com.swp.parking.model.enums.SubscriptionStatus;
import com.swp.parking.model.enums.VNPayOrderStatus;
import com.swp.parking.repository.FeeSubscriptionInvoiceRepository;
import com.swp.parking.repository.FeeSubscriptionRepository;
import com.swp.parking.repository.VNPayOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayOrderCleanupService {

    private static final ZoneId VNP_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VNPayOrderRepository vnPayOrderRepository;
    private final FeeSubscriptionRepository subscriptionRepository;
    private final FeeSubscriptionInvoiceRepository invoiceRepository;

    @Scheduled(
            initialDelayString = "${vnpay.cleanup-initial-delay-ms:60000}",
            fixedDelayString = "${vnpay.cleanup-fixed-delay-ms:60000}"
    )
    @Transactional
    public void deleteExpiredPendingOrders() {
        LocalDateTime now = LocalDateTime.now(VNP_TIME_ZONE);
        List<VNPayOrder> expiredOrders = vnPayOrderRepository
                .findByStatusAndExpiredAtBefore(VNPayOrderStatus.PENDING, now);

        if (expiredOrders.isEmpty()) {
            return;
        }

        for (VNPayOrder order : expiredOrders) {
            cancelPendingSubscription(order);
            failPendingInvoice(order);
        }

        vnPayOrderRepository.deleteAll(expiredOrders);
        log.info("[VNPay Cleanup] Deleted {} expired pending orders", expiredOrders.size());
    }

    private void cancelPendingSubscription(VNPayOrder order) {
        Long subscriptionId = order.getSubscriptionId();
        if (subscriptionId == null) {
            return;
        }

        subscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {
            if (SubscriptionStatus.PENDING_PAYMENT.equals(subscription.getStatus())) {
                subscription.setStatus(SubscriptionStatus.CANCELLED);
                subscriptionRepository.save(subscription);
            }
        });
    }

    private void failPendingInvoice(VNPayOrder order) {
        Long invoiceId = order.getInvoiceId();
        if (invoiceId == null) {
            return;
        }

        invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
            if (InvoiceStatus.PENDING.equals(invoice.getStatus())) {
                invoice.setStatus(InvoiceStatus.FAILED);
                invoice.setMessage("VNPay payment expired");
                invoiceRepository.save(invoice);
            }
        });
    }
}
