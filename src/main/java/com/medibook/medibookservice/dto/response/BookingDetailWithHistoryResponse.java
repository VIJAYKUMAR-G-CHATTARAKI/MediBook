
package com.medibook.medibookservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Booking detail response that includes the complete history timeline.
 * Used by the booking detail page to show: booking info + audit trail.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingDetailWithHistoryResponse {

    private BookingResponse booking;
    private List<BookingHistoryResponse> history;

    public BookingDetailWithHistoryResponse() {}

    public BookingDetailWithHistoryResponse(BookingResponse booking,
                                            List<BookingHistoryResponse> history) {
        this.booking = booking;
        this.history = history;
    }

    public BookingResponse getBooking() { return booking; }
    public void setBooking(BookingResponse booking) { this.booking = booking; }

    public List<BookingHistoryResponse> getHistory() { return history; }
    public void setHistory(List<BookingHistoryResponse> history) { this.history = history; }
}