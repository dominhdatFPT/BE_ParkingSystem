package com.swp.parking.admin.pricing.controller;

import com.swp.parking.admin.pricing.dto.request.UpdatePriceRequest;
import com.swp.parking.admin.pricing.dto.request.UpdateVisitorRateRequest;
import com.swp.parking.admin.pricing.dto.response.FeePackageResponse;
import com.swp.parking.admin.pricing.dto.response.VisitorFeeRateResponse;
import com.swp.parking.admin.pricing.service.PricingService;
import com.swp.parking.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/packages")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<FeePackageResponse>> getAllFeePackages() {
        return ApiResponse.success(pricingService.getAllFeePackages());
    }

    @PutMapping("/packages/{feePackageId}/price")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FeePackageResponse> updateFeePackagePrice(
            @PathVariable Long feePackageId,
            @Valid @RequestBody UpdatePriceRequest request) {
        return ApiResponse.success(
                pricingService.updateFeePackagePrice(feePackageId, request),
                "Cập nhật giá thành công");
    }

    @PatchMapping("/packages/{feePackageId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> toggleFeePackage(@PathVariable Long feePackageId) {
        pricingService.toggleFeePackage(feePackageId);
        return ApiResponse.success(null, "Cập nhật trạng thái gói thành công");
    }

    @GetMapping("/visitor-rates")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<VisitorFeeRateResponse>> getAllVisitorRates() {
        return ApiResponse.success(pricingService.getAllVisitorRates());
    }

    @PutMapping("/visitor-rates/{vehicleTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VisitorFeeRateResponse> updateVisitorRate(
            @PathVariable Long vehicleTypeId,
            @Valid @RequestBody UpdateVisitorRateRequest request) {
        return ApiResponse.success(
                pricingService.updateVisitorRate(vehicleTypeId, request),
                "Cập nhật giá vãng lai thành công");
    }
}
