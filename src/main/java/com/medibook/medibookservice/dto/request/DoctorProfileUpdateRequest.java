package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request payload for a doctor updating their professional profile.
 * Note: licenseNumber is NOT updatable (immutable after creation).
 * Used in PUT /doctors/profile (authenticated DOCTOR role).
 */
public class DoctorProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phoneNumber;

    @NotBlank(message = "Specialization is required")
    @Size(max = 50)
    private String specialization;

    @NotBlank(message = "Qualification is required")
    @Size(max = 200)
    private String qualification;

    @NotNull(message = "Experience years is required")
    @Min(value = 0) @Max(value = 70)
    private Integer experienceYears;

    @Size(max = 5000)
    private String bio;

    @Size(max = 500)
    @Pattern(regexp = "^(https?://.*)?$", message = "Profile picture URL must start with http:// or https://")
    private String profilePictureUrl;

    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "99999999.99")
    private BigDecimal consultationFee;

    @Size(max = 200)
    private String languagesSpoken;

    public DoctorProfileUpdateRequest() {}

    // Getters and setters (same fields as Create, minus licenseNumber)
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

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    public String getLanguagesSpoken() { return languagesSpoken; }
    public void setLanguagesSpoken(String languagesSpoken) { this.languagesSpoken = languagesSpoken; }
}