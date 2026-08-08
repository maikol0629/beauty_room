package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.DTOS.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.DTOS.client.ClientResponseDto;
import com.mr.sb.beauty_room.DTOS.service.ServiceResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.Exceptions.AppointmentConflictException;
import com.mr.sb.beauty_room.Services.IAppointmentService;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.Services.IAvailabilityService;
import com.mr.sb.beauty_room.Services.IMessagingChannel;

import com.mr.sb.beauty_room.entities.*;
import com.mr.sb.beauty_room.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.mr.sb.beauty_room.Services.IBlockedSlotService;
import com.mr.sb.beauty_room.Services.INotificationService;

@Slf4j
@org.springframework.stereotype.Service
public class AppointmentServiceImplement implements IAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final StylistRepository stylistRepository;
    private final ServiceRepository serviceRepository;
    private final StylistScheduleRepository stylistScheduleRepository;
    private final IBlockedSlotService blockedSlotService;
    private final INotificationService notificationService;
    private final IMessagingChannel messagingChannel;
    private final IAvailabilityService availabilityService;

    @Autowired
    public AppointmentServiceImplement(AppointmentRepository appointmentRepository, ClientRepository clientRepository, StylistRepository stylistRepository, ServiceRepository serviceRepository, StylistScheduleRepository stylistScheduleRepository, IBlockedSlotService blockedSlotService, INotificationService notificationService, IMessagingChannel messagingChannel, IAvailabilityService availabilityService) {
        this.appointmentRepository = appointmentRepository;
        this.clientRepository = clientRepository;
        this.stylistRepository = stylistRepository;
        this.serviceRepository = serviceRepository;
        this.stylistScheduleRepository = stylistScheduleRepository;
        this.blockedSlotService = blockedSlotService;
        this.notificationService = notificationService;
        this.messagingChannel = messagingChannel;
        this.availabilityService = availabilityService;
    }

    @Override
    public List<AppointmentResponseDto> findAll() {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return StreamSupport.stream(appointmentRepository.findByTenantId(tenantId).spliterator(), false)
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AppointmentResponseDto> findById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return appointmentRepository.findById(id)
                .filter(a -> a.getTenant() != null && a.getTenant().getId().equals(tenantId))
                .map(this::toResponseDto);
    }

    @Override
    public List<AppointmentResponseDto> findAppointmentsByStylistID(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return appointmentRepository.findAppointmentsByStylistId(id, tenantId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<AppointmentResponseDto> findAppointmentsByClientID(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return appointmentRepository.findAppointmentsByClientId(id, tenantId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    @Override
    public boolean save(AppointmentSaveDto appointmentSaveDto) {

        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();

        Optional<Client> client = clientRepository.findByIdAndTenantId(appointmentSaveDto.getClientId(), tenantId);
        Optional<Stylist> stylist = stylistRepository.findByIdAndTenantId(appointmentSaveDto.getStylistId(), tenantId);
        Optional<Service> service = serviceRepository.findByIdAndTenantId(appointmentSaveDto.getServiceId(), tenantId);

        if (client.isPresent() && service.isPresent() && stylist.isPresent()) {
            List<Service> services = stylist.get().getServices();
            if (services.contains(service.get())) {

                Duration serviceDuration = Duration.ofMinutes(service.get().getDuration());
                LocalDateTime endDate = appointmentSaveDto.getStartDate().plus(serviceDuration);

                Appointment appointment = Appointment.builder()
                        .service(service.get())
                        .client(client.get())
                        .startDate(appointmentSaveDto.getStartDate())
                        .endDate(endDate)
                        .stylist(stylist.get())
                        .tenant(Tenant.builder().id(tenantId).build())
                        .build();

                if (!validateStylistAvailability(appointment) || !validateAppointmentTime(appointment)) {
                    throw new AppointmentConflictException(
                            "La franja horaria solicitada no está disponible para el estilista " + stylist.get().getId());
                }
                appointmentRepository.save(appointment);
                notifyStylistOfNewAppointment(stylist.get(), client.get(), service.get(), appointment);
                return true;
            }
        }
        return false;
    }

    @Transactional
    @Override
    public boolean update(AppointmentSaveDto appointmentSaveDto, long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(id)
                .filter(a -> a.getTenant() != null && a.getTenant().getId().equals(tenantId));
        Optional<Client> client = clientRepository.findByIdAndTenantId(appointmentSaveDto.getClientId(), tenantId);
        Optional<Service> service = serviceRepository.findByIdAndTenantId(appointmentSaveDto.getServiceId(), tenantId);
        Optional<Stylist> stylist = stylistRepository.findByIdAndTenantId(appointmentSaveDto.getStylistId(), tenantId);
        if (optionalAppointment.isPresent()&&client.isPresent()&&service.isPresent()&&stylist.isPresent()) {

            if(stylist.get().getServices().contains(service.get())) {
                LocalDateTime endDate = appointmentSaveDto.getStartDate().plusMinutes(service.get().getDuration());
                Appointment existing = optionalAppointment.get();
                Appointment receivedAppointment =  Appointment.builder()
                        .id(id)
                        .service(service.get())
                        .client(client.get())
                        .startDate(appointmentSaveDto.getStartDate())
                        .endDate(endDate)
                        .stylist(stylist.get())
                        .status(existing.getStatus())
                        .tenant(Tenant.builder().id(tenantId).build())
                        .build();
                if (existing.equals(receivedAppointment)) {
                    return false;
                }else if(this.validateStylistAvailability(receivedAppointment)
                &&this.validateAppointmentTime(receivedAppointment)) {
                    appointmentRepository.save(receivedAppointment);
                    return true;
                }

            }

        }
        return false;
    }

    @Override
    public boolean deleteById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(id)
                .filter(a -> a.getTenant() != null && a.getTenant().getId().equals(tenantId));

        if (optionalAppointment.isPresent()) {
            appointmentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public boolean confirmAppointment(long id) {
        Optional<Appointment> opt = findAppointmentForTenant(id);
        if (opt.isPresent()) {
            Appointment appointment = opt.get();
            if (appointment.getStatus() != AppointmentStatus.PENDING) {
                return false;
            }
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public boolean rejectAppointment(long id) {
        Optional<Appointment> opt = findAppointmentForTenant(id);
        if (opt.isPresent()) {
            Appointment appointment = opt.get();
            if (appointment.getStatus() != AppointmentStatus.PENDING) {
                return false;
            }
            appointment.setStatus(AppointmentStatus.REJECTED);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public boolean cancelAppointment(long id) {
        Optional<Appointment> opt = findAppointmentForTenant(id);
        if (opt.isPresent()) {
            Appointment appointment = opt.get();
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                return false;
            }
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public boolean completeAppointment(long id) {
        Optional<Appointment> opt = findAppointmentForTenant(id);
        if (opt.isPresent()) {
            Appointment appointment = opt.get();
            if (appointment.getStatus() != AppointmentStatus.PENDING
                    && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                return false;
            }
            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public boolean noShowAppointment(long id) {
        Optional<Appointment> opt = findAppointmentForTenant(id);
        if (opt.isPresent()) {
            Appointment appointment = opt.get();
            if (appointment.getStatus() != AppointmentStatus.PENDING
                    && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                return false;
            }
            appointment.setStatus(AppointmentStatus.NO_SHOW);
            appointmentRepository.save(appointment);
            return true;
        }
        return false;
    }

    @Transactional
    @Override
    public boolean cancelAppointmentByStylist(long id) {
        Optional<Appointment> opt = findAppointmentForTenant(id);
        if (opt.isPresent()) {
            Appointment appointment = opt.get();
            if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
                return false;
            }
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointmentRepository.save(appointment);
            notifyClientOfCancellation(appointment);
            return true;
        }
        return false;
    }

    private void notifyStylistOfNewAppointment(Stylist stylist, Client client, Service service, Appointment appointment) {
        if (stylist == null || stylist.getTelegram_chat_id() == null || stylist.getTelegram_chat_id().isBlank()) {
            return;
        }
        String clientName = (client != null && client.getName_client() != null) ? client.getName_client() : "Cliente";
        String serviceName = (service != null) ? service.getName_service() : "Servicio";
        messagingChannel.sendMessage(stylist.getTelegram_chat_id(),
                "📅 ¡Nueva cita agendada!\n\n"
                        + "• Cliente: " + clientName + "\n"
                        + "• Servicio: " + serviceName + "\n"
                        + "• Fecha: " + appointment.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n"
                        + "• Hora: " + appointment.getStartDate().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    private void notifyClientOfCancellation(Appointment appointment) {
        Client client = appointment.getClient();
        if (client == null || client.getTelegram_chat_id() == null || client.getTelegram_chat_id().isBlank()) {
            return;
        }
        String serviceName = (appointment.getService() != null) ? appointment.getService().getName_service() : "cita";
        String date = appointment.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        messagingChannel.sendMessage(client.getTelegram_chat_id(),
                "❌ Tu cita del " + date + " (" + serviceName + ") fue cancelada por el salón.");
    }

    private Optional<Appointment> findAppointmentForTenant(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return appointmentRepository.findById(id)
                .filter(a -> a.getTenant() != null && a.getTenant().getId().equals(tenantId));
    }

    @Override
    public List<AppointmentResponseDto> findAppointmentsByFilters(
            Long stylistId, Long clientId, AppointmentStatus status,
            LocalDateTime from, LocalDateTime to) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return appointmentRepository.findAppointmentsByFilters(tenantId, stylistId, clientId, status, from, to)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    private AppointmentResponseDto toResponseDto(Appointment appointment) {
        return AppointmentResponseDto.builder()
                .id(appointment.getId())
                .startDate(appointment.getStartDate())
                .endDate(appointment.getEndDate())
                .status(appointment.getStatus())
                .client(ClientResponseDto.builder()
                        .id(appointment.getClient().getId())
                        .name(appointment.getClient().getName_client())
                        .phone(appointment.getClient().getPhone())
                        .email(appointment.getClient().getEmail())
                        .build())
                .stylist(StylistResponseDto.builder()
                        .id(appointment.getStylist().getId())
                        .name(appointment.getStylist().getName_stylist())
                        .phone(appointment.getStylist().getPhone())
                        .email(appointment.getStylist().getEmail())
                        .build())
                .service(ServiceResponseDto.builder()
                        .id(appointment.getService().getId())
                        .name(appointment.getService().getName_service())
                        .description(appointment.getService().getDescription())
                        .price(appointment.getService().getPrice())
                        .duration(appointment.getService().getDuration())
                        .build())
                .build();
    }

    @Override
    @Transactional
    public List<LocalTime> getAvailableSlots(Long stylistId, Long serviceId, LocalDate date) {
        return availabilityService.getAvailableSlots(stylistId, serviceId, date);
    }

    private boolean validateAppointmentTime(Appointment appointment) {
        LocalDateTime now = LocalDateTime.now(); // 📌 Fecha y hora actual
        LocalDateTime startDateTime = appointment.getStartDate();
        LocalDateTime endDateTime = appointment.getEndDate();

        LocalTime startTime = startDateTime.toLocalTime();
        LocalTime endTime = endDateTime.toLocalTime();

        // 📌 Verificar que la fecha sea hoy o en el futuro
        boolean isFutureDate = !startDateTime.toLocalDate().isBefore(now.toLocalDate());

        // 📌 Verificar que la cita no sea en menos de una hora
        boolean isAtLeastOneHourAhead = startDateTime.isAfter(now.plusHours(1));

        // 📌 Verificar que la hora esté entre 07:00 y 22:00
        boolean isWithinBusinessHours = !startTime.isBefore(LocalTime.of(7, 0)) && !endTime.isAfter(LocalTime.of(22, 0));

        return isFutureDate && isAtLeastOneHourAhead && isWithinBusinessHours;
    }


    /** Validar si el estilista está disponible en ese horario */
    private boolean validateStylistAvailability(Appointment appointment) {
        Long stylistId = appointment.getStylist().getId();
        Long tenantId = appointment.getTenant() != null ? appointment.getTenant().getId() : null;
        if (tenantId == null) {
            return false;
        }
        LocalDateTime startDate = appointment.getStartDate();
        LocalDateTime endDate = appointment.getEndDate();
        DayOfWeek day = startDate.getDayOfWeek();
        LocalTime startTime = startDate.toLocalTime().truncatedTo(ChronoUnit.SECONDS);
        LocalTime endTime = endDate.toLocalTime().truncatedTo(ChronoUnit.SECONDS);

        // Obtener los horarios del estilista para ese día
        List<StylistSchedule> schedules = stylistScheduleRepository
                .findByStylistIdAndDayAndTenantId(stylistId, day, tenantId);

        // Validar si el estilista tiene un horario disponible que cubra la cita
        boolean isWithinSchedule = schedules.stream().anyMatch(schedule ->
                !startTime.isBefore(schedule.getStartTime()) &&  // startTime >= schedule.getStartTime()
                        !endTime.isAfter(schedule.getEndTime())         // endTime <= schedule.getEndTime()
        );

        if (!isWithinSchedule) {
            return false;
        }

        // Verificar que no haya otra cita en ese horario
        boolean hasConflict = appointmentRepository.existsOverlappingAppointment(stylistId, tenantId, startDate, endDate);

        // Verificar que el horario no esté bloqueado
        boolean blocked = blockedSlotService.isSlotBlocked(tenantId, stylistId, startDate, endDate);

        return !hasConflict && !blocked;
    }

}
