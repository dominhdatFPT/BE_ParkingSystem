package com.swp.parking.controller;

import com.swp.parking.dto.request.VisitorCheckoutRequest;
import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.VisitorCheckoutResponse;
import com.swp.parking.service.VisitorCheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visitor-checkout")
@RequiredArgsConstructor
public class VisitorCheckoutController {

    private final VisitorCheckoutService visitorCheckoutService;

    @PostMapping("/lookup")
    public ResponseEntity<ApiResponse<VisitorCheckoutResponse>> lookup(
            @RequestBody VisitorCheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                visitorCheckoutService.lookup(request.getOrderCode())));
    }

    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<VisitorCheckoutResponse>> createStripePayment(
            @RequestBody VisitorCheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                visitorCheckoutService.createPaymentIntent(request.getOrderCode())));
    }

    @PostMapping("/stripe/{paymentIntentId}/confirm")
    public ResponseEntity<ApiResponse<VisitorCheckoutResponse>> confirmStripePayment(
            @PathVariable String paymentIntentId) {
        return ResponseEntity.ok(ApiResponse.success(
                visitorCheckoutService.confirmPayment(paymentIntentId)));
    }
}
