package com.medibook.medibookservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medibook.medibookservice.enums.BookingHistoryAction;
import com.medibook.medibookservice.enums.BookingStatus;

import java.time.LocalDateTime;

/**
 * Single audit trail entry response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingHistoryResponse {

    private Long id;
    private Long bookingId;
    private BookingStatus oldStatus;
    private BookingStatus newStatus;
    private BookingHistoryAction action;
    private Long performedByUserId;
    private String performedByUserEmail;  // Joined from user table
    private String notes;
    private LocalDateTime performedAt;

    public BookingHistoryResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public BookingStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(BookingStatus oldStatus) { this.oldStatus = oldStatus; }

    public BookingStatus getNewStatus() { return newStatus; }
    public void setNewStatus(BookingStatus newStatus) { this.newStatus = newStatus; }

    public BookingHistoryAction getAction() { return action; }
    public void setAction(BookingHistoryAction action) { this.action = action; }

    public Long getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(Long performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public String getPerformedByUserEmail() { return performedByUserEmail; }
    public void setPerformedByUserEmail(String performedByUserEmail) {
        this.performedByUserEmail = performedByUserEmail;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
}