package com.mr.sb.beauty_room.Controllers;


import com.mr.sb.beauty_room.DTOS.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.DTOS.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.Services.IAppointmentService;
import com.mr.sb.beauty_room.entities.AppointmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/appointment")
@Tag(name = "Appointments", description = "Gestión de citas (CRUD, disponibilidad, estados)")
public class AppointmentController {

    @Autowired
    private IAppointmentService appointmentService;


    @GetMapping("/findById/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> findAppointmentById(@PathVariable long id) {

        Optional<AppointmentResponseDto> OpAppointment = appointmentService.findById(id);

        if (OpAppointment.isPresent()) {
            AppointmentResponseDto appointmentResponseDto = OpAppointment.get();
            return ResponseEntity.ok(appointmentResponseDto);
        }
        return ResponseEntity.notFound().build();


    }


    @GetMapping("/findByStylistId/{id}")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<?> findAppointmentByStylistId(@PathVariable long id) {
        List<AppointmentResponseDto> optionalAppointments = appointmentService.findAppointmentsByStylistID(id);

        return ResponseEntity.ok(optionalAppointments);
    }

    @GetMapping("/findByClientId/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<?> findAppointmentByClientId(@PathVariable long id) {

        List<AppointmentResponseDto> optionalAppointments = appointmentService.findAppointmentsByClientID(id);

        return ResponseEntity.ok(optionalAppointments);
    }


    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAppointment(@PathVariable long id) {

        if(appointmentService.deleteById(id)){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }


    @PutMapping("update/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateAppointment(@PathVariable long id, @Valid @RequestBody AppointmentSaveDto appointmentSaveDto) {

        if(appointmentService.update(appointmentSaveDto, id)){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Crear cita", description = "Crea una cita validando disponibilidad. Devuelve 409 si la franja ya está ocupada o bloqueada")
    public ResponseEntity<?> saveAppointment(@Valid @RequestBody AppointmentSaveDto appointmentSaveDto) {

        if(appointmentService.save(appointmentSaveDto)){
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<?> confirmAppointment(@PathVariable long id) {
        if (appointmentService.confirmAppointment(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<?> rejectAppointment(@PathVariable long id) {
        if (appointmentService.rejectAppointment(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLIENT', 'STYLIST', 'ADMIN')")
    public ResponseEntity<?> cancelAppointment(@PathVariable long id) {
        if (appointmentService.cancelAppointment(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<?> completeAppointment(@PathVariable long id) {
        if (appointmentService.completeAppointment(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('STYLIST', 'ADMIN')")
    public ResponseEntity<List<AppointmentResponseDto>> getAppointmentHistory(
            @RequestParam(required = false) Long stylistId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(
                appointmentService.findAppointmentsByFilters(stylistId, clientId, status, from, to));
    }

    @GetMapping("/availability")
    @Operation(summary = "Franjas horarias disponibles",
            description = "Calcula los horarios libres de un estilista para un servicio en una fecha (endpoint público, requiere header X-Tenant-ID)")
    public ResponseEntity<?> getAvailability(
            @Parameter(description = "ID del estilista") @RequestParam Long stylistId,
            @Parameter(description = "ID del servicio (para la duración)") @RequestParam Long serviceId,
            @Parameter(description = "Fecha en formato YYYY-MM-DD") @RequestParam LocalDate date) {
        List<LocalTime> slots = appointmentService.getAvailableSlots(stylistId, serviceId, date);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/slots")
    @Operation(summary = "Franjas horarias disponibles (alias)",
            description = "Igual que /availability: horarios libres del estilista para un servicio en una fecha")
    public ResponseEntity<?> getAvailableSlots(
            @Parameter(description = "ID del estilista") @RequestParam Long stylistId,
            @Parameter(description = "ID del servicio (para la duración)") @RequestParam Long serviceId,
            @Parameter(description = "Fecha en formato YYYY-MM-DD") @RequestParam LocalDate date) {
        List<LocalTime> slots = appointmentService.getAvailableSlots(stylistId, serviceId, date);
        return ResponseEntity.ok(slots);
    }

}
