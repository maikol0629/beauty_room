package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.stylist.StylistResponseDto;
import com.mr.sb.beauty_room.dto.stylist.StylistSaveDto;
import com.mr.sb.beauty_room.security.TenantInterceptor;
import com.mr.sb.beauty_room.services.IStylistService;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.repository.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class StylistServiceImplement implements IStylistService {

    private final StylistRepository stylistRepository;
    private final PasswordEncoder passwordEncoder;

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
                        .name(stylist.getNameStylist())
                        .telegramChatId(stylist.getTelegramChatId())
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
                    .name(stylist.getNameStylist())
                    .telegramChatId(stylist.getTelegramChatId())
                    .build();



        }
        return null;
    }

    @Override
    public boolean save(StylistSaveDto stylistSaveDto) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        String password = stylistSaveDto.getPassword();
        if (stylistSaveDto.getName() == null || stylistSaveDto.getName().isBlank()
                || password == null || password.isBlank()) {
            return false;
        }
        Stylist stylist = Stylist.builder()
                .nameStylist(stylistSaveDto.getName())
                .email(stylistSaveDto.getEmail())
                .password(passwordEncoder.encode(password))
                .phone(stylistSaveDto.getPhone())
                .telegramChatId(stylistSaveDto.getTelegramChatId())
                .vincularCode(generateVincularCode())
                .tenant(Tenant.builder().id(tenantId).build())
                .build();
        stylistRepository.save(stylist);
        return true;
    }

    @Override
    public String findVincularCode(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        return stylistRepository.findByIdAndTenantId(id, tenantId)
                .map(Stylist::getVincularCode)
                .orElse(null);
    }

    @Override
    public String regenerateVincularCode(long id) {
        Long tenantId = TenantInterceptor.getCurrentTenantIdOrThrow();
        Optional<Stylist> opStylist = stylistRepository.findByIdAndTenantId(id, tenantId);
        if (opStylist.isEmpty()) {
            return null;
        }
        Stylist stylist = opStylist.get();
        String code = generateVincularCode();
        stylist.setVincularCode(code);
        stylistRepository.save(stylist);
        return code;
    }

    private String generateVincularCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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
            stylist.setNameStylist(stylistSaveDto.getName());
            stylist.setEmail(stylistSaveDto.getEmail());
            stylist.setPhone(stylistSaveDto.getPhone());
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
