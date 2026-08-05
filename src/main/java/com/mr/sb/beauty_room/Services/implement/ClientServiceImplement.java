package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.client.ClientResponseDto;
import com.mr.sb.beauty_room.DTOS.client.ClientSaveDto;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.Services.IClientService;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class ClientServiceImplement implements IClientService {
    @Autowired
    private ClientRepository clientRepository;

    @Override
    public List<ClientResponseDto> findAll() {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Iterable<Client> clients = clientRepository.findByTenantId(tenantId);

       return StreamSupport.stream(clients.spliterator(), false).map(
                client -> ClientResponseDto.builder()
                        .id(client.getId())
                        .email(client.getEmail())
                        .phone(client.getPhone())
                        .name(client.getName_client())
                        .appointments(client.getAppointments())
                        .build()
        ).toList();

    }

    @Override
    public ClientResponseDto findById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Client> optionalClient = clientRepository.findByIdAndTenantId(id, tenantId);
        if (optionalClient.isPresent()) {
            Client client = optionalClient.get();

            return ClientResponseDto.builder()
                    .id(client.getId())
                    .email(client.getEmail())
                    .phone(client.getPhone())
                    .name(client.getName_client())
                    .appointments(client.getAppointments())
                    .build();

        }
        return null;
    }

    @Override
    public boolean save(ClientSaveDto clientSaveDto) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        if (clientSaveDto.getName() == null || clientSaveDto.getName().isBlank()) {
            return false;
        }
        Client client = Client.builder()
                .name_client(clientSaveDto.getName())
                .email(clientSaveDto.getEmail())
                .phone(clientSaveDto.getPhone())
                .tenant(Tenant.builder().id(tenantId).build())
                .build();
        clientRepository.save(client);
        return true;
    }

    @Override
    public boolean deleteById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Client> client = clientRepository.findByIdAndTenantId(id, tenantId);

        if(client.isPresent()){
            clientRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean update(ClientSaveDto clientSaveDto, long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Client> optionalClient = clientRepository.findByIdAndTenantId(id, tenantId);

        if (optionalClient.isPresent()) {

            Client client = optionalClient.get();

            client.setName_client(clientSaveDto.getName());
            client.setPhone(clientSaveDto.getPhone());
            client.setEmail(clientSaveDto.getEmail());

            clientRepository.save(client);
            return true;

        }
        return false;


    }
}
