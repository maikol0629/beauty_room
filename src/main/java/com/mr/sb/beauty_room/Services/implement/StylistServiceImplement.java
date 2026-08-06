package com.mr.sb.beauty_room.Services.implement;

import com.mr.sb.beauty_room.DTOS.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.DTOS.stylist.StylistSaveDto;
import com.mr.sb.beauty_room.Security.TenantInterceptor;
import com.mr.sb.beauty_room.Services.IStylistService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
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
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Iterable<Stylist> listStylist = stylistRepository.findByTenantId(tenantId);

        return StreamSupport.stream(listStylist.spliterator(),false).map(

                stylist ->
                StylistResponseDto.builder()
                        .email(stylist.getEmail())
                        .phone(stylist.getPhone())
                        .id(stylist.getId())
                        .stylistRoom(stylist.getStylistRoom())
                        .name(stylist.getName_stylist())
                        .telegramChatId(stylist.getTelegram_chat_id())
                        .build()

        ).toList();
    }

    @Override
    public StylistResponseDto findById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Stylist> opStylist = stylistRepository.findByIdAndTenantId(id, tenantId);

        if(opStylist.isPresent()){
            Stylist stylist = opStylist.get();

            return StylistResponseDto.builder()
                    .email(stylist.getEmail())
                    .phone(stylist.getPhone())
                    .id(stylist.getId())
                    .stylistRoom(stylist.getStylistRoom())
                    .name(stylist.getName_stylist())
                    .telegramChatId(stylist.getTelegram_chat_id())
                    .build();



        }
        return null;
    }

    @Override
    public boolean save(StylistSaveDto stylistSaveDto) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        if (stylistSaveDto.getName() == null || stylistSaveDto.getName().isBlank()) {
            return false;
        }
        Stylist stylist = Stylist.builder()
                .name_stylist(stylistSaveDto.getName())
                .email(stylistSaveDto.getEmail())
                .phone(stylistSaveDto.getPhone())
                .telegram_chat_id(stylistSaveDto.getTelegramChatId())
                .tenant(Tenant.builder().id(tenantId).build())
                .build();
        stylistRepository.save(stylist);
        return true;
    }


    @Override
    public boolean deleteById(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Stylist> stylist = stylistRepository.findByIdAndTenantId(id, tenantId);

        if(stylist.isPresent()){

            stylistRepository.deleteById(id);
            return true;

        }

        return false;

    }

    @Override
    public boolean update(StylistSaveDto stylistSaveDto, long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Stylist> stylistOptional = stylistRepository.findByIdAndTenantId(id, tenantId);

        if(stylistOptional.isPresent()){

            Stylist stylist = stylistOptional.get();
            stylist.setName_stylist(stylistSaveDto.getName());
            stylist.setEmail(stylistSaveDto.getEmail());
            stylist.setPhone(stylistSaveDto.getPhone());
            stylist.setTelegram_chat_id(stylistSaveDto.getTelegramChatId());
            stylistRepository.save(stylist);
            return true;
        }
        return false ;
    }

    @Override
    public boolean isExistStylist(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return stylistRepository.findByIdAndTenantId(id, tenantId).isPresent();
    }


}
