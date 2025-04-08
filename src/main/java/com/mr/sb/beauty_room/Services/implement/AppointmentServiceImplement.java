package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.DTOS.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.DTOS.client.ClientResponseDto;
import com.mr.sb.beauty_room.DTOS.service.ServiceResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.Services.IAppointmentService;

import com.mr.sb.beauty_room.entities.*;
import com.mr.sb.beauty_room.repository.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@org.springframework.stereotype.Service
public class AppointmentServiceImplement implements IAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final StylistRepository stylistRepository;
    private final ServiceRepository serviceRepository;
    private final StylistScheduleRepository stylistScheduleRepository;
    
    @Autowired
    public AppointmentServiceImplement(AppointmentRepository appointmentRepository, ClientRepository clientRepository, StylistRepository stylistRepository, ServiceRepository serviceRepository, StylistScheduleRepository stylistScheduleRepository) {
        this.appointmentRepository = appointmentRepository;
        this.clientRepository = clientRepository;
        this.stylistRepository = stylistRepository;
        this.serviceRepository = serviceRepository;
        this.stylistScheduleRepository = stylistScheduleRepository;
    }

    @Override
    public List<AppointmentResponseDto> findAll() {
        Iterable<Appointment> iterable = appointmentRepository.findAll();
        return StreamSupport.stream(iterable.spliterator(), false).map(
                appointment -> AppointmentResponseDto.builder()
                        .client(ClientResponseDto.builder()
                                .name(appointment.getClient().getName_client())
                                .phone(appointment.getClient().getPhone())
                                .build())
                        .startDate(appointment.getStartDate())
                        .endDate(appointment.getEndDate())
                        .stylist(StylistResponseDto.builder()
                                .name(appointment.getStylist().getName_stylist())
                        .phone(appointment.getStylist().getPhone())
                        .build())
                        .build()
        ).collect(Collectors.toList());
    }

    @Override
    public Optional<AppointmentResponseDto> findById(long id) {

        Optional<Appointment> optionalAppointment = appointmentRepository.findById(id);

        if (optionalAppointment.isPresent()) {
            Appointment appointment = optionalAppointment.get();
            AppointmentResponseDto appointmentResponseDto = AppointmentResponseDto.builder()
                    .stylist(StylistResponseDto.builder()
                            .name(appointment.getStylist().getName_stylist())
                            .phone(appointment.getStylist().getPhone())
                            .build())
                    .client(
                            ClientResponseDto.builder()
                                    .name(appointment.getClient().getName_client())
                                    .phone(appointment.getClient().getPhone())
                                    .build())
                    .service(
                            ServiceResponseDto.builder()
                                    .price(appointment.getService().getPrice())
                                    .name(appointment.getService().getName_service())
                                    .description(appointment.getService().getDescription())
                                    .duration(appointment.getService().getDuration())
                                    .build())
                    .startDate(appointment.getStartDate())
                    .endDate(appointment.getEndDate())
                    .id(appointment.getId())
                    .build();
            return Optional.of(appointmentResponseDto);
        }
        return Optional.empty();
    }



    @Override
    public List<AppointmentResponseDto> findAppointmentsByStylistID(long id) {
        return appointmentRepository.findAppointmentsByStylistId(id).stream()
                .map(appointment -> AppointmentResponseDto.builder()
                        .startDate(appointment.getStartDate())
                        .endDate(appointment.getEndDate())
                        .service(ServiceResponseDto.builder()
                                .price(appointment.getService().getPrice())
                                .name(appointment.getService().getName_service())
                                .description(appointment.getService().getDescription())
                                .duration(appointment.getService().getDuration())
                                .build())
                        .client(ClientResponseDto.builder()
                                .name(appointment.getClient().getName_client())
                                .phone(appointment.getClient().getPhone())
                                .build())
                        .id(appointment.getId())
                        .build())
                .toList();
    }

    @Override
    public List<AppointmentResponseDto> findAppointmentsByClientID(long id) {
        return appointmentRepository.findAppointmentsByClientId(id).stream()
                .map(appointment -> AppointmentResponseDto.builder()
                        .startDate(appointment.getStartDate())
                        .endDate(appointment.getEndDate())
                        .stylist(StylistResponseDto.builder()
                                .name(appointment.getStylist().getName_stylist())
                                .phone(appointment.getStylist().getPhone())
                                .email(appointment.getStylist().getEmail())
                                .id(appointment.getStylist().getId())
                                .build())
                        .service(ServiceResponseDto.builder()
                                .price(appointment.getService().getPrice())
                                .name(appointment.getService().getName_service())
                                .description(appointment.getService().getDescription())
                                .duration(appointment.getService().getDuration())
                                .build())
                        .id(appointment.getId())
                        .build())
                .toList();
    }

    @Transactional
    @Override
    public boolean save(AppointmentSaveDto appointmentSaveDto) {

        Optional<Client> client = clientRepository.findById(appointmentSaveDto.getId_client());
        Optional<Stylist> stylist = stylistRepository.findById(appointmentSaveDto.getId_stylist());
        Optional<Service> service = serviceRepository.findById(appointmentSaveDto.getId_service());

        if (client.isPresent() && service.isPresent() && stylist.isPresent()) {
            List<Service> services = stylist.get().getServices();
            if (services.contains(service.get())) {

                Duration serviceDuration = Duration.ofMinutes(service.get().getDuration());
                System.out.println(serviceDuration);// Asegurar que devuelve Duration
                LocalDateTime endDate = appointmentSaveDto.getStartDate().plus(serviceDuration);

                Appointment appointment = Appointment.builder()
                        .service(service.get())
                        .client(client.get())
                        .startDate(appointmentSaveDto.getStartDate())
                        .endDate(endDate)
                        .stylist(stylist.get())
                        .build();

                if (validateStylistAvailability(appointment) && validateAppointmentTime(appointment)) {
                    appointmentRepository.save(appointment);
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional
    @Override
    public boolean update(AppointmentSaveDto appointmentSaveDto, long id) {
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(id);
        Optional<Client> client = clientRepository.findById(appointmentSaveDto.getId_client());
        Optional<Service> service = serviceRepository.findById(appointmentSaveDto.getId_service());
        Optional<Stylist> stylist = stylistRepository.findById(appointmentSaveDto.getId_stylist());
        if (optionalAppointment.isPresent()&&client.isPresent()&&service.isPresent()&&stylist.isPresent()) {

            if(stylist.get().getServices().contains(service.get())) {
                LocalDateTime endDate = appointmentSaveDto.getStartDate().plusMinutes(service.get().getDuration());
                Appointment appointment = optionalAppointment.get();
                Appointment receivedAppointment =  Appointment.builder()
                        .id(id)
                        .service(service.get())
                        .client(client.get())
                        .startDate(appointmentSaveDto.getStartDate())
                        .endDate(endDate)
                        .stylist(stylist.get())
                        .build();
                if (appointment.equals(receivedAppointment)) {
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

        Optional<Appointment> optionalAppointment = appointmentRepository.findById(id);

        if (optionalAppointment.isPresent()) {
            appointmentRepository.deleteById(id);
            return true;
        }
        return false;
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
        LocalDateTime startDate = appointment.getStartDate();
        LocalDateTime endDate = appointment.getEndDate();
        DayOfWeek day = startDate.getDayOfWeek();
        LocalTime startTime = startDate.toLocalTime().truncatedTo(ChronoUnit.SECONDS);
        LocalTime endTime = endDate.toLocalTime().truncatedTo(ChronoUnit.SECONDS);

        // Obtener los horarios del estilista para ese día
        List<StylistSchedule> schedules = stylistScheduleRepository.findByStylistIdAndDay(stylistId, day);

        // Validar si el estilista tiene un horario disponible que cubra la cita
        boolean isWithinSchedule = schedules.stream().anyMatch(schedule ->
                !startTime.isBefore(schedule.getStartTime()) &&  // startTime >= schedule.getStartTime()
                        !endTime.isAfter(schedule.getEndTime())         // endTime <= schedule.getEndTime()
        );

        if (!isWithinSchedule) {
           System.out.println("Estilista no disponible en este horario.");
            return false;
        }

        // Verificar que no haya otra cita en ese horario
        boolean hasConflict = appointmentRepository.existsOverlappingAppointment(stylistId, startDate, endDate);

        if (hasConflict) {
            System.out.println("El estilista ya tiene una cita en este horario.");
        }

        return !hasConflict;
    }

}