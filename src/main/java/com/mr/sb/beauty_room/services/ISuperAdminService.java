package com.mr.sb.beauty_room.services;

import com.mr.sb.beauty_room.dto.superadmin.TenantCreateDto;
import com.mr.sb.beauty_room.dto.superadmin.TenantUpdateDto;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.TenantPlan;
import com.mr.sb.beauty_room.entities.TenantStatus;
import com.mr.sb.beauty_room.entities.User;

import java.util.List;

/**
 * Operaciones del super administrador sobre los salones (tenants).
 * No filtra por tenant: el super admin ve toda la plataforma.
 */
public interface ISuperAdminService {

    List<Tenant> findAllTenants();

    Tenant findTenantById(Long id);

    Tenant createTenant(TenantCreateDto dto);

    Tenant updateTenant(Long id, TenantUpdateDto dto);

    Tenant setTenantStatus(Long id, TenantStatus status);

    List<User> findTenantUsers(Long tenantId);

    long countTenants();

    long countTenantsByStatus(TenantStatus status);

    long countTenantsByPlan(TenantPlan plan);

    long countAppointments();

    long countClients();

    long countStylists();
}
