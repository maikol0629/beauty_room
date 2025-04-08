package com.mr.sb.beauty_room.Controllers;


import com.mr.sb.beauty_room.DTOS.appointments.AppointmentResponseDto;
import com.mr.sb.beauty_room.DTOS.appointments.AppointmentSaveDto;
import com.mr.sb.beauty_room.Services.IAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/appointment")
public class AppointmentController {

    @Autowired
    private IAppointmentService appointmentService;


    @GetMapping("/findById/{id}")
    public ResponseEntity<?> findAppointmentById(@PathVariable long id) {

        Optional<AppointmentResponseDto> OpAppointment = appointmentService.findById(id);

        if (OpAppointment.isPresent()) {
            AppointmentResponseDto appointmentResponseDto = OpAppointment.get();
            return ResponseEntity.ok(appointmentResponseDto);
        }
        return ResponseEntity.notFound().build();


    }


    @GetMapping("/findByStylistId/{id}")
    public ResponseEntity<?> findAppointmentByStylistId(@PathVariable long id) {
        System.out.println(id);
        List<AppointmentResponseDto> optionalAppointments = appointmentService.findAppointmentsByStylistID(id);

        return ResponseEntity.ok(optionalAppointments);
    }

    @GetMapping("/findByClientId/{id}")
    public ResponseEntity<?> findAppointmentByClientId(@PathVariable long id) {

        List<AppointmentResponseDto> optionalAppointments = appointmentService.findAppointmentsByClientID(id);

        return ResponseEntity.ok(optionalAppointments);
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable long id) {

        if(appointmentService.deleteById(id)){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }


    @PutMapping("update/{id}")
    public ResponseEntity<?> updateAppointment(@PathVariable long id, @RequestBody AppointmentSaveDto appointmentSaveDto) {

        if(appointmentService.update(appointmentSaveDto, id)){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveAppointment(@RequestBody AppointmentSaveDto appointmentSaveDto) {

        if(appointmentSaveDto != null){
           if(appointmentService.save(appointmentSaveDto)){
               return ResponseEntity.ok().build();
           }

        }
        return ResponseEntity.badRequest().build();

    }


}
