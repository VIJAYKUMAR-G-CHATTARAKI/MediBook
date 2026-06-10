package com.medibook.medibookservice.mapper;

import com.medibook.medibookservice.dto.request.HolidayRequest;
import com.medibook.medibookservice.dto.response.HolidayResponse;
import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorHoliday;
import org.springframework.stereotype.Component;

/**
 * Mapper for DoctorHoliday entity.
 */
@Component
public class HolidayMapper {

    /**
     * Create a new DoctorHoliday from request.
     */
    public DoctorHoliday toEntity(HolidayRequest request, Doctor doctor, Long createdByUserId) {
        if (request == null) return null;

        return new DoctorHoliday(
                doctor,
                request.getHolidayDate(),
                request.getReason(),
                createdByUserId
        );
    }

    /**
     * Convert DoctorHoliday entity to response DTO.
     */
    public HolidayResponse toResponse(DoctorHoliday holiday) {
        if (holiday == null) return null;

        HolidayResponse response = new HolidayResponse();
        response.setId(holiday.getId());

        if (holiday.getDoctor() != null) {
            response.setDoctorId(holiday.getDoctor().getId());
        }

        response.setHolidayDate(holiday.getHolidayDate());
        response.setReason(holiday.getReason());
        response.setCreatedByUserId(holiday.getCreatedByUserId());
        response.setCreatedAt(holiday.getCreatedAt());
        return response;
    }
}