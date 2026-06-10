package com.medibook.medibookservice.mapper;

import com.medibook.medibookservice.dto.request.SlotConfigRequest;
import com.medibook.medibookservice.dto.request.WeeklyScheduleRequest;
import com.medibook.medibookservice.dto.response.SlotConfigResponse;
import com.medibook.medibookservice.dto.response.WeeklyScheduleResponse;
import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorSlotConfig;
import com.medibook.medibookservice.entity.DoctorWeeklySchedule;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for DoctorSlotConfig and its nested DoctorWeeklySchedule entities.
 *
 * <p>Bundles both since they're always created/loaded together as a unit.</p>
 */
@Component
public class SlotConfigMapper {

    // =========================================================
    // DTO -> ENTITY (for creation)
    // =========================================================

    /**
     * Build a new DoctorSlotConfig from request + doctor.
     * Creates all 7 weekly schedules - defaults to non-working
     * for any day not specified in the request.
     */
    public DoctorSlotConfig toEntity(SlotConfigRequest request, Doctor doctor) {
        if (request == null) return null;

        DoctorSlotConfig config = new DoctorSlotConfig();
        config.setDoctor(doctor);
        config.setSlotDurationMinutes(request.getSlotDurationMinutes());
        config.setMaxBookingsPerSlot(request.getMaxBookingsPerSlot());
        config.setTimezone(request.getTimezone());
        config.setActive(request.getActive() != null ? request.getActive() : true);

        // Build the 7 weekly schedules
        buildWeeklySchedules(config, request.getWeeklySchedules());

        return config;
    }

    // =========================================================
    // DTO -> ENTITY (for updates)
    // =========================================================

    /**
     * Update an existing config with new values (in-place for dirty checking).
     * Rebuilds all weekly schedules from scratch.
     */
    public void updateEntity(DoctorSlotConfig config, SlotConfigRequest request) {
        if (config == null || request == null) return;

        config.setSlotDurationMinutes(request.getSlotDurationMinutes());
        config.setMaxBookingsPerSlot(request.getMaxBookingsPerSlot());
        config.setTimezone(request.getTimezone());
        if (request.getActive() != null) {
            config.setActive(request.getActive());
        }

        // Clear existing schedules (orphanRemoval=true triggers deletes)
        config.getWeeklySchedules().clear();

        // Rebuild
        buildWeeklySchedules(config, request.getWeeklySchedules());
    }

    // =========================================================
    // BUILD WEEKLY SCHEDULES (helper)
    // =========================================================

    /**
     * Build 7 weekly schedule entries.
     * Days not in the request become non-working days.
     */
    private void buildWeeklySchedules(DoctorSlotConfig config,
                                      List<WeeklyScheduleRequest> requests) {
        // Iterate all 7 days
        for (DayOfWeek day : DayOfWeek.values()) {
            // Find request for this day, or default to non-working
            WeeklyScheduleRequest matchingRequest = findRequestForDay(requests, day);

            DoctorWeeklySchedule schedule;
            if (matchingRequest != null && Boolean.TRUE.equals(matchingRequest.getIsWorkingDay())) {
                schedule = new DoctorWeeklySchedule(
                        day,
                        matchingRequest.getWorkStartTime(),
                        matchingRequest.getWorkEndTime()
                );
            } else {
                // Day not specified OR explicitly non-working
                schedule = DoctorWeeklySchedule.nonWorkingDay(day);
            }

            // Use helper method to maintain bidirectional link
            config.addWeeklySchedule(schedule);
        }
    }

    /**
     * Find the request entry for a specific day, or null if not present.
     */
    private WeeklyScheduleRequest findRequestForDay(
            List<WeeklyScheduleRequest> requests, DayOfWeek day) {
        if (requests == null) return null;
        return requests.stream()
                .filter(r -> r.getDayOfWeek() == day)
                .findFirst()
                .orElse(null);
    }

    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    /**
     * Convert SlotConfig entity to response DTO.
     */
    public SlotConfigResponse toResponse(DoctorSlotConfig config) {
        if (config == null) return null;

        SlotConfigResponse response = new SlotConfigResponse();
        response.setId(config.getId());

        if (config.getDoctor() != null) {
            response.setDoctorId(config.getDoctor().getId());
            response.setDoctorName(config.getDoctor().getFullName());
        }

        response.setSlotDurationMinutes(config.getSlotDurationMinutes());
        response.setMaxBookingsPerSlot(config.getMaxBookingsPerSlot());
        response.setTimezone(config.getTimezone());
        response.setActive(config.isActive());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());

        // Map weekly schedules
        if (config.getWeeklySchedules() != null) {
            List<WeeklyScheduleResponse> scheduleResponses = config.getWeeklySchedules().stream()
                    .map(this::toWeeklyScheduleResponse)
                    .collect(Collectors.toList());
            response.setWeeklySchedules(scheduleResponses);
        } else {
            response.setWeeklySchedules(new ArrayList<>());
        }

        return response;
    }

    /**
     * Convert a single WeeklySchedule entity to response DTO.
     */
    public WeeklyScheduleResponse toWeeklyScheduleResponse(DoctorWeeklySchedule schedule) {
        if (schedule == null) return null;

        WeeklyScheduleResponse response = new WeeklyScheduleResponse();
        response.setId(schedule.getId());
        response.setDayOfWeek(schedule.getDayOfWeek());
        response.setIsWorkingDay(schedule.isWorkingDay());
        response.setWorkStartTime(schedule.getWorkStartTime());
        response.setWorkEndTime(schedule.getWorkEndTime());
        return response;
    }
}