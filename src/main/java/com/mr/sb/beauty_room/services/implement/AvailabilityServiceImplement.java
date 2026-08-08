package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.services.IAvailabilityService;
import com.mr.sb.beauty_room.services.IBlockedSlotService;
import com.mr.sb.beauty_room.entities.Appointment;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.StylistSchedule;
import com.mr.sb.beauty_room.repository.AppointmentRepository;
import com.mr.sb.beauty_room.repository.SalonServiceRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.StylistScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImplement implements IAvailabilityService {

    private final StylistRepository stylistRepository;
    private final SalonServiceRepository serviceRepository;
    private final StylistScheduleRepository stylistScheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final IBlockedSlotService blockedSlotService;

    @Override
    @Transactional
    public List<LocalTime> getAvailableSlots(Long stylistId, Long serviceId, LocalDate date) {
        Optional<Stylist> stylistOpt = stylistRepository.findById(stylistId);
        if (stylistOpt.isEmpty()) {
            return List.of();
        }
        Long tenantId = stylistOpt.get().getTenant() != null ? stylistOpt.get().getTenant().getId() : null;
        if (tenantId == null) {
            return List.of();
        }
        Optional<SalonService> serviceOpt = serviceRepository.findByIdAndTenantId(serviceId, tenantId);
        if (serviceOpt.isEmpty()) {
            return List.of();
        }

        int durationMinutes = serviceOpt.get().getDuration();
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        List<StylistSchedule> schedules = stylistScheduleRepository
                .findByStylistIdAndDayAndTenantId(stylistId, dayOfWeek, tenantId);

        if (schedules.isEmpty()) {
            return List.of();
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        List<Appointment> existingAppointments = appointmentRepository
                .findByStylistIdAndStartDateBetweenAndStatusNotAndTenantId(
                        stylistId, dayStart, dayEnd, AppointmentStatus.CANCELLED, tenantId);

        List<LocalTime> availableSlots = new ArrayList<>();

        for (StylistSchedule schedule : schedules) {
            LocalTime slotStart = schedule.getStartTime();
            LocalTime slotEnd = schedule.getEndTime();

            while (!slotStart.plusMinutes(durationMinutes).isAfter(slotEnd)) {
                LocalDateTime candidateStart = date.atTime(slotStart);
                LocalDateTime candidateEnd = candidateStart.plusMinutes(durationMinutes);

                boolean overlaps = existingAppointments.stream().anyMatch(a ->
                        a.getStartDate().isBefore(candidateEnd) &&
                        a.getEndDate().isAfter(candidateStart));

                boolean blocked = blockedSlotService.isSlotBlocked(tenantId, stylistId, candidateStart, candidateEnd);

                if (!overlaps && !blocked) {
                    availableSlots.add(slotStart);
                }

                slotStart = slotStart.plusMinutes(30);
            }
        }

        return availableSlots;
    }
}
