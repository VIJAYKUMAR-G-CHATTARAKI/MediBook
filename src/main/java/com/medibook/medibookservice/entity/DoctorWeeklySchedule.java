package com.medibook.medibookservice.entity;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Per-day working hours for a doctor.
 *
 * <p>One row per day of the week (MON-SUN). Belongs to one
 * {@link DoctorSlotConfig}. Allows different hours per day
 * (e.g., full day weekdays, half day Saturday, closed Sunday).</p>
 *
 * <p>When {@code isWorkingDay = false}, start/end times are null.</p>
 */
@Entity
@Table(name = "doctor_weekly_schedules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_schedule_config_day",
                        columnNames = {"slot_config_id", "day_of_week"}
                )
        },
        indexes = {
                @Index(name = "idx_schedule_config", columnList = "slot_config_id"),
                @Index(name = "idx_schedule_day", columnList = "day_of_week")
        })
public class DoctorWeeklySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The slot config this schedule entry belongs to.
     * Many-to-One: 7 schedules per config (one per day).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_config_id", referencedColumnName = "id",
            nullable = false)
    private DoctorSlotConfig slotConfig;

    /**
     * Which day of the week this entry represents.
     * Stored as enum string: "MONDAY", "TUESDAY", etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    /**
     * Whether the doctor works on this day.
     * If false, start/end times should be null.
     */
    @Column(name = "is_working_day", nullable = false)
    private boolean isWorkingDay = true;

    /**
     * Start of working hours (e.g., 10:00).
     * NULL when isWorkingDay is false.
     */
    @Column(name = "work_start_time")
    private LocalTime workStartTime;

    /**
     * End of working hours (e.g., 20:00).
     * NULL when isWorkingDay is false.
     */
    @Column(name = "work_end_time")
    private LocalTime workEndTime;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public DoctorWeeklySchedule() {
    }

    /**
     * Convenience constructor for a working day.
     */
    public DoctorWeeklySchedule(DayOfWeek dayOfWeek,
                                LocalTime workStartTime,
                                LocalTime workEndTime) {
        this.dayOfWeek = dayOfWeek;
        this.isWorkingDay = true;
        this.workStartTime = workStartTime;
        this.workEndTime = workEndTime;
    }

    /**
     * Convenience factory for a non-working day.
     */
    public static DoctorWeeklySchedule nonWorkingDay(DayOfWeek dayOfWeek) {
        DoctorWeeklySchedule schedule = new DoctorWeeklySchedule();
        schedule.dayOfWeek = dayOfWeek;
        schedule.isWorkingDay = false;
        schedule.workStartTime = null;
        schedule.workEndTime = null;
        return schedule;
    }

    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DoctorSlotConfig getSlotConfig() { return slotConfig; }
    public void setSlotConfig(DoctorSlotConfig slotConfig) { this.slotConfig = slotConfig; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public boolean isWorkingDay() { return isWorkingDay; }
    public void setWorkingDay(boolean workingDay) { isWorkingDay = workingDay; }

    public LocalTime getWorkStartTime() { return workStartTime; }
    public void setWorkStartTime(LocalTime workStartTime) { this.workStartTime = workStartTime; }

    public LocalTime getWorkEndTime() { return workEndTime; }
    public void setWorkEndTime(LocalTime workEndTime) { this.workEndTime = workEndTime; }
}