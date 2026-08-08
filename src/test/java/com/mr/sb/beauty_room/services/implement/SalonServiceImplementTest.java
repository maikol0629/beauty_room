package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.service.SalonServiceResponseDto;
import com.mr.sb.beauty_room.dto.service.SalonServiceSaveDto;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.entities.SalonService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.repository.SalonServiceRepository;
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
class SalonServiceImplementTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private SalonServiceRepository serviceRepository;

    @Mock
    private StylistRepository stylistRepository;

    @InjectMocks
    private SalonServiceImplement serviceService;

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

        SalonServiceSaveDto dto = SalonServiceSaveDto.builder()
                .name("Corte")
                .description("Corte basico")
                .price(15000)
                .duration(30)
                .stylistId(stylist.getId())
                .build();

        when(stylistRepository.findByIdAndTenantId(stylist.getId(), TENANT_ID)).thenReturn(Optional.of(stylist));

        serviceService.save(dto);

        ArgumentCaptor<SalonService> captor = ArgumentCaptor.forClass(SalonService.class);
        verify(serviceRepository).save(captor.capture());
        SalonService saved = captor.getValue();
        assertThat(saved.getNameService()).isEqualTo(dto.getName());
        assertThat(saved.getDescription()).isEqualTo(dto.getDescription());
        assertThat(saved.getPrice()).isEqualTo(dto.getPrice());
        assertThat(saved.getDuration()).isEqualTo(dto.getDuration());
        assertThat(saved.getStylist()).isEqualTo(stylist);
    }

    @Test
    void save_shouldThrowWhenStylistNotFound() {
        SalonServiceSaveDto dto = SalonServiceSaveDto.builder()
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

        SalonService service = SalonService.builder()
                .id(10L)
                .nameService("Corte")
                .description("Corte basico")
                .price(15000)
                .duration(30)
                .stylist(stylist)
                .build();

        when(serviceRepository.findByStylistIdAndTenantId(stylist.getId(), TENANT_ID))
                .thenReturn(List.of(service));

        List<SalonServiceResponseDto> result = serviceService.findByStylistId(stylist.getId());

        assertThat(result).hasSize(1);
        SalonServiceResponseDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(service.getId());
        assertThat(dto.getName()).isEqualTo(service.getNameService());
        assertThat(dto.getDescription()).isEqualTo(service.getDescription());
        assertThat(dto.getPrice()).isEqualTo(service.getPrice());
        assertThat(dto.getStylistId()).isEqualTo(stylist.getId());
    }

    @Test
    void findAll_shouldThrowWhenNoTenantContext() {
        TenantInterceptor.clear();
        assertThatThrownBy(() -> serviceService.findAll())
                .isInstanceOf(com.mr.sb.beauty_room.exceptions.TenantNotResolvedException.class);
    }
}
