package com.medibook.medibookservice.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records a date when a doctor is NOT available, overriding their regular schedule.
 *
 * <p>Used during slot generation: any holiday date is skipped, no slots created.</p>
 *
 * <p>Examples: festivals (Diwali, Christmas), personal leave, conferences, vacations.</p>
 *
 * <p>One holiday entry per (doctor, date) - enforced by unique constraint.</p>
 */
@Entity
@Table(name = "doctor_holidays",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_holiday_doctor_date",
                        columnNames = {"doctor_id", "holiday_date"}
                )
        },
        indexes = {
                @Index(name = "idx_holiday_doctor", columnList = "doctor_id"),
                @Index(name = "idx_holiday_date", columnList = "holiday_date"),
                @Index(name = "idx_holiday_doctor_date",
                        columnList = "doctor_id, holiday_date")
        })
@EntityListeners(AuditingEntityListener.class)
public class DoctorHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The doctor this holiday applies to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", referencedColumnName = "id", nullable = false)
    private Doctor doctor;

    /**
     * The date the doctor is unavailable.
     */
    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    /**
     * Human-readable reason for the holiday.
     * Examples: "Diwali", "Personal Leave", "Conference - Mumbai", "Sick Leave"
     */
    @Column(name = "reason", nullable = false, length = 200)
    private String reason;

    /**
     * ID of the admin user who created this holiday entry (audit trail).
     */
    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public DoctorHoliday() {
    }

    public DoctorHoliday(Doctor doctor, LocalDate holidayDate,
                         String reason, Long createdByUserId) {
        this.doctor = doctor;
        this.holidayDate = holidayDate;
        this.reason = reason;
        this.createdByUserId = createdByUserId;
    }

    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}