package com.medibook.medibookservice.service.impl;

import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorHoliday;
import com.medibook.medibookservice.entity.DoctorSlot;
import com.medibook.medibookservice.entity.DoctorSlotConfig;
import com.medibook.medibookservice.entity.DoctorWeeklySchedule;
import com.medibook.medibookservice.repository.DoctorHolidayRepository;
import com.medibook.medibookservice.repository.DoctorSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for generating doctor appointment slots.
 *
 * <p>Takes a doctor's config + weekly schedule + holidays + date range,
 * and produces a list of DoctorSlot entities ready to be persisted.</p>
 *
 * <p>This is a stateless utility service - no internal state, pure computation.</p>
 */
@Service
public class SlotGenerationService {

    private static final Logger log = LoggerFactory.getLogger(SlotGenerationService.class);

    private final DoctorHolidayRepository holidayRepository;
    private final DoctorSlotRepository slotRepository;

    public SlotGenerationService(DoctorHolidayRepository holidayRepository,
                                 DoctorSlotRepository slotRepository) {
        this.holidayRepository = holidayRepository;
        this.slotRepository = slotRepository;
    }

    /**
     * Generate slots for a doctor over a date range.
     *
     * @param doctor       The doctor for whom slots are generated
     * @param config       Slot configuration (duration, max bookings, etc.)
     * @param startDate    Start of date range (inclusive)
     * @param endDate      End of date range (inclusive)
     * @param skipExisting If true, skip dates that already have slots
     * @return List of newly generated slots (not yet persisted)
     */
    public List<DoctorSlot> generateSlots(Doctor doctor,
                                          DoctorSlotConfig config,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          boolean skipExisting) {
        log.info("Generating slots for doctor {} from {} to {}",
                doctor.getId(), startDate, endDate);

        // STEP 1: Validate inputs
        validateDateRange(startDate, endDate);

        // STEP 2: Load holidays once for the entire range (performance)
        Set<LocalDate> holidayDates = loadHolidayDates(doctor, startDate, endDate);
        log.debug("Found {} holidays in range", holidayDates.size());

        // STEP 3: Map weekly schedules by day for quick lookup
        Map<DayOfWeek, DoctorWeeklySchedule> scheduleByDay = mapSchedulesByDay(config);

        // STEP 4: Iterate each day in the range
        List<DoctorSlot> generatedSlots = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<DoctorSlot> slotsForDay = generateSlotsForDay(
                    doctor, config, currentDate,
                    scheduleByDay, holidayDates, skipExisting
            );
            generatedSlots.addAll(slotsForDay);
            currentDate = currentDate.plusDays(1);
        }

        log.info("Generated {} slots for doctor {}", generatedSlots.size(), doctor.getId());
        return generatedSlots;
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    /**
     * Validate the date range is sensible.
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date " + startDate + " must be before or equal to end date " + endDate);
        }

        // Prevent generating too many days (DOS protection)
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        if (days > 365) {
            throw new IllegalArgumentException(
                    "Cannot generate slots for more than 365 days at once. Requested: " + days);
        }
    }

    /**
     * Load all holiday dates for the given doctor and range.
     * Using a Set for O(1) lookup during iteration.
     */
    private Set<LocalDate> loadHolidayDates(Doctor doctor,
                                            LocalDate startDate,
                                            LocalDate endDate) {
        List<DoctorHoliday> holidays = holidayRepository.findByDoctorAndDateRange(
                doctor, startDate, endDate);
        return holidays.stream()
                .map(DoctorHoliday::getHolidayDate)
                .collect(Collectors.toSet());
    }

    /**
     * Build a map from DayOfWeek to schedule entry for O(1) lookup.
     */
    private Map<DayOfWeek, DoctorWeeklySchedule> mapSchedulesByDay(DoctorSlotConfig config) {
        Map<DayOfWeek, DoctorWeeklySchedule> map = new HashMap<>();
        if (config.getWeeklySchedules() != null) {
            for (DoctorWeeklySchedule schedule : config.getWeeklySchedules()) {
                map.put(schedule.getDayOfWeek(), schedule);
            }
        }
        return map;
    }

    /**
     * Generate all slots for ONE specific date.
     * Returns empty list if it's a holiday, non-working day, or skipped.
     */
    private List<DoctorSlot> generateSlotsForDay(Doctor doctor,
                                                 DoctorSlotConfig config,
                                                 LocalDate date,
                                                 Map<DayOfWeek, DoctorWeeklySchedule> scheduleByDay,
                                                 Set<LocalDate> holidayDates,
                                                 boolean skipExisting) {
        // CHECK 1: Is it a holiday?
        if (holidayDates.contains(date)) {
            log.debug("Skipping {} - holiday", date);
            return List.of();
        }

        // CHECK 2: Look up the schedule for this day of week
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        DoctorWeeklySchedule schedule = scheduleByDay.get(dayOfWeek);

        if (schedule == null) {
            log.debug("Skipping {} - no schedule for {}", date, dayOfWeek);
            return List.of();
        }

        // CHECK 3: Is doctor working on this day of week?
        if (!schedule.isWorkingDay()
                || schedule.getWorkStartTime() == null
                || schedule.getWorkEndTime() == null) {
            log.debug("Skipping {} - {} is non-working", date, dayOfWeek);
            return List.of();
        }

        // CHECK 4: If skipExisting, check if any slot exists for this date
        if (skipExisting && slotsAlreadyExist(doctor, date, schedule.getWorkStartTime())) {
            log.debug("Skipping {} - slots already exist", date);
            return List.of();
        }

        // GENERATE: Build slots from work_start_time to work_end_time
        return buildSlotsForDay(doctor, config, date, schedule);
    }

    /**
     * Quick check if at least one slot exists for this date.
     * We check the FIRST slot's start time - if it exists, the whole day was generated.
     */
    private boolean slotsAlreadyExist(Doctor doctor, LocalDate date, LocalTime firstStartTime) {
        return slotRepository.existsByDoctorAndSlotDateAndStartTime(
                doctor, date, firstStartTime);
    }

    /**
     * Build the actual slots for a working day.
     * Steps from work_start to work_end by slot_duration_minutes.
     */
    private List<DoctorSlot> buildSlotsForDay(Doctor doctor,
                                              DoctorSlotConfig config,
                                              LocalDate date,
                                              DoctorWeeklySchedule schedule) {
        List<DoctorSlot> slots = new ArrayList<>();
        LocalTime currentStart = schedule.getWorkStartTime();
        LocalTime endOfDay = schedule.getWorkEndTime();
        int duration = config.getSlotDurationMinutes();
        int maxBookings = config.getMaxBookingsPerSlot();

        while (true) {
            LocalTime currentEnd = currentStart.plusMinutes(duration);

            // Stop if the slot would extend past end of day
            if (currentEnd.isAfter(endOfDay)) {
                break;
            }

            DoctorSlot slot = new DoctorSlot(
                    doctor, date, currentStart, currentEnd, maxBookings);
            slots.add(slot);

            // Move to next slot start
            currentStart = currentEnd;

            // Safety: if we've reached or passed end-of-day exactly, stop
            if (!currentStart.isBefore(endOfDay)) {
                break;
            }
        }

        log.debug("Generated {} slots for {} ({})", slots.size(), date, date.getDayOfWeek());
        return slots;
    }
}