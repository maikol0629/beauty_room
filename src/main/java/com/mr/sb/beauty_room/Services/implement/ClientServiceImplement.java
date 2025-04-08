package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.client.ClientResponseDto;
import com.mr.sb.beauty_room.DTOS.client.ClientSaveDto;
import com.mr.sb.beauty_room.Services.IClientService;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ClientServiceImplement implements IClientService {
    @Autowired
    private ClientRepository clientRepository;

    @Override
    public List<ClientResponseDto> findAll() {

        Iterable<Client> clients = clientRepository.findAll();

       return StreamSupport.stream(clients.spliterator(), false).map(
                client -> ClientResponseDto.builder().email(client.getEmail())
                        .phone(client.getPhone())
                        .name(client.getName_client())
                        .appointments(client.getAppointments())
                        .build()
        ).toList();



    }

    @Override
    public ClientResponseDto findById(long id) {
        Optional<Client> optionalClient = clientRepository.findById(id);
        if (optionalClient.isPresent()) {
            Client client = optionalClient.get();

            return ClientResponseDto.builder()
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

        if(clientSaveDto.getName().isEmpty()&&clientSaveDto.getPhone().isEmpty()){

            return false;
        }

        clientRepository.save(
                Client.builder()
                        .email(clientSaveDto.getEmail())
                        .phone(clientSaveDto.getPhone())
                        .name_client(clientSaveDto.getName())
                        .build()
        );
        return true;


    }




    @Override
    public boolean deleteById(long id) {

        Optional<Client> client = clientRepository.findById(id);

        if(client.isPresent()){
            clientRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean update(ClientSaveDto clientSaveDto, long id) {
        Optional<Client> optionalClient = clientRepository.findById(id);


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
