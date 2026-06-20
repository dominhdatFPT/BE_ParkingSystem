package com.swp.parking.repository;

import com.swp.parking.model.FeeSubscriptionInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeeSubscriptionInvoiceRepository extends JpaRepository<FeeSubscriptionInvoice, Long> {

    List<FeeSubscriptionInvoice> findByFeeSubscriptionIdOrderByCreatedAtDesc(Long feeSubscriptionId);

    @Query("""
            SELECT fsi FROM FeeSubscriptionInvoice fsi
            JOIN FETCH fsi.feeSubscription fs
            JOIN FETCH fs.vehicle v
            JOIN FETCH fs.feePackage pkg
            WHERE v.customer.user.id = :userId
            ORDER BY fsi.createdAt DESC
            """)
    List<FeeSubscriptionInvoice> findAllByUserId(@Param("userId") Long userId);
}
