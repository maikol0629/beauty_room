package com.mr.sb.beauty_room.Controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "telegram.bot.username=beauty_room_panel_bot",
        "server.error.include-message=always",
        "server.error.include-stacktrace=always"
})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class PanelSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPage_shouldBePublic() throws Exception {
        mockMvc.perform(get("/panel/login")).andExpect(status().isOk());
    }

    @Test
    void unauthenticatedPanel_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/panel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/panel/login"));
    }

    @Test
    void stylistLogin_shouldAccessPanelPagesAndCreateService() throws Exception {
        MockHttpSession session = loginAs("john@example.com");

        mockMvc.perform(get("/panel").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/panel/services").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/panel/schedules").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/panel/blocked").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/panel/appointments").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/panel/clients").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/panel/stylists").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/panel/public").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("https://t.me/beauty_room_panel_bot?start=salon-maria-001")));

        mockMvc.perform(post("/panel/services").session(session).with(csrf())
                        .param("name", "Corte de prueba")
                        .param("description", "Servicio creado desde el panel")
                        .param("price", "10000")
                        .param("duration", "30")
                        .param("stylistId", "1"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/panel/services").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Corte de prueba")));
    }

    @Test
    void qrImage_shouldReturnPngForLoggedStylist() throws Exception {
        MockHttpSession session = loginAs("john@example.com");
        mockMvc.perform(get("/panel/qr.png").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/png"));
    }

    @Test
    void clientRole_shouldBeForbiddenInPanel() throws Exception {
        MockHttpSession session = loginAs("alice@example.com");
        mockMvc.perform(get("/panel").session(session)).andExpect(status().isForbidden());
    }

    private MockHttpSession loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/panel/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
