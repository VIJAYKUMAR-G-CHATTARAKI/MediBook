package com.medibook.medibookservice.service.impl;

import com.medibook.medibookservice.dto.request.BulkHolidayRequest;
import com.medibook.medibookservice.dto.request.HolidayRequest;
import com.medibook.medibookservice.dto.response.HolidayResponse;
import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorHoliday;
import com.medibook.medibookservice.exception.BadRequestException;
import com.medibook.medibookservice.exception.ResourceAlreadyExistsException;
import com.medibook.medibookservice.exception.ResourceNotFoundException;
import com.medibook.medibookservice.mapper.HolidayMapper;
import com.medibook.medibookservice.repository.DoctorHolidayRepository;
import com.medibook.medibookservice.repository.DoctorRepository;
import com.medibook.medibookservice.repository.DoctorSlotRepository;
import com.medibook.medibookservice.service.DoctorHolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorHolidayServiceImpl implements DoctorHolidayService {

    private static final Logger log = LoggerFactory.getLogger(DoctorHolidayServiceImpl.class);

    private final DoctorRepository doctorRepository;
    private final DoctorHolidayRepository holidayRepository;
    private final DoctorSlotRepository slotRepository;
    private final HolidayMapper holidayMapper;

    public DoctorHolidayServiceImpl(DoctorRepository doctorRepository,
                                    DoctorHolidayRepository holidayRepository,
                                    DoctorSlotRepository slotRepository,
                                    HolidayMapper holidayMapper) {
        this.doctorRepository = doctorRepository;
        this.holidayRepository = holidayRepository;
        this.slotRepository = slotRepository;
        this.holidayMapper = holidayMapper;
    }

    // =========================================================
    // ADD SINGLE HOLIDAY (with auto-cancel slots)
    // =========================================================

    @Override
    @Transactional
    public HolidayResponse addHoliday(Long doctorId, HolidayRequest request, Long adminUserId) {
        log.info("Adding holiday {} for doctor {}", request.getHolidayDate(), doctorId);

        Doctor doctor = findDoctorById(doctorId);

        // Check if holiday already exists for this date
        if (holidayRepository.existsByDoctorAndHolidayDate(doctor, request.getHolidayDate())) {
            throw new ResourceAlreadyExistsException(
                    "Holiday already exists for doctor " + doctorId
                            + " on date " + request.getHolidayDate());
        }

        // Create holiday entity
        DoctorHoliday holiday = holidayMapper.toEntity(request, doctor, adminUserId);
        holiday = holidayRepository.save(holiday);

        // Auto-cancel existing slots (per your design choice!)
        if (Boolean.TRUE.equals(request.getCancelExistingSlots())) {
            int cancelled = cancelSlotsForDate(
                    doctor, request.getHolidayDate(), request.getReason(), adminUserId);
            log.info("Auto-cancelled {} existing slots for {}",
                    cancelled, request.getHolidayDate());
        }

        log.info("Holiday added: id={}, date={}, reason={}",
                holiday.getId(), holiday.getHolidayDate(), holiday.getReason());
        return holidayMapper.toResponse(holiday);
    }

    // =========================================================
    // ADD BULK HOLIDAYS (e.g., vacation week)
    // =========================================================

    @Override
    @Transactional
    public List<HolidayResponse> addBulkHolidays(Long doctorId, BulkHolidayRequest request,
                                                 Long adminUserId) {
        log.info("Adding {} holidays for doctor {}",
                request.getHolidayDates().size(), doctorId);

        Doctor doctor = findDoctorById(doctorId);

        // Check for any duplicates upfront (fail-fast)
        for (LocalDate date : request.getHolidayDates()) {
            if (holidayRepository.existsByDoctorAndHolidayDate(doctor, date)) {
                throw new ResourceAlreadyExistsException(
                        "Holiday already exists for doctor " + doctorId + " on date " + date);
            }
        }

        // Create all holiday entities
        List<DoctorHoliday> holidays = new ArrayList<>();
        for (LocalDate date : request.getHolidayDates()) {
            DoctorHoliday holiday = new DoctorHoliday(
                    doctor, date, request.getReason(), adminUserId);
            holidays.add(holiday);
        }

        // Bulk save
        holidays = holidayRepository.saveAll(holidays);

        // Auto-cancel slots for each date
        if (Boolean.TRUE.equals(request.getCancelExistingSlots())) {
            int totalCancelled = 0;
            for (LocalDate date : request.getHolidayDates()) {
                totalCancelled += cancelSlotsForDate(doctor, date, request.getReason(), adminUserId);
            }
            log.info("Auto-cancelled {} total slots across {} dates",
                    totalCancelled, request.getHolidayDates().size());
        }

        log.info("{} holidays added for doctor {}", holidays.size(), doctorId);
        return holidays.stream()
                .map(holidayMapper::toResponse)
                .toList();
    }

    // =========================================================
    // REMOVE HOLIDAY
    // =========================================================

    @Override
    @Transactional
    public void removeHoliday(Long holidayId) {
        log.info("Removing holiday {}", holidayId);

        DoctorHoliday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Holiday not found with id: " + holidayId));

        // Note: We don't restore the cancelled slots.
        // Admin must re-generate slots for that date if needed.
        // This is intentional - cancellations are part of history.

        holidayRepository.delete(holiday);
        log.info("Holiday {} removed (slots remain cancelled)", holidayId);
    }

    // =========================================================
    // QUERY HOLIDAYS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<HolidayResponse> getHolidaysForDoctor(Long doctorId, Pageable pageable) {
        Doctor doctor = findDoctorById(doctorId);
        return holidayRepository.findByDoctor(doctor, pageable)
                .map(holidayMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> getUpcomingHolidays(Long doctorId) {
        Doctor doctor = findDoctorById(doctorId);
        List<DoctorHoliday> upcoming = holidayRepository.findUpcomingHolidays(
                doctor, LocalDate.now());
        return upcoming.stream()
                .map(holidayMapper::toResponse)
                .toList();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private Doctor findDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
    }

    /**
     * Bulk cancel slots for a doctor on a specific date.
     * Returns the number of slots cancelled.
     */
    private int cancelSlotsForDate(Doctor doctor, LocalDate date,
                                   String reason, Long adminUserId) {
        String fullReason = "Holiday: " + reason;
        return slotRepository.bulkCancelSlotsForDate(
                doctor.getId(),
                date,
                fullReason,
                adminUserId,
                LocalDateTime.now()
        );
    }
}