package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.service.ServiceResponseDto;
import com.mr.sb.beauty_room.DTOS.service.ServiceSaveDto;
import com.mr.sb.beauty_room.Services.IServiceService;
import com.mr.sb.beauty_room.entities.Service;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.repository.ServiceRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@org.springframework.stereotype.Service
public class ServiceServiceImplement implements IServiceService {
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private StylistRepository stylistRepository;

    @Override
    public List<ServiceResponseDto> findAll() {

        Iterable<Service> services = serviceRepository.findAll();

        return StreamSupport.stream(services.spliterator(),false).map(
                service -> ServiceResponseDto.builder()
                        .idService(service.getId())
                        .price(service.getPrice())
                        .name(service.getName_service())
                        .description(service.getDescription())
                        .id_stylist(service.getStylist().getId())
                        .build()

        ).collect(Collectors.toList());
    }

    @Override
    public ServiceResponseDto findById(long id) {

        if(serviceRepository.findById(id).isPresent()){

            Service service = serviceRepository.findById(id).get();


            return ServiceResponseDto.builder()
                    .idService(service.getId())
                    .price(service.getPrice())
                    .name(service.getName_service())
                    .description(service.getDescription())
                    .id_stylist(service.getStylist().getId())
                    .build();
        }

        return null;
    }

    @Override
    public List<ServiceResponseDto> findByStylystId(Long id) {

        if(!serviceRepository.findServicesStylistId(id).isEmpty()){

            List<Service> services = serviceRepository.findServicesStylistId(id);

            return services.stream().map(
                    service -> ServiceResponseDto.builder()
                            .idService(service.getId())
                            .price(service.getPrice())
                            .name(service.getName_service())
                            .description(service.getDescription())
                            .id_stylist(service.getStylist().getId())
                            .build()
            ).collect(Collectors.toList());

        }
        return null;
    }

    @Transactional
    @Override
    public void save(ServiceSaveDto serviceSaveDto) {

        Optional<Stylist> optionalStylist = stylistRepository.findById(serviceSaveDto.getId_stylist());

        if (optionalStylist.isPresent()) {
            Service service = Service.builder()
                    .name_service(serviceSaveDto.getName())
                    .description(serviceSaveDto.getDescription())
                    .stylist(optionalStylist.get())
                    .price(serviceSaveDto.getPrice())
                    .duration((int) serviceSaveDto.getDuration())
                    .build();
            serviceRepository.save(service);
        }
    }





    @Override
    public boolean deleteById(long id) {

        Optional<Service> service = serviceRepository.findById(id);

        if(service.isPresent()){
            serviceRepository.deleteById(id);
            return true;
        }else {
            return false;
        }
    }

    @Override
    public boolean update(ServiceSaveDto serviceSaveDto, long id) {
        Optional<Service> service = serviceRepository.findById(id);
        if(service.isPresent()){

            serviceRepository.save(
                    Service.builder()
                            .duration((int)(serviceSaveDto.getDuration()))
                            .price(serviceSaveDto.getPrice())
                            .name_service(serviceSaveDto.getName())
                            .description(serviceSaveDto.getDescription())
                            .build()

            );
            return true;

        }
        return false;


    }

    @Override
    public boolean isExistService(long id) {
        return serviceRepository.findById(id).isPresent();
    }

    @Override
    public boolean serviceBelongToStylyst(long stylystId, long serviceId) {
        List<Service> list = serviceRepository.findServicesStylistId(stylystId);
        boolean flag;
        flag = list.contains(serviceRepository.findById(serviceId).get());
        return flag;
    }
}
