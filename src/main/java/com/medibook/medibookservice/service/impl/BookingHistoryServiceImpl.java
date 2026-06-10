package com.medibook.medibookservice.service.impl;

import com.medibook.medibookservice.dto.response.BookingHistoryResponse;
import com.medibook.medibookservice.entity.BookingHistory;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.enums.BookingHistoryAction;
import com.medibook.medibookservice.repository.BookingHistoryRepository;
import com.medibook.medibookservice.repository.UserRepository;
import com.medibook.medibookservice.service.BookingHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookingHistoryServiceImpl implements BookingHistoryService {

    private final BookingHistoryRepository bookingHistoryRepository;
    private final UserRepository userRepository;

    public BookingHistoryServiceImpl(BookingHistoryRepository bookingHistoryRepository,
                                     UserRepository userRepository) {
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.userRepository = userRepository;
    }

    // =========================================================
    // BOOKING TIMELINE
    // =========================================================

    @Override
    public List<BookingHistoryResponse> getBookingTimeline(Long bookingId) {
        List<BookingHistory> history = bookingHistoryRepository
                .findByBookingIdOrderByPerformedAtAsc(bookingId);
        return enrichWithUserEmails(history);
    }

    // =========================================================
    // USER ACTIVITY
    // =========================================================

    @Override
    public Page<BookingHistoryResponse> getUserActivity(Long userId, Pageable pageable) {
        Page<BookingHistory> historyPage = bookingHistoryRepository
                .findByPerformedByUserIdOrderByPerformedAtDesc(userId, pageable);

        // Enrich the content
        List<BookingHistoryResponse> enriched = enrichWithUserEmails(historyPage.getContent());

        // Return a new Page with enriched content
        return new org.springframework.data.domain.PageImpl<>(
                enriched, pageable, historyPage.getTotalElements());
    }

    // =========================================================
    // ANALYTICS
    // =========================================================

    @Override
    public Page<BookingHistoryResponse> getActionsInRange(
            BookingHistoryAction action,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable) {

        Page<BookingHistory> historyPage = bookingHistoryRepository
                .findByActionAndPerformedAtBetween(action, startDateTime, endDateTime, pageable);

        List<BookingHistoryResponse> enriched = enrichWithUserEmails(historyPage.getContent());

        return new org.springframework.data.domain.PageImpl<>(
                enriched, pageable, historyPage.getTotalElements());
    }

    @Override
    public long countActionsInRange(BookingHistoryAction action,
                                    LocalDateTime startDateTime,
                                    LocalDateTime endDateTime) {
        return bookingHistoryRepository.countByActionAndPerformedAtBetween(
                action, startDateTime, endDateTime);
    }

    @Override
    public long countActionsByDoctorInRange(BookingHistoryAction action,
                                            Long doctorId,
                                            LocalDateTime startDateTime,
                                            LocalDateTime endDateTime) {
        return bookingHistoryRepository.countByActionAndDoctorAndDateRange(
                action, doctorId, startDateTime, endDateTime);
    }

    // =========================================================
    // ENRICHMENT (the N+1 fix!)
    // =========================================================

    /**
     * Batch-load user emails for a list of history entries.
     * Solves the N+1 query problem by fetching all users in ONE query.
     */
    private List<BookingHistoryResponse> enrichWithUserEmails(List<BookingHistory> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return List.of();
        }

        // STEP 1: Collect all unique user IDs from the history entries
        Set<Long> userIds = historyList.stream()
                .map(BookingHistory::getPerformedByUserId)
                .collect(Collectors.toSet());

        // STEP 2: Fetch all users in ONE query
        List<User> users = userRepository.findAllById(userIds);

        // STEP 3: Build a Map<userId, email> for O(1) lookups
        Map<Long, String> userIdToEmail = new HashMap<>();
        for (User user : users) {
            userIdToEmail.put(user.getId(), user.getEmail());
        }

        // STEP 4: Map each history entry, using the prefetched email
        return historyList.stream()
                .map(history -> toResponseWithEmail(history, userIdToEmail))
                .collect(Collectors.toList());
    }

    /**
     * Convert one history entry, using prefetched email map.
     */
    private BookingHistoryResponse toResponseWithEmail(BookingHistory history,
                                                       Map<Long, String> userIdToEmail) {
        BookingHistoryResponse response = new BookingHistoryResponse();

        response.setId(history.getId());
        if (history.getBooking() != null) {
            response.setBookingId(history.getBooking().getId());
        }
        response.setOldStatus(history.getOldStatus());
        response.setNewStatus(history.getNewStatus());
        response.setAction(history.getAction());
        response.setPerformedByUserId(history.getPerformedByUserId());
        response.setPerformedByUserEmail(userIdToEmail.get(history.getPerformedByUserId()));
        response.setNotes(history.getNotes());
        response.setPerformedAt(history.getPerformedAt());

        return response;
    }
}