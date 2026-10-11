package com.estudo.votacao.urna;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Fluxo completo da seção 2 da zona 3: abertura, votação, encerramento, transmissão e apuração. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UrnaFluxoIntegracaoTest {

    private static final String URNA = "/api/eleicoes/1/zonas/3/secoes/2/urna";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ResultActions postJson(String url, Object corpo) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corpo)));
    }

    private void habilitar(String titulo) throws Exception {
        postJson(URNA + "/habilitacao", Map.of("titulo", titulo)).andExpect(status().isOk());
    }

    private static Map<String, Object> nominal(String cargo, String numero) {
        return Map.of("cargo", cargo, "numero", numero, "branco", false);
    }

    private static Map<String, Object> branco(String cargo) {
        return Map.of("cargo", cargo, "branco", true);
    }

    private void votar(Map<String, Object> senador, Map<String, Object> governador, Map<String, Object> presidente)
            throws Exception {
        postJson(URNA + "/votos", Map.of("escolhas", List.of(senador, governador, presidente)))
                .andExpect(status().isNoContent());
    }

    @Test
    void abrirEmiteZeresimaComTudoZerado() throws Exception {
        mockMvc.perform(post(URNA + "/abertura"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrosNoRdv").value(0))
                .andExpect(jsonPath("$.cargos[*].cargo").value(org.hamcrest.Matchers.contains("SENADOR", "GOVERNADOR", "PRESIDENTE")))
                .andExpect(jsonPath("$.cargos[*].candidatos[*].votos", everyItem(is(0))))
                .andExpect(jsonPath("$.cargos[*].brancos", everyItem(is(0))));

        mockMvc.perform(post(URNA + "/abertura")).andExpect(status().isConflict());
    }

    @Test
    void naoAbreUrnaDeSecaoQueJaTemBoletim() throws Exception {
        mockMvc.perform(post("/api/eleicoes/1/zonas/1/secoes/1/urna/abertura")).andExpect(status().isConflict());
    }

    @Test
    void regrasDeHabilitacao() throws Exception {
        mockMvc.perform(post(URNA + "/abertura")).andExpect(status().isCreated());

        // Sem eleitor habilitado não há voto.
        postJson(URNA + "/votos", Map.of("escolhas", List.of(branco("SENADOR"), branco("GOVERNADOR"), branco("PRESIDENTE"))))
                .andExpect(status().isUnprocessableEntity());

        habilitar("900000000001");
        // Um eleitor por vez.
        postJson(URNA + "/habilitacao", Map.of("titulo", "900000000002")).andExpect(status().isConflict());

        votar(branco("SENADOR"), branco("GOVERNADOR"), branco("PRESIDENTE"));

        // Quem já votou não vota de novo.
        postJson(URNA + "/habilitacao", Map.of("titulo", "900000000001")).andExpect(status().isConflict());
        // Título inexistente.
        postJson(URNA + "/habilitacao", Map.of("titulo", "123456789012")).andExpect(status().isNotFound());

        habilitar("900000000002");
        mockMvc.perform(delete(URNA + "/habilitacao")).andExpect(status().isNoContent());
        habilitar("900000000002");
    }

    @Test
    void votoIncompletoOuComCargoRepetidoEhRecusado() throws Exception {
        mockMvc.perform(post(URNA + "/abertura")).andExpect(status().isCreated());
        habilitar("900000000001");

        postJson(URNA + "/votos", Map.of("escolhas", List.of(branco("SENADOR"), branco("PRESIDENTE"))))
                .andExpect(status().isUnprocessableEntity());
        postJson(URNA + "/votos", Map.of("escolhas", List.of(branco("SENADOR"), branco("SENADOR"), branco("PRESIDENTE"))))
                .andExpect(status().isUnprocessableEntity());
        postJson(URNA + "/votos", Map.of("escolhas", List.of(branco("SENADOR"), branco("GOVERNADOR"), nominal("PRESIDENTE", "101"))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get(URNA)).andExpect(jsonPath("$.comparecimento").value(0))
                .andExpect(jsonPath("$.eleitorHabilitado").value(true));
    }

    @Test
    void fluxoCompletoAteATotalizacao() throws Exception {
        mockMvc.perform(post(URNA + "/abertura")).andExpect(status().isCreated());

        habilitar("900000000001");
        votar(nominal("SENADOR", "202"), nominal("GOVERNADOR", "10"), nominal("PRESIDENTE", "10"));
        habilitar("900000000002");
        votar(nominal("SENADOR", "202"), branco("GOVERNADOR"), nominal("PRESIDENTE", "99")); // 99 não existe: nulo
        habilitar("900000000003");
        votar(nominal("SENADOR", "101"), nominal("GOVERNADOR", "20"), nominal("PRESIDENTE", "20"));

        mockMvc.perform(get(URNA + "/caderno").param("pendentes", "true"))
                .andExpect(jsonPath("$.totalElementos").value(327))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("900000000004"));

        // RDV só depois do encerramento.
        mockMvc.perform(get(URNA + "/rdv")).andExpect(status().isUnprocessableEntity());

        String encerramento = mockMvc.perform(post(URNA + "/encerramento"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode bu = objectMapper.readTree(encerramento);
        JsonNode conteudo = objectMapper.readTree(bu.get("conteudo").asText());

        assertThat(conteudo.get("comparecimento").asInt()).isEqualTo(3);
        JsonNode presidente = conteudo.get("cargos").get(2);
        assertThat(presidente.get("cargo").asText()).isEqualTo("PRESIDENTE");
        assertThat(presidente.get("nulos").asInt()).isEqualTo(1);
        assertThat(presidente.get("votos")).hasSize(2);
        assertThat(bu.get("hash").asText()).hasSize(64);

        // Depois de encerrada, a urna não habilita mais ninguém.
        postJson(URNA + "/habilitacao", Map.of("titulo", "900000000004")).andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get(URNA + "/rdv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9));

        Map<String, Object> envelope = Map.of("eleicaoId", 1, "zona", 3, "secao", 2,
                "conteudo", bu.get("conteudo").asText(), "assinatura", bu.get("assinatura").asText());

        // Adulteração: um voto a mais para o candidato 10. A assinatura deixa de bater.
        Map<String, Object> adulterado = new java.util.HashMap<>(envelope);
        adulterado.put("conteudo", bu.get("conteudo").asText().replace("\"numero\":\"10\",\"quantidade\":1", "\"numero\":\"10\",\"quantidade\":2"));
        assertThat(adulterado.get("conteudo")).isNotEqualTo(envelope.get("conteudo"));
        postJson("/api/boletins/assinados", adulterado)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Assinatura digital inválida. Boletim descartado."));

        postJson("/api/boletins/assinados", envelope).andExpect(status().isCreated());
        // Retransmissão idempotente.
        postJson("/api/boletins/assinados", envelope).andExpect(status().isOk());

        mockMvc.perform(get("/api/eleicoes/1/resultado").param("cargo", "PRESIDENTE"))
                .andExpect(jsonPath("$.parcial").value(false))
                .andExpect(jsonPath("$.resultado.candidatos[0].votos").value(691))
                .andExpect(jsonPath("$.resultado.nulos").value(115));
    }
}
