package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.service.ServiceResponseDto;
import com.mr.sb.beauty_room.DTOS.service.ServiceSaveDto;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.entities.Service;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.repository.ServiceRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceImplementTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private StylistRepository stylistRepository;

    @InjectMocks
    private ServiceServiceImplement serviceService;

    @BeforeEach
    void setUp() {
        TenantInterceptor.setCurrentTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantInterceptor.clear();
    }

    @Test
    void save_shouldPersistServiceWhenStylistExists() {
        Stylist stylist = new Stylist();
        stylist.setId(1L);

        ServiceSaveDto dto = ServiceSaveDto.builder()
                .name("Corte")
                .description("Corte basico")
                .price(15000)
                .duration(30)
                .stylistId(stylist.getId())
                .build();

        when(stylistRepository.findByIdAndTenantId(stylist.getId(), TENANT_ID)).thenReturn(Optional.of(stylist));

        serviceService.save(dto);

        ArgumentCaptor<Service> captor = ArgumentCaptor.forClass(Service.class);
        verify(serviceRepository).save(captor.capture());
        Service saved = captor.getValue();
        assertThat(saved.getName_service()).isEqualTo(dto.getName());
        assertThat(saved.getDescription()).isEqualTo(dto.getDescription());
        assertThat(saved.getPrice()).isEqualTo(dto.getPrice());
        assertThat(saved.getDuration()).isEqualTo(dto.getDuration());
        assertThat(saved.getStylist()).isEqualTo(stylist);
    }

    @Test
    void save_shouldThrowWhenStylistNotFound() {
        ServiceSaveDto dto = ServiceSaveDto.builder()
                .name("Corte")
                .description("Corte basico")
                .price(15000)
                .duration(30)
                .stylistId(99L)
                .build();

        when(stylistRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceService.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stylist not found for id");
    }

    @Test
    void findByStylistId_shouldMapEntitiesToDtos() {
        Stylist stylist = new Stylist();
        stylist.setId(1L);

        Service service = Service.builder()
                .id(10L)
                .name_service("Corte")
                .description("Corte basico")
                .price(15000)
                .duration(30)
                .stylist(stylist)
                .build();

        when(serviceRepository.findServicesStylistId(stylist.getId(), TENANT_ID))
                .thenReturn(List.of(service));

        List<ServiceResponseDto> result = serviceService.findByStylystId(stylist.getId());

        assertThat(result).hasSize(1);
        ServiceResponseDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(service.getId());
        assertThat(dto.getName()).isEqualTo(service.getName_service());
        assertThat(dto.getDescription()).isEqualTo(service.getDescription());
        assertThat(dto.getPrice()).isEqualTo(service.getPrice());
        assertThat(dto.getStylistId()).isEqualTo(stylist.getId());
    }

    @Test
    void findAll_shouldThrowWhenNoTenantContext() {
        TenantInterceptor.clear();
        assertThatThrownBy(() -> serviceService.findAll())
                .isInstanceOf(com.mr.sb.beauty_room.Exceptions.TenantNotResolvedException.class);
    }
}
