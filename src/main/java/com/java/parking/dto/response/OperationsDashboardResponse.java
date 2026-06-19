package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationsDashboardResponse {

    private Metrics metrics;
    private List<AreaOccupancy> areaOccupancy;
    private List<TrafficPoint> trafficByHour;
    private List<RecentBooking> pendingBookings;
    private List<RecentIncident> recentIncidents;
    private List<VehicleActivity> recentVehicleActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        private long vehiclesInParking;
        private long availableSlots;
        private long pendingBookings;
        private long vehiclesInToday;
        private long vehiclesInTodayCars;
        private long vehiclesInTodayMotorbikes;
        private long vehiclesOutToday;
        private long vehiclesOutTodayCars;
        private long vehiclesOutTodayMotorbikes;
        private long activeCars;
        private long activeMotorbikes;
        private long openIncidents;
        private BigDecimal revenueToday;
        private long totalSlots;
        private int occupancyRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaOccupancy {
        private String name;
        private String facilityName;
        private Integer floorNumber;
        private String areaKey;
        private long total;
        private long occupied;
        private long available;
        private int fillRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrafficPoint {
        private String hour;
        private long in;
        private long out;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentBooking {
        private Long id;
        private Long userId;
        private String userFullName;
        private String slotNumber;
        private String parkingName;
        private String floorName;
        private String zoneName;
        private String status;
        private String paymentStatus;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentIncident {
        private Long id;
        private String type;
        private String title;
        private String description;
        private String status;
        private String licensePlate;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleActivity {
        private Long id;
        private String orderCode;
        private String licensePlate;
        private String parkingName;
        private String floorName;
        private String vehicleType;
        private String customerType;
        private String visitorCardCode;
        private String status;
        private LocalDateTime entryTime;
        private LocalDateTime exitTime;
        private BigDecimal calculatedFee;
        private LocalDateTime updatedAt;
    }
}
