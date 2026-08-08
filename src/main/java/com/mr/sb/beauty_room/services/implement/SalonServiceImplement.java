package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.service.SalonServiceResponseDto;
import com.mr.sb.beauty_room.dto.service.SalonServiceSaveDto;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.services.ISalonService;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.SalonServiceRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class SalonServiceImplement implements ISalonService {
    private final SalonServiceRepository serviceRepository;
    private final StylistRepository stylistRepository;

    @Override
    public List<SalonServiceResponseDto> findAll() {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Iterable<SalonService> services = serviceRepository.findByTenantId(tenantId);

        return StreamSupport.stream(services.spliterator(),false).map(
                service -> SalonServiceResponseDto.builder()
                        .id(service.getId())
                        .price(service.getPrice())
                        .name(service.getNameService())
                        .description(service.getDescription())
                        .stylistId(service.getStylist().getId())
                        .build()

        ).collect(Collectors.toList());
    }

    @Override
    public SalonServiceResponseDto findById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<SalonService> optService = serviceRepository.findByIdAndTenantId(id, tenantId);

        if (optService.isPresent()) {
            SalonService service = optService.get();

            return SalonServiceResponseDto.builder()
                    .id(service.getId())
                    .price(service.getPrice())
                    .name(service.getNameService())
                    .description(service.getDescription())
                    .stylistId(service.getStylist().getId())
                    .build();
        }

        return null;
    }

    @Override
    public List<SalonServiceResponseDto> findByStylistId(Long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        List<SalonService> services = serviceRepository.findByStylistIdAndTenantId(id, tenantId);
        if (services.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return services.stream()
                .map(service -> SalonServiceResponseDto.builder()
                        .id(service.getId())
                        .price(service.getPrice())
                        .name(service.getNameService())
                        .description(service.getDescription())
                        .stylistId(service.getStylist().getId())
                        .build()
                )
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void save(SalonServiceSaveDto serviceSaveDto) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();

        Long stylistId = Objects.requireNonNull(
                serviceSaveDto.getStylistId(),
                "Stylist id is required"
        );

        Stylist stylist = stylistRepository.findByIdAndTenantId(stylistId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Stylist not found for id: " + stylistId
                ));

        SalonService service = SalonService.builder()
                .nameService(serviceSaveDto.getName())
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
        Optional<SalonService> service = serviceRepository.findByIdAndTenantId(id, tenantId);

        if(service.isPresent()){
            serviceRepository.deleteById(id);
            return true;
        }else {
            return false;
        }
    }

    @Override
    public boolean update(SalonServiceSaveDto serviceSaveDto, long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<SalonService> optionalService = serviceRepository.findByIdAndTenantId(id, tenantId);
        if (optionalService.isPresent()) {
            SalonService service = optionalService.get();
            service.setDuration((int) serviceSaveDto.getDuration());
            service.setPrice(serviceSaveDto.getPrice());
            service.setNameService(serviceSaveDto.getName());
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
    public boolean serviceBelongsToStylist(long stylistId, long serviceId) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        List<SalonService> list = serviceRepository.findByStylistIdAndTenantId(stylistId, tenantId);
        boolean flag;
        flag = list.contains(serviceRepository.findByIdAndTenantId(serviceId, tenantId).get());
        return flag;
    }
}
