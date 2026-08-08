package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.exceptions.TenantNotResolvedException;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.services.IStylistService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TenantIsolationTest {

    @Autowired
    private StylistRepository stylistRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private IStylistService stylistService;

    @AfterEach
    void tearDown() {
        TenantInterceptor.clear();
    }

    @Test
    void findAll_shouldReturnOnlyStylistsOfCurrentTenant() {
        Tenant tenantA = tenantRepository.save(newTenant("A"));
        Tenant tenantB = tenantRepository.save(newTenant("B"));

        Stylist stylistA = newStylist("stylistA", tenantA);
        Stylist stylistB = newStylist("stylistB", tenantB);
        stylistRepository.save(stylistA);
        stylistRepository.save(stylistB);

        TenantInterceptor.setCurrentTenantId(tenantA.getId());
        List<StylistResponseDto> listA = stylistService.findAll();
        assertThat(listA).isNotEmpty();
        assertThat(listA.stream().map(StylistResponseDto::getId))
                .contains(stylistA.getId())
                .doesNotContain(stylistB.getId());

        TenantInterceptor.setCurrentTenantId(tenantB.getId());
        List<StylistResponseDto> listB = stylistService.findAll();
        assertThat(listB.stream().map(StylistResponseDto::getId))
                .contains(stylistB.getId())
                .doesNotContain(stylistA.getId());
    }

    @Test
    void findById_shouldNotReturnStylistFromAnotherTenant() {
        Tenant tenantA = tenantRepository.save(newTenant("C"));
        Tenant tenantB = tenantRepository.save(newTenant("D"));

        Stylist stylistA = newStylist("stylistC", tenantA);
        Stylist stylistB = newStylist("stylistD", tenantB);
        stylistRepository.save(stylistA);
        stylistRepository.save(stylistB);

        TenantInterceptor.setCurrentTenantId(tenantA.getId());
        assertThat(stylistService.findById(stylistB.getId())).isNull();
        assertThat(stylistService.findById(stylistA.getId())).isNotNull();
    }

    @Test
    void findAll_shouldThrowWhenNoTenantContext() {
        TenantInterceptor.clear();
        assertThatThrownBy(() -> stylistService.findAll())
                .isInstanceOf(TenantNotResolvedException.class);
    }

    private Tenant newTenant(String label) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return Tenant.builder()
                .name("Tenant " + label + " " + suffix)
                .tenantKey("tenant-" + label.toLowerCase() + "-" + suffix)
                .build();
    }

    private Stylist newStylist(String name, Tenant tenant) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return Stylist.builder()
                .email(name + "-" + suffix + "@test.com")
                .password("password")
                .nameStylist(name + "-" + suffix)
                .phone("3000000000")
                .tenant(tenant)
                .build();
    }
}
