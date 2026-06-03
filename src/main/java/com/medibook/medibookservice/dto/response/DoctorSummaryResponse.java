package com.medibook.medibookservice.dto.response;

import com.medibook.medibookservice.enums.DoctorStatus;

import java.math.BigDecimal;

/**
 * Minimal doctor summary for list views.
 * Used in pages where many doctors are listed - keeps payload small.
 */
public class DoctorSummaryResponse {

    private Long id;
    private String fullName;
    private String specialization;
    private Integer experienceYears;
    private BigDecimal consultationFee;
    private String profilePictureUrl;
    private DoctorStatus status;  // Only for admin views; null for public

    public DoctorSummaryResponse() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public DoctorStatus getStatus() { return status; }
    public void setStatus(DoctorStatus status) { this.status = status; }
}