package com.mr.sb.beauty_room.Controllers;


import com.mr.sb.beauty_room.DTOS.service.ServiceResponseDto;
import com.mr.sb.beauty_room.DTOS.service.ServiceSaveDto;
import com.mr.sb.beauty_room.Services.IServiceService;
import com.mr.sb.beauty_room.entities.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/service")
public class ServiceController {

    @Autowired
    private IServiceService serviceService;


    @GetMapping("/findAll")
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(serviceService.findAll());
    }


    @GetMapping("/find/{id}")
    public ResponseEntity<?> findById(@PathVariable long id){

        ServiceResponseDto serviceResponse= serviceService.findById(id);

        if(serviceResponse!=null){

            return ResponseEntity.ok(serviceResponse);
        }
        return ResponseEntity.notFound().build();

    }


    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody ServiceSaveDto serviceSaveDto)  {
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
    public ResponseEntity<?> update(@PathVariable long id, @RequestBody ServiceSaveDto serviceSaveDto){


        if(serviceService.update(serviceSaveDto,id)){

         return   ResponseEntity.ok().build();

        }

       else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/findByStylistId/{id}")
    public ResponseEntity<?> findByStylistId(@PathVariable long id){

        List<ServiceResponseDto> services = serviceService.findByStylystId(id);

        if(!services.isEmpty()){

            return ResponseEntity.ok(services);



        }

        return ResponseEntity.notFound().build();
    }








}
