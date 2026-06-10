package com.medibook.medibookservice.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Master configuration for a Doctor's appointment slots.
 *
 * <p>Holds universal slot settings (duration, max bookings, timezone).
 * Per-day working hours are stored in {@link DoctorWeeklySchedule}.</p>
 *
 * <p>One configuration per doctor (enforced by unique constraint on doctor_id).</p>
 */
@Entity
@Table(name = "doctor_slot_configs", indexes = {
        @Index(name = "idx_slot_config_doctor", columnList = "doctor_id", unique = true),
        @Index(name = "idx_slot_config_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
public class DoctorSlotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The Doctor this configuration belongs to.
     * One-to-One: each Doctor has exactly one slot config.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", referencedColumnName = "id",
            nullable = false, unique = true)
    private Doctor doctor;

    /**
     * Duration of each slot in minutes (e.g., 30 for half-hour slots).
     */
    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes = 30;

    /**
     * Maximum patients allowed per slot (e.g., 2 for double-booking).
     */
    @Column(name = "max_bookings_per_slot", nullable = false)
    private Integer maxBookingsPerSlot = 2;

    /**
     * Timezone for slot times (e.g., "Asia/Kolkata").
     * Used to convert between server time and display time.
     */
    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Asia/Kolkata";

    /**
     * Whether this config is active. Inactive configs disable slot generation.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Per-day schedule (7 entries: MON-SUN).
     * Cascade: when DoctorSlotConfig is deleted, all schedules are deleted too.
     */
    @OneToMany(mappedBy = "slotConfig", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DoctorWeeklySchedule> weeklySchedules = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public DoctorSlotConfig() {
    }

    // =========================================================
    // HELPER METHOD - bidirectional relationship management
    // =========================================================

    /**
     * Add a weekly schedule and maintain the bidirectional link.
     */
    public void addWeeklySchedule(DoctorWeeklySchedule schedule) {
        weeklySchedules.add(schedule);
        schedule.setSlotConfig(this);
    }

    /**
     * Remove a weekly schedule and break the bidirectional link.
     */
    public void removeWeeklySchedule(DoctorWeeklySchedule schedule) {
        weeklySchedules.remove(schedule);
        schedule.setSlotConfig(null);
    }

    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(Integer slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public Integer getMaxBookingsPerSlot() { return maxBookingsPerSlot; }
    public void setMaxBookingsPerSlot(Integer maxBookingsPerSlot) {
        this.maxBookingsPerSlot = maxBookingsPerSlot;
    }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<DoctorWeeklySchedule> getWeeklySchedules() { return weeklySchedules; }
    public void setWeeklySchedules(List<DoctorWeeklySchedule> weeklySchedules) {
        this.weeklySchedules = weeklySchedules;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}