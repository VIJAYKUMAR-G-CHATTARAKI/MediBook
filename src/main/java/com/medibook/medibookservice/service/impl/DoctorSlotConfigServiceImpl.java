package com.medibook.medibookservice.service.impl;

import com.medibook.medibookservice.dto.request.GenerateSlotsRequest;
import com.medibook.medibookservice.dto.request.SlotConfigRequest;
import com.medibook.medibookservice.dto.response.SlotConfigResponse;
import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorSlot;
import com.medibook.medibookservice.entity.DoctorSlotConfig;
import com.medibook.medibookservice.enums.DoctorStatus;
import com.medibook.medibookservice.exception.BadRequestException;
import com.medibook.medibookservice.exception.ResourceNotFoundException;
import com.medibook.medibookservice.mapper.SlotConfigMapper;
import com.medibook.medibookservice.repository.DoctorRepository;
import com.medibook.medibookservice.repository.DoctorSlotConfigRepository;
import com.medibook.medibookservice.repository.DoctorSlotRepository;
import com.medibook.medibookservice.service.DoctorSlotConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorSlotConfigServiceImpl implements DoctorSlotConfigService {

    private static final Logger log = LoggerFactory.getLogger(DoctorSlotConfigServiceImpl.class);

    private final DoctorRepository doctorRepository;
    private final DoctorSlotConfigRepository slotConfigRepository;
    private final DoctorSlotRepository slotRepository;
    private final SlotConfigMapper slotConfigMapper;
    private final SlotGenerationService slotGenerationService;

    public DoctorSlotConfigServiceImpl(DoctorRepository doctorRepository,
                                       DoctorSlotConfigRepository slotConfigRepository,
                                       DoctorSlotRepository slotRepository,
                                       SlotConfigMapper slotConfigMapper,
                                       SlotGenerationService slotGenerationService) {
        this.doctorRepository = doctorRepository;
        this.slotConfigRepository = slotConfigRepository;
        this.slotRepository = slotRepository;
        this.slotConfigMapper = slotConfigMapper;
        this.slotGenerationService = slotGenerationService;
    }

    // =========================================================
    // CREATE OR UPDATE CONFIG
    // =========================================================

    @Override
    @Transactional
    public SlotConfigResponse createOrUpdateConfig(Long doctorId, SlotConfigRequest request) {
        log.info("Create/update slot config for doctor {}", doctorId);

        Doctor doctor = findDoctorById(doctorId);

        // Validate doctor is APPROVED
        if (doctor.getStatus() != DoctorStatus.APPROVED) {
            throw new BadRequestException(
                    "Slot config can only be set for APPROVED doctors. Current status: "
                            + doctor.getStatus());
        }

        // Validate the weekly schedule data
        validateSchedules(request);

        DoctorSlotConfig config = slotConfigRepository.findByDoctor(doctor).orElse(null);

        if (config == null) {
            // Create new
            config = slotConfigMapper.toEntity(request, doctor);
            config = slotConfigRepository.save(config);
            log.info("Created new slot config {} for doctor {}", config.getId(), doctorId);
        } else {
            // Update existing
            slotConfigMapper.updateEntity(config, request);
            // JPA dirty checking saves automatically
            log.info("Updated slot config {} for doctor {}", config.getId(), doctorId);
        }

        return slotConfigMapper.toResponse(config);
    }

    // =========================================================
    // GET CONFIG
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public SlotConfigResponse getConfigByDoctorId(Long doctorId) {
        Doctor doctor = findDoctorById(doctorId);
        DoctorSlotConfig config = getConfigEntityForDoctor(doctor);
        return slotConfigMapper.toResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorSlotConfig getConfigEntityForDoctor(Doctor doctor) {
        return slotConfigRepository.findByDoctor(doctor)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot config not found for doctor: " + doctor.getId()));
    }

    // =========================================================
    // GENERATE SLOTS (the big one!)
    // =========================================================

    @Override
    @Transactional
    public int generateSlots(Long doctorId, GenerateSlotsRequest request) {
        log.info("Generating slots for doctor {} from {} to {}",
                doctorId, request.getStartDate(), request.getEndDate());

        Doctor doctor = findDoctorById(doctorId);

        // Doctor must be APPROVED to generate slots
        if (doctor.getStatus() != DoctorStatus.APPROVED) {
            throw new BadRequestException(
                    "Cannot generate slots for non-APPROVED doctor. Status: " + doctor.getStatus());
        }

        DoctorSlotConfig config = getConfigEntityForDoctor(doctor);

        // Config must be active
        if (!config.isActive()) {
            throw new BadRequestException(
                    "Cannot generate slots when slot config is inactive. " +
                            "Activate the config first.");
        }

        // Use SlotGenerationService to compute slots
        boolean skipExisting = request.getSkipExisting() == null
                ? true
                : request.getSkipExisting();

        List<DoctorSlot> generatedSlots = slotGenerationService.generateSlots(
                doctor,
                config,
                request.getStartDate(),
                request.getEndDate(),
                skipExisting
        );

        if (generatedSlots.isEmpty()) {
            log.warn("No slots generated for doctor {} - check schedule and holidays", doctorId);
            return 0;
        }

        // Bulk save - one transaction for all slots
        slotRepository.saveAll(generatedSlots);

        log.info("Persisted {} slots for doctor {}", generatedSlots.size(), doctorId);
        return generatedSlots.size();
    }

    // =========================================================
    // ACTIVATE/DEACTIVATE CONFIG
    // =========================================================

    @Override
    @Transactional
    public SlotConfigResponse setConfigActive(Long doctorId, boolean active) {
        log.info("Setting config active={} for doctor {}", active, doctorId);

        Doctor doctor = findDoctorById(doctorId);
        DoctorSlotConfig config = getConfigEntityForDoctor(doctor);

        config.setActive(active);
        // JPA dirty checking saves automatically

        return slotConfigMapper.toResponse(config);
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private Doctor findDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
    }

    /**
     * Validate the request's weekly schedule data.
     */
    private void validateSchedules(SlotConfigRequest request) {
        if (request.getWeeklySchedules() == null) {
            return;  // Mapper will create non-working defaults for all 7 days
        }

        for (var schedule : request.getWeeklySchedules()) {
            // If marked as working day, start/end times must be present
            if (Boolean.TRUE.equals(schedule.getIsWorkingDay())) {
                if (schedule.getWorkStartTime() == null || schedule.getWorkEndTime() == null) {
                    throw new BadRequestException(
                            "Working day " + schedule.getDayOfWeek()
                                    + " must have start and end times");
                }

                if (!schedule.getWorkStartTime().isBefore(schedule.getWorkEndTime())) {
                    throw new BadRequestException(
                            "Working day " + schedule.getDayOfWeek()
                                    + ": start time must be before end time");
                }
            } else {
                // Non-working day shouldn't have times set
                if (schedule.getWorkStartTime() != null || schedule.getWorkEndTime() != null) {
                    throw new BadRequestException(
                            "Non-working day " + schedule.getDayOfWeek()
                                    + " should not have start/end times");
                }
            }
        }
    }
}