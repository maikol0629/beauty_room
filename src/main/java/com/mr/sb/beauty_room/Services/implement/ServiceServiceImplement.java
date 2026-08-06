package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.service.ServiceResponseDto;
import com.mr.sb.beauty_room.DTOS.service.ServiceSaveDto;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.Services.IServiceService;
import com.mr.sb.beauty_room.entities.Service;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.ServiceRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceServiceImplement implements IServiceService {
    private final ServiceRepository serviceRepository;
    private final StylistRepository stylistRepository;

    @Override
    public List<ServiceResponseDto> findAll() {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Iterable<Service> services = serviceRepository.findByTenantId(tenantId);

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
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Service> optService = serviceRepository.findByIdAndTenantId(id, tenantId);

        if (optService.isPresent()) {
            Service service = optService.get();

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
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        List<Service> services = serviceRepository.findServicesStylistId(id, tenantId);
        if (services.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return services.stream()
                .map(service -> ServiceResponseDto.builder()
                        .idService(service.getId())
                        .price(service.getPrice())
                        .name(service.getName_service())
                        .description(service.getDescription())
                        .id_stylist(service.getStylist().getId())
                        .build()
                )
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void save(ServiceSaveDto serviceSaveDto) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();

        Long stylistId = Objects.requireNonNull(
                serviceSaveDto.getId_stylist(),
                "Stylist id is required"
        );

        Stylist stylist = stylistRepository.findByIdAndTenantId(stylistId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stylist not found for id: " + stylistId
                ));

        Service service = Service.builder()
                .name_service(serviceSaveDto.getName())
                .description(serviceSaveDto.getDescription())
                .stylist(stylist)
                .price(serviceSaveDto.getPrice())
                .duration((int) serviceSaveDto.getDuration())
                .tenant(Tenant.builder().id(tenantId).build())
                .build();

        serviceRepository.save(service);
    }





    @Override
    public boolean deleteById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Service> service = serviceRepository.findByIdAndTenantId(id, tenantId);

        if(service.isPresent()){
            serviceRepository.deleteById(id);
            return true;
        }else {
            return false;
        }
    }

    @Override
    public boolean update(ServiceSaveDto serviceSaveDto, long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Service> optionalService = serviceRepository.findByIdAndTenantId(id, tenantId);
        if (optionalService.isPresent()) {
            Service service = optionalService.get();
            service.setDuration((int) serviceSaveDto.getDuration());
            service.setPrice(serviceSaveDto.getPrice());
            service.setName_service(serviceSaveDto.getName());
            service.setDescription(serviceSaveDto.getDescription());
            serviceRepository.save(service);
            return true;
        }
        return false;
    }

    @Override
    public boolean isExistService(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return serviceRepository.findByIdAndTenantId(id, tenantId).isPresent();
    }

    @Override
    public boolean serviceBelongToStylyst(long stylystId, long serviceId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        List<Service> list = serviceRepository.findServicesStylistId(stylystId, tenantId);
        boolean flag;
        flag = list.contains(serviceRepository.findByIdAndTenantId(serviceId, tenantId).get());
        return flag;
    }
}
