package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.client.ClientResponseDto;
import com.mr.sb.beauty_room.dto.client.ClientSaveDto;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.services.IClientService;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class ClientServiceImplement implements IClientService {
    private final ClientRepository clientRepository;

    @Override
    public List<ClientResponseDto> findAll() {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Iterable<Client> clients = clientRepository.findByTenantId(tenantId);

       return StreamSupport.stream(clients.spliterator(), false).map(
                client -> ClientResponseDto.builder()
                        .id(client.getId())
                        .email(client.getEmail())
                        .phone(client.getPhone())
                        .name(client.getNameClient())
                        .telegramChatId(client.getTelegramChatId())
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
                    .name(client.getNameClient())
                    .telegramChatId(client.getTelegramChatId())
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
                .nameClient(clientSaveDto.getName())
                .email(clientSaveDto.getEmail())
                .phone(clientSaveDto.getPhone())
                .telegramChatId(clientSaveDto.getTelegramChatId())
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

            client.setNameClient(clientSaveDto.getName());
            client.setPhone(clientSaveDto.getPhone());
            client.setEmail(clientSaveDto.getEmail());
            client.setTelegramChatId(clientSaveDto.getTelegramChatId());

            clientRepository.save(client);
            return true;

        }
        return false;


    }
}
