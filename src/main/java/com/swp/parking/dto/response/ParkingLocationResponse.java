package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về vị trí đỗ (tầng/bãi) và thời gian gửi xe cho từng xe đang hoạt động.
 * Không theo dõi ô đỗ cụ thể – chỉ hiển thị thông tin tầng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLocationResponse {

    private String licensePlate;
    private String parkingName;
    private String floorName;
    private Integer floorNumber;
    private LocalDateTime entryTime;
    private Long durationMinutes;
}
