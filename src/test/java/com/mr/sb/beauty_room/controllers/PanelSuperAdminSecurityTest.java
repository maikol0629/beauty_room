package com.mr.sb.beauty_room.controllers;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "telegram.bot.username=beauty_room_panel_bot",
        "server.error.include-message=always",
        "server.error.include-stacktrace=always"
})
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class PanelSuperAdminSecurityTest {

    private static final String SUPER_ADMIN_EMAIL = "superadmin@beautyroom.app";
    private static final String SUPER_ADMIN_PASSWORD = "superadmin123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void superAdmin_shouldAccessSuperPanel() throws Exception {
        MockHttpSession session = loginAs(SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD);

        mockMvc.perform(get("/panel/super").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/panel/super/tenants").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Salones")));
    }

    @Test
    void superAdmin_homeShouldRedirectToSuperPanel() throws Exception {
        MockHttpSession session = loginAs(SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD);

        mockMvc.perform(get("/panel").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/panel/super"));
    }

    @Test
    void superAdmin_shouldCreateTenant() throws Exception {
        MockHttpSession session = loginAs(SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD);

        mockMvc.perform(post("/panel/super/tenants").session(session).with(csrf())
                        .param("name", "Salón Prueba")
                        .param("plan", "TRIAL")
                        .param("status", "ACTIVE")
                        .param("adminEmail", "admin.prueba@example.com")
                        .param("adminPassword", "password"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/panel/super/tenants").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Salón Prueba")));
    }

    @Test
    void stylist_shouldBeForbiddenInSuperPanel() throws Exception {
        MockHttpSession session = loginAs("john@example.com", "password");

        mockMvc.perform(get("/panel/super").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/panel/super/tenants").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void client_shouldBeForbiddenInSuperPanel() throws Exception {
        MockHttpSession session = loginAs("alice@example.com", "password");

        mockMvc.perform(get("/panel/super").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_shouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/panel/super"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/panel/login"));
    }

    private MockHttpSession loginAs(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/panel/login")
                        .with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
