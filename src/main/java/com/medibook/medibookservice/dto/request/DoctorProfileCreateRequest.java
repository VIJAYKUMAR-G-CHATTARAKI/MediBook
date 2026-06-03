package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request payload for a doctor creating their professional profile.
 * Used in POST /doctors/profile (authenticated DOCTOR role).
 */
public class DoctorProfileCreateRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    private String fullName;

    @Pattern(
            regexp = "^[+]?[0-9]{10,15}$",
            message = "Invalid phone number format"
    )
    private String phoneNumber;

    @NotBlank(message = "Specialization is required")
    @Size(max = 50, message = "Specialization must not exceed 50 characters")
    private String specialization;

    @NotBlank(message = "Qualification is required")
    @Size(max = 200, message = "Qualification must not exceed 200 characters")
    private String qualification;

    @NotNull(message = "Experience years is required")
    @Min(value = 0, message = "Experience years cannot be negative")
    @Max(value = 70, message = "Experience years seems unrealistic")
    private Integer experienceYears;

    @NotBlank(message = "License number is required")
    @Size(max = 50, message = "License number must not exceed 50 characters")
    private String licenseNumber;

    @Size(max = 5000, message = "Bio must not exceed 5000 characters")
    private String bio;

    @Size(max = 500, message = "Profile picture URL too long")
    @Pattern(
            regexp = "^(https?://.*)?$",
            message = "Profile picture URL must start with http:// or https://"
    )
    private String profilePictureUrl;

    @DecimalMin(value = "0.0", inclusive = true, message = "Consultation fee cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Consultation fee too large")
    private BigDecimal consultationFee;

    @Size(max = 200, message = "Languages list too long")
    private String languagesSpoken;

    // Default constructor
    public DoctorProfileCreateRequest() {}

    // Getters and setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public String getLanguagesSpoken() { return languagesSpoken; }
    public void setLanguagesSpoken(String languagesSpoken) { this.languagesSpoken = languagesSpoken; }
}