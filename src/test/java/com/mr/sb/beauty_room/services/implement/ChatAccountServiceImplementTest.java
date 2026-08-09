package com.mr.sb.beauty_room.services.implement;

import com.mr.sb.beauty_room.dto.messaging.Channel;
import com.mr.sb.beauty_room.dto.messaging.ChannelMessage;
import com.mr.sb.beauty_room.entities.Client;
import com.mr.sb.beauty_room.entities.Stylist;
import com.mr.sb.beauty_room.entities.Tenant;
import com.mr.sb.beauty_room.entities.TenantStatus;
import com.mr.sb.beauty_room.repository.ClientRepository;
import com.mr.sb.beauty_room.repository.StylistRepository;
import com.mr.sb.beauty_room.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAccountServiceImplementTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private StylistRepository stylistRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChatAccountServiceImplement service;

    private ChannelMessage telegramMessage(String chatId, String text, String username, String firstName) {
        return new ChannelMessage(Channel.TELEGRAM, chatId, text, username, firstName, 123L, null);
    }

    private ChannelMessage whatsappMessage(String chatId, String text, String profileName) {
        return new ChannelMessage(Channel.WHATSAPP, chatId, text, null, profileName, null, null);
    }

    @Test
    void resolveTenant_withDeepLinkPayload_shouldResolveFromTenantKey() {
        when(tenantRepository.findByTenantKey("salon-maria-001"))
                .thenReturn(Optional.of(Tenant.builder().id(1L).build()));
        ChannelMessage msg = telegramMessage("111", "/start salon-maria-001", "juan", "Juan");

        assertThat(service.resolveTenant(msg)).isEqualTo(1L);
        verify(tenantRepository).findByTenantKey("salon-maria-001");
    }

    @Test
    void resolveTenant_withWhatsappPlainTextKey_shouldResolveFromTenantKey() {
        when(tenantRepository.findByTenantKey("estilos-ana-001"))
                .thenReturn(Optional.of(Tenant.builder().id(2L).build()));
        ChannelMessage msg = whatsappMessage("5491101234567", "estilos-ana-001", "Ana");

        assertThat(service.resolveTenant(msg)).isEqualTo(2L);
        verify(tenantRepository).findByTenantKey("estilos-ana-001");
    }

    @Test
    void resolveTenant_withoutPayload_knownClient_shouldResolveFromClientTenant() {
        Client client = new Client();
        client.setTenant(Tenant.builder().id(2L).build());
        when(clientRepository.findByTelegramChatId("222")).thenReturn(Optional.of(client));
        ChannelMessage msg = telegramMessage("222", "/start", null, null);

        assertThat(service.resolveTenant(msg)).isEqualTo(2L);
        verify(clientRepository).findByTelegramChatId("222");
    }

    @Test
    void resolveTenant_withoutPayload_knownWhatsappClient_shouldResolveFromClientTenant() {
        Client client = new Client();
        client.setTenant(Tenant.builder().id(4L).build());
        when(clientRepository.findByTelegramChatId("5491101234567")).thenReturn(Optional.empty());
        when(clientRepository.findByWhatsappChatId("5491101234567")).thenReturn(Optional.of(client));
        ChannelMessage msg = whatsappMessage("5491101234567", "Hola", "Ana");

        assertThat(service.resolveTenant(msg)).isEqualTo(4L);
        verify(clientRepository).findByWhatsappChatId("5491101234567");
    }

    @Test
    void resolveTenant_withoutPayload_knownStylist_shouldResolveFromStylistTenant() {
        Stylist stylist = new Stylist();
        stylist.setTenant(Tenant.builder().id(3L).build());
        when(clientRepository.findByTelegramChatId("333")).thenReturn(Optional.empty());
        when(stylistRepository.findByTelegramChatId("333")).thenReturn(Optional.of(stylist));
        ChannelMessage msg = telegramMessage("333", "/start", null, null);

        assertThat(service.resolveTenant(msg)).isEqualTo(3L);
    }

    @Test
    void resolveTenant_unknownChat_shouldReturnNull() {
        when(clientRepository.findByTelegramChatId("999")).thenReturn(Optional.empty());
        when(stylistRepository.findByTelegramChatId("999")).thenReturn(Optional.empty());
        ChannelMessage msg = telegramMessage("999", "/start", null, null);

        assertThat(service.resolveTenant(msg)).isNull();
    }

    @Test
    void findClientByChat_shouldDelegateToRepository() {
        Client client = Client.builder().id(3L).build();
        when(clientRepository.findByTelegramChatIdAndTenantId("111", 1L)).thenReturn(Optional.of(client));
        ChannelMessage msg = telegramMessage("111", null, null, null);

        assertThat(service.findClientByChat(msg, 1L)).contains(client);
    }

    @Test
    void findClientByChat_withWhatsapp_shouldDelegateToWhatsappRepository() {
        Client client = Client.builder().id(3L).build();
        when(clientRepository.findByWhatsappChatIdAndTenantId("5491101234567", 1L)).thenReturn(Optional.of(client));
        ChannelMessage msg = whatsappMessage("5491101234567", null, "Ana");

        assertThat(service.findClientByChat(msg, 1L)).contains(client);
        verify(clientRepository).findByWhatsappChatIdAndTenantId("5491101234567", 1L);
    }

    @Test
    void ensureClient_whenExists_shouldReturnExistingWithoutSaving() {
        Client existing = Client.builder().id(3L).build();
        when(clientRepository.findByTelegramChatIdAndTenantId("111", 1L)).thenReturn(Optional.of(existing));
        ChannelMessage msg = telegramMessage("111", null, "juan", "Juan");

        assertThat(service.ensureClient(msg, 1L)).isSameAs(existing);
        verify(clientRepository).findByTelegramChatIdAndTenantId("111", 1L);
    }

    @Test
    void ensureClient_whenNew_shouldCreateAndSaveWithEncodedPassword() {
        when(clientRepository.findByTelegramChatIdAndTenantId("111", 1L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
        ChannelMessage msg = telegramMessage("111", null, "juan", "Juan");

        Client client = service.ensureClient(msg, 1L);

        assertThat(client.getNameClient()).isEqualTo("Juan");
        assertThat(client.getTelegramChatId()).isEqualTo("111");
        assertThat(client.getEmail()).isEqualTo("tg_111@bot.local");
        assertThat(client.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(client.getTenant().getId()).isEqualTo(1L);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void ensureClient_whenNewWhatsapp_shouldCreateWithWhatsappChatId() {
        when(clientRepository.findByWhatsappChatIdAndTenantId("5491101234567", 1L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
        ChannelMessage msg = whatsappMessage("5491101234567", null, "Ana");

        Client client = service.ensureClient(msg, 1L);

        assertThat(client.getNameClient()).isEqualTo("Ana");
        assertThat(client.getWhatsappChatId()).isEqualTo("5491101234567");
        assertThat(client.getTelegramChatId()).isNull();
        assertThat(client.getEmail()).isEqualTo("wa_5491101234567@bot.local");
        assertThat(client.getTenant().getId()).isEqualTo(1L);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void findStylistByChat_shouldDelegateToRepository() {
        Stylist stylist = Stylist.builder().id(5L).build();
        when(stylistRepository.findByTelegramChatIdAndTenantId("111", 1L)).thenReturn(Optional.of(stylist));
        ChannelMessage msg = telegramMessage("111", null, null, null);

        assertThat(service.findStylistByChat(msg, 1L)).contains(stylist);
    }

    @Test
    void findStylistByChat_withWhatsapp_shouldDelegateToWhatsappRepository() {
        Stylist stylist = Stylist.builder().id(5L).build();
        when(stylistRepository.findByWhatsappChatIdAndTenantId("5491101234567", 1L)).thenReturn(Optional.of(stylist));
        ChannelMessage msg = whatsappMessage("5491101234567", null, "Ana");

        assertThat(service.findStylistByChat(msg, 1L)).contains(stylist);
        verify(stylistRepository).findByWhatsappChatIdAndTenantId("5491101234567", 1L);
    }

    @Test
    void findStylistByIdAndTenant_shouldDelegateToRepository() {
        Stylist stylist = Stylist.builder().id(5L).build();
        when(stylistRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(stylist));

        assertThat(service.findStylistByIdAndTenant(5L, 1L)).contains(stylist);
    }

    @Test
    void linkStylistByCode_shouldSetTelegramChatIdAndConsumeCode() {
        Stylist stylist = Stylist.builder().id(5L).vincularCode("abc123").build();
        when(stylistRepository.findByVincularCode("abc123")).thenReturn(Optional.of(stylist));
        when(stylistRepository.save(any(Stylist.class))).thenAnswer(inv -> inv.getArgument(0));
        ChannelMessage msg = telegramMessage("111", null, "juan", "Juan");

        Optional<Stylist> result = service.linkStylistByCode(msg, "abc123");

        assertThat(result).contains(stylist);
        assertThat(stylist.getTelegramChatId()).isEqualTo("111");
        assertThat(stylist.getVincularCode()).isNull();
        verify(stylistRepository).save(stylist);
    }

    @Test
    void linkStylistByCode_withWhatsapp_shouldSetWhatsappChatId() {
        Stylist stylist = Stylist.builder().id(5L).vincularCode("abc123").build();
        when(stylistRepository.findByVincularCode("abc123")).thenReturn(Optional.of(stylist));
        when(stylistRepository.save(any(Stylist.class))).thenAnswer(inv -> inv.getArgument(0));
        ChannelMessage msg = whatsappMessage("5491101234567", null, "Ana");

        Optional<Stylist> result = service.linkStylistByCode(msg, "abc123");

        assertThat(result).contains(stylist);
        assertThat(stylist.getWhatsappChatId()).isEqualTo("5491101234567");
        assertThat(stylist.getVincularCode()).isNull();
        verify(stylistRepository).save(stylist);
    }

    @Test
    void linkStylistByCode_unknownCode_shouldReturnEmpty() {
        when(stylistRepository.findByVincularCode("zzz")).thenReturn(Optional.empty());
        ChannelMessage msg = telegramMessage("111", null, null, null);

        assertThat(service.linkStylistByCode(msg, "zzz")).isEmpty();
    }

    @Test
    void linkStylistByCode_blankCode_shouldReturnEmpty() {
        ChannelMessage msg = telegramMessage("111", null, null, null);

        assertThat(service.linkStylistByCode(msg, "   ")).isEmpty();
        verify(stylistRepository, never()).findByVincularCode(anyString());
    }

    @Test
    void safeName_withUsername_shouldReturnUsernameWithAt() {
        ChannelMessage msg = telegramMessage("111", null, "juan", "Juan");

        assertThat(service.safeName(msg)).isEqualTo("@juan");
    }

    @Test
    void safeName_withoutUsername_shouldReturnFirstName() {
        ChannelMessage msg = telegramMessage("111", null, null, "Juan");

        assertThat(service.safeName(msg)).isEqualTo("Juan");
    }

    @Test
    void isTenantUsable_activeTenant_shouldReturnTrue() {
        Tenant active = Tenant.builder().id(1L).status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThat(service.isTenantUsable(1L)).isTrue();
    }

    @Test
    void isTenantUsable_suspendedTenant_shouldReturnFalse() {
        Tenant suspended = Tenant.builder().id(1L).status(TenantStatus.SUSPENDED).build();
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(suspended));

        assertThat(service.isTenantUsable(1L)).isFalse();
    }

    @Test
    void isTenantUsable_unknownTenant_shouldReturnFalse() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.isTenantUsable(99L)).isFalse();
    }

    @Test
    void isTenantUsable_nullTenantId_shouldReturnFalse() {
        assertThat(service.isTenantUsable(null)).isFalse();
    }

    @Test
    void displayName_withoutNames_shouldReturnFallback() {
        ChannelMessage msg = telegramMessage("111", null, null, null);

        assertThat(service.displayName(msg)).isEqualTo("Cliente Telegram");
    }

    @Test
    void displayName_whatsapp_withoutNames_shouldReturnWhatsappFallback() {
        ChannelMessage msg = whatsappMessage("5491101234567", null, null);

        assertThat(service.displayName(msg)).isEqualTo("Cliente WhatsApp");
    }
}
