package com.medibook.medibookservice.service.impl;

import com.medibook.medibookservice.dto.request.CancelBookingRequest;
import com.medibook.medibookservice.dto.request.CompleteBookingRequest;
import com.medibook.medibookservice.dto.request.CreateBookingRequest;
import com.medibook.medibookservice.dto.response.BookingResponse;
import com.medibook.medibookservice.dto.response.BookingSummaryResponse;
import com.medibook.medibookservice.entity.Booking;
import com.medibook.medibookservice.entity.BookingHistory;
import com.medibook.medibookservice.entity.DoctorSlot;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.enums.BookingStatus;
import com.medibook.medibookservice.enums.DoctorSlotStatus;
import com.medibook.medibookservice.exception.BadRequestException;
import com.medibook.medibookservice.exception.ResourceNotFoundException;
import com.medibook.medibookservice.mapper.BookingMapper;
import com.medibook.medibookservice.repository.BookingHistoryRepository;
import com.medibook.medibookservice.repository.BookingRepository;
import com.medibook.medibookservice.repository.DoctorSlotRepository;
import com.medibook.medibookservice.repository.UserRepository;
import com.medibook.medibookservice.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    /** Patient cancellation cutoff: cannot cancel within 2 hours of slot. */
    private static final int CANCELLATION_HOURS_LIMIT = 2;

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final DoctorSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final BookingReferenceGenerator referenceGenerator;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              BookingHistoryRepository bookingHistoryRepository,
                              DoctorSlotRepository slotRepository,
                              UserRepository userRepository,
                              BookingMapper bookingMapper,
                              BookingReferenceGenerator referenceGenerator) {
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.bookingMapper = bookingMapper;
        this.referenceGenerator = referenceGenerator;
    }

    // =========================================================
    // CREATE BOOKING (the critical one!)
    // =========================================================

    @Override
    @Transactional
    public BookingResponse createBooking(Long patientUserId, CreateBookingRequest request) {
        log.info("Creating booking for patient {} on slot {}",
                patientUserId, request.getSlotId());

        // STEP 1: Validate patient exists
        User patient = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", patientUserId));

        // STEP 2: Acquire PESSIMISTIC_WRITE lock on the slot
        //         → other transactions trying same slot will WAIT
        DoctorSlot slot = slotRepository.findByIdWithLock(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot", "id", request.getSlotId()));

        log.debug("Lock acquired on slot {}", slot.getId());

        // STEP 3: Validate slot state (fresh data inside lock)
        validateSlotForBooking(slot);

        // STEP 4: Check for duplicate booking (patient already has active booking for slot)
        if (bookingRepository.existsByPatientUserIdAndSlotIdAndStatusIn(
                patientUserId,
                slot.getId(),
                List.of(BookingStatus.BOOKED, BookingStatus.COMPLETED))) {
            throw new BadRequestException(
                    "You already have an active booking for this slot");
        }

        // STEP 5: Generate booking reference (DR5-20261209-001)
        String bookingReference = referenceGenerator.generate(
                slot.getDoctor().getId(),
                slot.getSlotDate());

        // STEP 6: Create booking entity
        Booking booking = new Booking(patient, slot, bookingReference);
        booking.setNotes(request.getNotes());
        booking = bookingRepository.save(booking);

        // STEP 7: Increment slot's booking count
        slot.setCurrentBookings(slot.getCurrentBookings() + 1);
        if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
            slot.setStatus(DoctorSlotStatus.FULLY_BOOKED);
        }
        slotRepository.save(slot);

        // STEP 8: Create audit history entry (CREATED action)
        BookingHistory history = BookingHistory.created(booking, patientUserId);
        bookingHistoryRepository.save(history);

        log.info("Booking created: id={}, reference={}, slot remaining={}",
                booking.getId(), bookingReference, slot.getRemainingSpots());

        return bookingMapper.toResponse(booking);
    }

    /**
     * Validate that a slot can accept a new booking.
     */
    private void validateSlotForBooking(DoctorSlot slot) {
        // Check 1: Slot status must be AVAILABLE
        if (slot.getStatus() != DoctorSlotStatus.AVAILABLE) {
            throw new BadRequestException(
                    "Slot is not available for booking. Status: " + slot.getStatus());
        }

        // Check 2: Slot must not be expired
        if (slot.isExpired()) {
            throw new BadRequestException(
                    "Cannot book a slot that has already passed");
        }

        // Check 3: Capacity available
        if (slot.getCurrentBookings() >= slot.getMaxBookings()) {
            throw new BadRequestException(
                    "Slot is fully booked. " +
                            slot.getCurrentBookings() + "/" + slot.getMaxBookings() + " bookings");
        }
    }

    // =========================================================
    // CANCEL BOOKING (with 2-hour rule for patients)
    // =========================================================

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId,
                                         CancelBookingRequest request,
                                         Long cancelledByUserId,
                                         String cancelledByRole) {
        log.info("Cancelling booking {} by user {} (role={})",
                bookingId, cancelledByUserId, cancelledByRole);

        // Lock the booking row
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking", "id", bookingId));

        // Validate booking can be cancelled
        if (!booking.isCancellable()) {
            throw new BadRequestException(
                    "Cannot cancel booking in state: " + booking.getStatus());
        }

        // Enforce 2-hour rule for PATIENT cancellations only
        // Doctor/admin can cancel anytime (medical reasons)
        if ("PATIENT".equals(cancelledByRole)) {
            validatePatientCancellationWindow(booking);
        }

        // Record old status for audit
        BookingStatus oldStatus = booking.getStatus();

        // Update booking
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getReason());
        booking.setCancelledByUserId(cancelledByUserId);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Decrement slot's booking count, mark AVAILABLE again if was FULLY_BOOKED
        DoctorSlot slot = booking.getSlot();
        slot.setCurrentBookings(Math.max(0, slot.getCurrentBookings() - 1));
        if (slot.getStatus() == DoctorSlotStatus.FULLY_BOOKED) {
            slot.setStatus(DoctorSlotStatus.AVAILABLE);
        }
        slotRepository.save(slot);

        // Audit trail
        BookingHistory history = BookingHistory.cancelled(
                booking, oldStatus, cancelledByUserId, request.getReason());
        bookingHistoryRepository.save(history);

        log.info("Booking {} cancelled. Slot now: {}/{} bookings",
                bookingId, slot.getCurrentBookings(), slot.getMaxBookings());

        return bookingMapper.toResponse(booking);
    }

    /**
     * Enforce: patients cannot cancel within 2 hours of slot time.
     */
    private void validatePatientCancellationWindow(Booking booking) {
        LocalDateTime slotDateTime = booking.getSlotDateTime();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = slotDateTime.minusHours(CANCELLATION_HOURS_LIMIT);

        if (now.isAfter(cutoff)) {
            throw new BadRequestException(
                    "Cannot cancel within " + CANCELLATION_HOURS_LIMIT +
                            " hours of the appointment. Please contact the clinic directly.");
        }
    }

    // =========================================================
    // COMPLETE BOOKING (doctor only, after slot time)
    // =========================================================

    @Override
    @Transactional
    public BookingResponse completeBooking(Long bookingId,
                                           CompleteBookingRequest request,
                                           Long doctorUserId) {
        log.info("Completing booking {} by doctor user {}", bookingId, doctorUserId);

        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking", "id", bookingId));

        // Must be in BOOKED state
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BadRequestException(
                    "Can only complete bookings in BOOKED state. Current: " + booking.getStatus());
        }

        // Slot time must have passed (can't complete a future booking!)
        if (!booking.isPastSlotTime()) {
            throw new BadRequestException(
                    "Cannot complete booking before its slot time");
        }

        BookingStatus oldStatus = booking.getStatus();

        // Update booking
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        if (request != null && request.getNotes() != null) {
            // Append doctor notes if present (keeping patient notes intact)
            String existingNotes = booking.getNotes() != null ? booking.getNotes() : "";
            booking.setNotes(existingNotes + "\n[Doctor notes]: " + request.getNotes());
        }
        bookingRepository.save(booking);

        // Audit trail
        BookingHistory history = BookingHistory.completed(booking, oldStatus, doctorUserId);
        bookingHistoryRepository.save(history);

        log.info("Booking {} marked COMPLETED", bookingId);
        return bookingMapper.toResponse(booking);
    }

    // =========================================================
    // MARK NO-SHOW (doctor only, after slot time)
    // =========================================================

    @Override
    @Transactional
    public BookingResponse markNoShow(Long bookingId, Long doctorUserId) {
        log.info("Marking booking {} as NO_SHOW by doctor user {}", bookingId, doctorUserId);

        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BadRequestException(
                    "Can only mark NO_SHOW for BOOKED bookings. Current: " + booking.getStatus());
        }

        if (!booking.isPastSlotTime()) {
            throw new BadRequestException(
                    "Cannot mark NO_SHOW before slot time has passed");
        }

        BookingStatus oldStatus = booking.getStatus();

        booking.setStatus(BookingStatus.NO_SHOW);
        bookingRepository.save(booking);

        BookingHistory history = BookingHistory.noShowMarked(booking, oldStatus, doctorUserId);
        bookingHistoryRepository.save(history);

        log.info("Booking {} marked NO_SHOW", bookingId);
        return bookingMapper.toResponse(booking);
    }

    // =========================================================
    // READ OPERATIONS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking", "id", bookingId));
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking with reference " + bookingReference + " not found"));
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingSummaryResponse> getBookingsForPatient(Long patientUserId,
                                                              Pageable pageable) {
        User patient = userRepository.findById(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", patientUserId));
        return bookingRepository.findByPatientUser(patient, pageable)
                .map(bookingMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingSummaryResponse> getBookingsForDoctor(Long doctorId, Pageable pageable) {
        // Use the date range method with a very wide range (or query for all)
        java.time.LocalDate veryEarlyDate = java.time.LocalDate.of(2000, 1, 1);
        java.time.LocalDate veryLateDate = java.time.LocalDate.of(2100, 12, 31);

        return bookingRepository.findByDoctorIdAndDateRange(
                        doctorId, veryEarlyDate, veryLateDate, pageable)
                .map(bookingMapper::toSummaryResponse);
    }
}