package com.mr.sb.beauty_room.controllers;


import com.mr.sb.beauty_room.dto.service.SalonServiceResponseDto;
import com.mr.sb.beauty_room.dto.service.SalonServiceSaveDto;
import com.mr.sb.beauty_room.services.ISalonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import java.util.List;

@RestController
@RequestMapping("/api/service")
@Tag(name = "Services", description = "Gestión de servicios")
@RequiredArgsConstructor
public class SalonServiceController {

    private final ISalonService serviceService;

    @GetMapping("/public")
    @Operation(summary = "Listar servicios (público)",
            description = "Lista los servicios del tenant. Requiere header X-Tenant-ID (sin JWT)")
    public ResponseEntity<?> findAllPublic() {
        return ResponseEntity.ok(serviceService.findAll());
    }

    @GetMapping("/findAll")
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(serviceService.findAll());
    }


    @GetMapping("/find/{id}")
    public ResponseEntity<?> findById(@PathVariable long id){

        SalonServiceResponseDto serviceResponse= serviceService.findById(id);

        if(serviceResponse!=null){

            return ResponseEntity.ok(serviceResponse);
        }
        return ResponseEntity.notFound().build();

    }


    @PostMapping("/save")
    public ResponseEntity<?> save(@Valid @RequestBody SalonServiceSaveDto serviceSaveDto)  {
        serviceService.save(serviceSaveDto);
        return ResponseEntity.created(URI.create("/api/service/save")).build();
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable long id){

        if(serviceService.deleteById(id)){

            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();

    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable long id, @Valid @RequestBody SalonServiceSaveDto serviceSaveDto){


        if(serviceService.update(serviceSaveDto,id)){

         return   ResponseEntity.ok().build();

        }

       else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/findByStylistId/{id}")
    public ResponseEntity<?> findByStylistId(@PathVariable long id){

        List<SalonServiceResponseDto> services = serviceService.findByStylistId(id);

        if(!services.isEmpty()){

            return ResponseEntity.ok(services);



        }

        return ResponseEntity.notFound().build();
    }








}
