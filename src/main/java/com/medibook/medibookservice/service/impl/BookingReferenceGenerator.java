package com.medibook.medibookservice.service.impl;

import com.medibook.medibookservice.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates human-readable booking references in format: DR{doctorId}-{yyyyMMdd}-{sequence}
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>DR5-20261209-001 (first booking for Doctor 5 on Dec 9, 2026)</li>
 *   <li>DR5-20261209-042 (42nd booking same doctor, same date)</li>
 * </ul>
 *
 * <p><b>Concurrency:</b> Under simultaneous bookings for the same (doctor, date),
 * two threads might compute the same reference. The DB unique constraint catches this,
 * and our retry logic tries the next sequence number.</p>
 */
@Service
public class BookingReferenceGenerator {

    private static final Logger log = LoggerFactory.getLogger(BookingReferenceGenerator.class);

    /** Format for the date portion: 20261209 */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    /** Format for the sequence portion: 001, 002, ..., 999 */
    private static final String SEQUENCE_FORMAT = "%03d";

    /** Maximum bookings per (doctor, date) before format breaks. */
    private static final int MAX_SEQUENCE = 999;

    /** Max retries when reference collisions occur. */
    private static final int MAX_RETRIES = 5;

    private final BookingRepository bookingRepository;

    public BookingReferenceGenerator(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // =========================================================
    // MAIN ENTRY POINT
    // =========================================================

    /**
     * Generate a unique booking reference for the given doctor and slot date.
     *
     * <p>Strategy: count existing bookings, format as DRx-yyyyMMdd-NNN.
     * If a collision happens (concurrent insert), retry with the next sequence.</p>
     *
     * @return Unique reference string like "DR5-20261209-001"
     * @throws IllegalStateException if max sequence (999) exceeded or max retries hit
     */
    public String generate(Long doctorId, LocalDate slotDate) {
        if (doctorId == null || slotDate == null) {
            throw new IllegalArgumentException("doctorId and slotDate must not be null");
        }

        String dateString = slotDate.format(DATE_FORMAT);

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            // Count existing bookings for this (doctor, date)
            long existingCount = bookingRepository.countByDoctorIdAndSlotDate(doctorId, slotDate);

            // Next sequence = count + 1 + attempt (in case of retry due to collision)
            long sequence = existingCount + 1 + attempt;

            if (sequence > MAX_SEQUENCE) {
                throw new IllegalStateException(
                        "Booking reference sequence exceeded maximum (" + MAX_SEQUENCE +
                                ") for doctor " + doctorId + " on " + slotDate);
            }

            String candidateReference = buildReference(doctorId, dateString, sequence);

            // Check if it already exists (could happen under concurrent inserts)
            if (!bookingRepository.existsByBookingReference(candidateReference)) {
                log.debug("Generated booking reference: {}", candidateReference);
                return candidateReference;
            }

            log.warn("Booking reference collision on attempt {}: {} - retrying",
                    attempt + 1, candidateReference);
        }

        throw new IllegalStateException(
                "Failed to generate unique booking reference after " + MAX_RETRIES +
                        " attempts for doctor " + doctorId + " on " + slotDate);
    }

    // =========================================================
    // FORMATTING (private helper)
    // =========================================================

    /**
     * Build the reference string: DR{doctorId}-{dateString}-{NNN}
     */
    private String buildReference(Long doctorId, String dateString, long sequence) {
        String sequenceFormatted = String.format(SEQUENCE_FORMAT, sequence);
        return "DR" + doctorId + "-" + dateString + "-" + sequenceFormatted;
    }
}