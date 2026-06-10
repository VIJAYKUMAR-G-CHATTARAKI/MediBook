package com.medibook.medibookservice.service.impl;

import com.medibook.medibookservice.dto.request.CancelSlotRequest;
import com.medibook.medibookservice.dto.response.SlotResponse;
import com.medibook.medibookservice.dto.response.SlotSummaryResponse;
import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorSlot;
import com.medibook.medibookservice.enums.DoctorSlotStatus;
import com.medibook.medibookservice.exception.BadRequestException;
import com.medibook.medibookservice.exception.ResourceNotFoundException;
import com.medibook.medibookservice.mapper.SlotMapper;
import com.medibook.medibookservice.repository.DoctorRepository;
import com.medibook.medibookservice.repository.DoctorSlotRepository;
import com.medibook.medibookservice.service.DoctorSlotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DoctorSlotServiceImpl implements DoctorSlotService {

    private static final Logger log = LoggerFactory.getLogger(DoctorSlotServiceImpl.class);

    private final DoctorRepository doctorRepository;
    private final DoctorSlotRepository slotRepository;
    private final SlotMapper slotMapper;

    public DoctorSlotServiceImpl(DoctorRepository doctorRepository,
                                 DoctorSlotRepository slotRepository,
                                 SlotMapper slotMapper) {
        this.doctorRepository = doctorRepository;
        this.slotRepository = slotRepository;
        this.slotMapper = slotMapper;
    }

    // =========================================================
    // AUTHENTICATED VIEWS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<SlotResponse> getSlotsForDoctorByDate(Long doctorId, LocalDate date) {
        Doctor doctor = findDoctorById(doctorId);
        List<DoctorSlot> slots = slotRepository
                .findByDoctorAndSlotDateOrderByStartTimeAsc(doctor, date);
        return slots.stream().map(slotMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SlotResponse> getSlotsForDoctorInRange(
            Long doctorId, LocalDate startDate, LocalDate endDate, Pageable pageable) {

        Doctor doctor = findDoctorById(doctorId);

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }

        return slotRepository
                .findByDoctorAndSlotDateBetween(doctor, startDate, endDate, pageable)
                .map(slotMapper::toResponse);
    }

    @Override
    @Transactional
    public SlotResponse cancelSlot(Long slotId, CancelSlotRequest request, Long cancelledByUserId) {
        log.info("Cancelling slot {} by user {}", slotId, cancelledByUserId);

        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot not found with id: " + slotId));

        // Validate current state - can't cancel already-cancelled or expired slots
        if (slot.getStatus() == DoctorSlotStatus.CANCELLED) {
            throw new BadRequestException("Slot is already cancelled");
        }
        if (slot.getStatus() == DoctorSlotStatus.EXPIRED) {
            throw new BadRequestException("Cannot cancel expired slot");
        }

        // Apply cancellation
        slot.setStatus(DoctorSlotStatus.CANCELLED);
        slot.setCancellationReason(request.getReason());
        slot.setCancelledByUserId(cancelledByUserId);
        slot.setCancelledAt(LocalDateTime.now());
        // JPA dirty checking saves automatically

        log.info("Slot {} cancelled. Had {} bookings", slotId, slot.getCurrentBookings());

        // TODO Phase 5: notify booked patients about cancellation

        return slotMapper.toResponse(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public SlotResponse getSlotById(Long slotId) {
        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot not found with id: " + slotId));
        return slotMapper.toResponse(slot);
    }

    // =========================================================
    // PUBLIC VIEWS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<SlotSummaryResponse> getAvailableSlotsByDoctorAndDate(
            Long doctorId, LocalDate date) {

        // Verify doctor exists (will throw 404 if not, hiding non-existence)
        findDoctorById(doctorId);

        List<DoctorSlot> slots = slotRepository.findAvailableSlotsByDoctorAndDate(doctorId, date);
        return slots.stream().map(slotMapper::toSummaryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotSummaryResponse> getAvailableSlotsInRange(
            Long doctorId, LocalDate startDate, LocalDate endDate) {

        findDoctorById(doctorId);

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before or equal to end date");
        }

        // Prevent huge range queries (DOS protection)
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        if (days > 90) {
            throw new BadRequestException("Range cannot exceed 90 days");
        }

        List<DoctorSlot> slots = slotRepository.findAvailableSlotsInRange(
                doctorId, startDate, endDate);
        return slots.stream().map(slotMapper::toSummaryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotSummaryResponse> getNextAvailableSlots(Long doctorId, int limit) {
        findDoctorById(doctorId);

        if (limit <= 0 || limit > 100) {
            throw new BadRequestException("Limit must be between 1 and 100");
        }

        LocalDate today = LocalDate.now();
        Pageable pageable = PageRequest.of(0, limit);

        List<DoctorSlot> slots = slotRepository.findNextAvailableSlots(
                doctorId,
                today,
                java.time.LocalTime.now(),
                pageable
        );
        return slots.stream().map(slotMapper::toSummaryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getDatesWithAvailability(Long doctorId, LocalDate fromDate) {
        findDoctorById(doctorId);
        return slotRepository.findDistinctAvailableDates(doctorId, fromDate);
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private Doctor findDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
    }
}