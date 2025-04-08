package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist.StylistSaveDto;
import com.mr.sb.beauty_room.Services.IStylistService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.repository.StylistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class StylistServiceImplement implements IStylistService {

    @Autowired
    private StylistRepository stylistRepository;

    @Override
    public List<StylistResponseDto> findAll() {
        Iterable<Stylist> listStylist = stylistRepository.findAll();

        return StreamSupport.stream(listStylist.spliterator(),false).map(

                stylist ->
                StylistResponseDto.builder()
                        .email(stylist.getEmail())
                        .phone(stylist.getPhone())
                        .id(stylist.getId())
                        .stylistRoom(stylist.getStylistRoom())
                        .name(stylist.getName_stylist())
                        .build()

        ).toList();
    }

    @Override
    public StylistResponseDto findById(long id) {

        Optional<Stylist> opStylist = stylistRepository.findById(id);

        if(opStylist.isPresent()){
            Stylist stylist = opStylist.get();

            return StylistResponseDto.builder()
                    .email(stylist.getEmail())
                    .phone(stylist.getPhone())
                    .id(stylist.getId())
                    .stylistRoom(stylist.getStylistRoom())
                    .name(stylist.getName_stylist())
                    .build();



        }
        return null;
    }

    @Override
    public boolean save(StylistSaveDto stylistSaveDto) {

        if (stylistSaveDto.isNotEmpty()){

            Stylist stylist = Stylist.builder().name_stylist(stylistSaveDto.getName())
                    .email(stylistSaveDto.getEmail())
                    .phone(stylistSaveDto.getPhone())
                    .build();
            stylistRepository.save(stylist);
            return true;
        }
        return false;


    }


    @Override
    public boolean deleteById(long id) {
        Optional<Stylist> stylist = stylistRepository.findById(id);

        if(stylist.isPresent()){

            stylistRepository.deleteById(id);
            return true;

        }

        return false;

    }

    @Override
    public boolean update(StylistSaveDto stylistSaveDto, long id) {
        Optional<Stylist> stylistOptional = stylistRepository.findById(id);

        if(stylistOptional.isPresent()){

            Stylist stylist = stylistOptional.get();
            stylist.setName_stylist(stylistSaveDto.getName());
            stylist.setEmail(stylistSaveDto.getEmail());
            stylist.setPhone(stylistSaveDto.getPhone());
            stylistRepository.save(stylist);
            return true;
        }
        return false ;
    }

    @Override
    public boolean isExistStylist(long id) {
        Optional<Stylist> stylistOptional = stylistRepository.findById(id);

        return stylistOptional.isPresent();
    }


}
