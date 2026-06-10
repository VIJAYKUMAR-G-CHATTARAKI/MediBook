package com.medibook.medibookservice.service;

import com.medibook.medibookservice.dto.request.GenerateSlotsRequest;
import com.medibook.medibookservice.dto.request.SlotConfigRequest;
import com.medibook.medibookservice.dto.response.SlotConfigResponse;
import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorSlotConfig;

/**
 * Service for managing doctor slot configurations (admin operations).
 *
 * <p>Admin uses this to:</p>
 * <ul>
 *   <li>Set up a doctor's working pattern (hours, slot duration, etc.)</li>
 *   <li>View existing configs</li>
 *   <li>Trigger slot generation for a date range</li>
 *   <li>Activate/deactivate configs</li>
 * </ul>
 */
public interface DoctorSlotConfigService {

    /**
     * Create or update the slot configuration for a doctor.
     * If the doctor has an existing config, it's updated.
     * Otherwise, a new config is created.
     *
     * @param doctorId  ID of the doctor
     * @param request   Configuration details
     * @return Created/updated config response
     */
    SlotConfigResponse createOrUpdateConfig(Long doctorId, SlotConfigRequest request);

    /**
     * Get the slot configuration for a specific doctor.
     *
     * @throws ResourceNotFoundException if doctor or config not found
     */
    SlotConfigResponse getConfigByDoctorId(Long doctorId);

    /**
     * Internal helper - returns the config entity (used by other services).
     *
     * @throws ResourceNotFoundException if config not found
     */
    DoctorSlotConfig getConfigEntityForDoctor(Doctor doctor);

    /**
     * Generate slots for a doctor over a date range.
     * Uses the doctor's existing slot configuration.
     *
     * @return Number of slots successfully generated
     */
    int generateSlots(Long doctorId, GenerateSlotsRequest request);

    /**
     * Activate or deactivate a doctor's slot config.
     */
    SlotConfigResponse setConfigActive(Long doctorId, boolean active);
}