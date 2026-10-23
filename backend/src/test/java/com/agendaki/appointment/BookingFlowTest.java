package com.agendaki.appointment;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Teste de integração do fluxo completo: dono configura o negócio → cliente agenda,
 * remarca e cancela pela página pública. Usa um PostgreSQL real (Testcontainers) porque a
 * regra "sem sobreposição" depende da constraint EXCLUDE do banco. Precisa de Docker.
 *
 * Rode com: mvn test   (ou só este: mvn -Dtest=BookingFlowTest test)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BookingFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String slug;
    private String ownerToken;
    private long serviceId;
    private long professionalId;
    private LocalDate monday;

    /** Cada teste cria seu próprio negócio (slug único), então um não interfere no outro. */
    @BeforeEach
    void setUpBusiness() throws Exception {
        slug = "teste-" + UUID.randomUUID().toString().substring(0, 8);
        monday = LocalDate.now(ZONE).plusDays(1).with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));

        JsonNode auth = send(post("/api/auth/register"), Map.of(
                "businessName", "Barbearia Teste", "slug", slug,
                "email", slug + "@teste.com", "password", "senha12345"), 201);
        ownerToken = auth.get("token").asText();

        serviceId = send(owner(post("/api/services")), Map.of(
                "name", "Corte", "durationMin", 60, "priceCents", 4000), 201).get("id").asLong();

        professionalId = send(owner(post("/api/professionals")), Map.of(
                "name", "Zé", "serviceIds", List.of(serviceId)), 201).get("id").asLong();

        // Segunda-feira (weekday 1), 09:00–12:00 → serviço de 60 min cabe em 09:00…11:00 (passo de 15 min)
        send(owner(put("/api/professionals/" + professionalId + "/working-hours")),
                List.of(Map.of("weekday", 1, "startTime", "09:00", "endTime", "12:00")), 200);
    }

    @Test
    void catalogoPublicoListaServicoEProfissional() throws Exception {
        mvc.perform(get("/api/public/businesses/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services", hasSize(1)))
                .andExpect(jsonPath("$.services[0].name").value("Corte"))
                .andExpect(jsonPath("$.professionals[0].serviceIds[0]").value(serviceId));
    }

    @Test
    void horariosRespeitamJanelaDeTrabalhoEDuracao() throws Exception {
        mvc.perform(availability())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0]").value("09:00"))
                .andExpect(jsonPath("$.slots", hasItem("11:00")))
                // 11:15 não cabe: o serviço de 60 min terminaria depois das 12:00
                .andExpect(jsonPath("$.slots", not(hasItem("11:15"))));
    }

    @Test
    void agendarRemarcarECancelarPeloLinkDoCliente() throws Exception {
        JsonNode booked = send(post("/api/public/businesses/" + slug + "/appointments"),
                booking("10:00", "Maria"), 201);
        String token = booked.get("manageToken").asText();
        long appointmentId = booked.get("id").asLong();

        // O horário sai da lista (e 09:15–10:45 também, pois o serviço de 60 min se sobreporia)
        mvc.perform(availability())
                .andExpect(jsonPath("$.slots", not(hasItem("10:00"))))
                .andExpect(jsonPath("$.slots", not(hasItem("09:15"))))
                .andExpect(jsonPath("$.slots", hasItem("09:00")))
                .andExpect(jsonPath("$.slots", hasItem("11:00")));

        // Dono vê o agendamento no dia (sem expor o token)
        mvc.perform(owner(get("/api/appointments")).param("date", monday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(appointmentId))
                .andExpect(jsonPath("$[0].startTime").value("10:00"))
                .andExpect(jsonPath("$[0].manageToken").doesNotExist());

        // Cliente remarca para 09:00 → 10:00 volta a ficar livre
        send(post("/api/public/appointments/" + token + "/reschedule"),
                Map.of("date", monday.toString(), "time", "09:00"), 200);
        mvc.perform(availability()).andExpect(jsonPath("$.slots", hasItem("11:00")))
                .andExpect(jsonPath("$.slots", not(hasItem("09:00"))));
        mvc.perform(get("/api/public/appointments/" + token))
                .andExpect(jsonPath("$.startTime").value("09:00"));

        // Cliente cancela → horário volta; cancelar de novo dá 409
        send(post("/api/public/appointments/" + token + "/cancel"), null, 200);
        mvc.perform(availability()).andExpect(jsonPath("$.slots", hasItem("09:00")));
        mvc.perform(post("/api/public/appointments/" + token + "/cancel")).andExpect(status().isConflict());
    }

    @Test
    void mesmoHorarioDuasVezesRetorna409() throws Exception {
        send(post("/api/public/businesses/" + slug + "/appointments"), booking("09:00", "Ana"), 201);
        send(post("/api/public/businesses/" + slug + "/appointments"), booking("09:00", "Bia"), 409);
        // sobreposição parcial também é barrada
        send(post("/api/public/businesses/" + slug + "/appointments"), booking("09:30", "Cris"), 409);
    }

    @Test
    void donoCancelaAgendamentoEHorarioVoltaAFicarLivre() throws Exception {
        long id = send(post("/api/public/businesses/" + slug + "/appointments"),
                booking("11:00", "Davi"), 201).get("id").asLong();

        send(owner(post("/api/appointments/" + id + "/cancel")), null, 200);

        mvc.perform(availability()).andExpect(jsonPath("$.slots", hasItem("11:00")));
        send(post("/api/public/businesses/" + slug + "/appointments"), booking("11:00", "Eva"), 201);
    }

    @Test
    void validacoesRetornamErroEmVezDeAgendar() throws Exception {
        // fora da janela de trabalho
        send(post("/api/public/businesses/" + slug + "/appointments"), booking("14:00", "Fê"), 409);
        // telefone inválido
        Map<String, Object> badPhone = new java.util.HashMap<>(booking("10:00", "Gui"));
        badPhone.put("customerPhone", "abc");
        send(post("/api/public/businesses/" + slug + "/appointments"), badPhone, 400);
        // passado
        Map<String, Object> past = new java.util.HashMap<>(booking("10:00", "Hugo"));
        past.put("date", LocalDate.now(ZONE).minusDays(1).toString());
        mvc.perform(post("/api/public/businesses/" + slug + "/appointments")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(past)))
                .andExpect(status().is4xxClientError());
        // negócio inexistente
        mvc.perform(get("/api/public/businesses/nao-existe-" + slug)).andExpect(status().isNotFound());
    }

    @Test
    void rotasDoDonoExigemLoginEIsolamNegocios() throws Exception {
        mvc.perform(get("/api/appointments")).andExpect(status().isUnauthorized());

        // Outro negócio não enxerga nem cancela agendamentos deste
        long id = send(post("/api/public/businesses/" + slug + "/appointments"),
                booking("10:00", "Iara"), 201).get("id").asLong();
        String other = "outro-" + UUID.randomUUID().toString().substring(0, 8);
        String otherToken = send(post("/api/auth/register"), Map.of(
                "businessName", "Outro", "slug", other,
                "email", other + "@teste.com", "password", "senha12345"), 201).get("token").asText();

        mvc.perform(get("/api/appointments").param("date", monday.toString())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(post("/api/appointments/" + id + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ helpers

    private MockHttpServletRequestBuilder owner(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + ownerToken);
    }

    private MockHttpServletRequestBuilder availability() {
        return get("/api/public/businesses/" + slug + "/availability")
                .param("serviceId", String.valueOf(serviceId))
                .param("professionalId", String.valueOf(professionalId))
                .param("date", monday.toString());
    }

    private Map<String, Object> booking(String time, String name) {
        return Map.of("serviceId", serviceId, "professionalId", professionalId,
                "date", monday.toString(), "time", time,
                "customerName", name, "customerPhone", "(31) 99999-0000");
    }

    /** Envia JSON, confere o status esperado e devolve o corpo como árvore JSON. */
    private JsonNode send(MockHttpServletRequestBuilder builder, Object body, int expectedStatus) throws Exception {
        builder.contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            builder.content(json.writeValueAsString(body));
        }
        MvcResult result = mvc.perform(builder).andExpect(status().is(expectedStatus)).andReturn();
        String content = result.getResponse().getContentAsString();
        return content.isBlank() ? json.createObjectNode() : json.readTree(content);
    }
}
