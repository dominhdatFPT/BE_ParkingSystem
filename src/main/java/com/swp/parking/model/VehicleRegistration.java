package com.swp.parking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_registrations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(columnDefinition = "TEXT")
    private String brand;

    @Column(columnDefinition = "TEXT")
    private String color;

    @Column(name = "cccd_front_image", columnDefinition = "TEXT")
    private String cccdFrontImage;

    @Column(name = "cccd_back_image", columnDefinition = "TEXT")
    private String cccdBackImage;

    @Column(name = "license_image", columnDefinition = "TEXT")
    private String licenseImage;

    @Column(name = "vehicle_document_image", columnDefinition = "TEXT")
    private String vehicleDocumentImage;

    @Column(name = "plate_image", columnDefinition = "TEXT")
    private String plateImage;

    @Column(name = "ekyc_cccd_id")
    private String ekycCccdId;

    @Column(name = "ekyc_full_name")
    private String ekycFullName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_fee_package_id")
    private FeePackage requestedFeePackage;

    @Builder.Default
    @Column(name = "registration_source", nullable = false)
    private String registrationSource = "FORM";

    @Column(name = "ekyc_date_of_birth")
    private LocalDate ekycDateOfBirth;

    @Column(name = "ekyc_gender")
    private String ekycGender;

    @Column(name = "ekyc_nationality")
    private String ekycNationality;

    @Column(name = "ekyc_place_of_origin", columnDefinition = "TEXT")
    private String ekycPlaceOfOrigin;

    @Column(name = "ekyc_place_of_residence", columnDefinition = "TEXT")
    private String ekycPlaceOfResidence;

    @Column(name = "ekyc_cccd_issue_date")
    private LocalDate ekycCccdIssueDate;

    @Column(name = "ekyc_cccd_expiry_date")
    private LocalDate ekycCccdExpiryDate;

    @Column(name = "ekyc_license_number")
    private String ekycLicenseNumber;

    @Column(name = "ekyc_license_class")
    private String ekycLicenseClass;

    @Column(name = "ekyc_license_issue_date")
    private LocalDate ekycLicenseIssueDate;

    @Column(name = "ekyc_license_expiry")
    private LocalDate ekycLicenseExpiry;

    @Column(name = "ekyc_issuing_authority", columnDefinition = "TEXT")
    private String ekycIssuingAuthority;

    @Column(name = "ekyc_is_valid")
    private Boolean ekycIsValid;

    @Column(name = "ekyc_is_fake")
    private Boolean ekycIsFake;

    @Column(name = "ekyc_confidence_score")
    private Double ekycConfidenceScore;

    @Column(name = "ekyc_document_type")
    private String ekycDocumentType;

    @Column(name = "ekyc_face_match_score")
    private Double ekycFaceMatchScore;

    @Builder.Default
    private String status = "PENDING";

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
