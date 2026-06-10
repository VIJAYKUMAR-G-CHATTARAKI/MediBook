package com.medibook.medibookservice.mapper;

import com.medibook.medibookservice.dto.response.BookingHistoryResponse;
import com.medibook.medibookservice.entity.BookingHistory;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Mapper for BookingHistory entity to response DTO.
 *
 * <p>Enriches the response with the performer's email by joining with User table.</p>
 */
@Component
public class BookingHistoryMapper {

    private final UserRepository userRepository;

    public BookingHistoryMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Convert BookingHistory entity to response DTO.
     * Enriches with performer email via User lookup.
     */
    public BookingHistoryResponse toResponse(BookingHistory history) {
        if (history == null) return null;

        BookingHistoryResponse response = new BookingHistoryResponse();
        response.setId(history.getId());

        if (history.getBooking() != null) {
            response.setBookingId(history.getBooking().getId());
        }

        response.setOldStatus(history.getOldStatus());
        response.setNewStatus(history.getNewStatus());
        response.setAction(history.getAction());
        response.setPerformedByUserId(history.getPerformedByUserId());
        response.setNotes(history.getNotes());
        response.setPerformedAt(history.getPerformedAt());

        // Enrich with performer's email (for audit display)
        Long performedByUserId = history.getPerformedByUserId();
        if (performedByUserId != null) {
            userRepository.findById(performedByUserId)
                    .map(User::getEmail)
                    .ifPresent(response::setPerformedByUserEmail);
        }

        return response;
    }
}