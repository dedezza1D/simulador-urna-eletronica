package com.estudo.votacao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sobe a aplicação inteira com H2 + Flyway (V1 schema, V2 dados de exemplo).
 * {@code @Transactional} desfaz as alterações de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VotacaoIntegracaoTest {

    private static final String BOLETIM_ZONA3_SECAO2 = """
            {
              "eleicaoId": 1, "zona": 3, "secao": 2, "comparecimento": 300,
              "cargos": [
                {"cargo": "PRESIDENTE", "brancos": 10, "nulos": 10,
                 "votos": [{"numero": "10", "quantidade": 200}, {"numero": "20", "quantidade": 80}]},
                {"cargo": "GOVERNADOR", "brancos": 10, "nulos": 10,
                 "votos": [{"numero": "10", "quantidade": 150}, {"numero": "20", "quantidade": 130}]},
                {"cargo": "SENADOR", "brancos": 10, "nulos": 10,
                 "votos": [{"numero": "101", "quantidade": 100}, {"numero": "202", "quantidade": 180}]}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void presidenteVaiParaSegundoTurnoComApuracaoParcial() throws Exception {
        mockMvc.perform(get("/api/eleicoes/1/resultado").param("cargo", "PRESIDENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcial").value(true))
                .andExpect(jsonPath("$.secoesTotalizadas").value(7))
                .andExpect(jsonPath("$.secoesTotal").value(8))
                .andExpect(jsonPath("$.resultado.votosValidos").value(1800))
                .andExpect(jsonPath("$.resultado.brancos").value(76))
                .andExpect(jsonPath("$.resultado.candidatos[0].nome").value("Ana Ribeiro"))
                .andExpect(jsonPath("$.resultado.candidatos[0].votos").value(690))
                .andExpect(jsonPath("$.resultado.candidatos[0].percentualValidos").value(38.33))
                .andExpect(jsonPath("$.resultado.candidatos[0].situacao").value("SEGUNDO_TURNO"))
                .andExpect(jsonPath("$.resultado.candidatos[1].situacao").value("SEGUNDO_TURNO"))
                .andExpect(jsonPath("$.resultado.candidatos[4].nome").value("Elias Rocha"))
                .andExpect(jsonPath("$.resultado.candidatos[4].votos").value(0));
    }

    @Test
    void governadorEleitoNoPrimeiroTurnoESenadorPorMaioriaSimples() throws Exception {
        mockMvc.perform(get("/api/eleicoes/1/resultado").param("cargo", "GOVERNADOR"))
                .andExpect(jsonPath("$.resultado.candidatos[0].nome").value("Fernanda Lopes"))
                .andExpect(jsonPath("$.resultado.candidatos[0].situacao").value("ELEITO"));

        mockMvc.perform(get("/api/eleicoes/1/resultado").param("cargo", "SENADOR"))
                .andExpect(jsonPath("$.resultado.candidatos[0].nome").value("Júlia Fontes"))
                .andExpect(jsonPath("$.resultado.candidatos[0].situacao").value("ELEITO"));
    }

    @Test
    void ultimoBoletimFechaAApuracaoESegundoEnvioEhRecusado() throws Exception {
        mockMvc.perform(post("/api/boletins").contentType(MediaType.APPLICATION_JSON).content(BOLETIM_ZONA3_SECAO2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zona").value(3))
                .andExpect(jsonPath("$.secao").value(2));

        mockMvc.perform(get("/api/eleicoes/1/resultado").param("cargo", "PRESIDENTE"))
                .andExpect(jsonPath("$.parcial").value(false))
                .andExpect(jsonPath("$.resultado.candidatos[0].votos").value(890));

        mockMvc.perform(post("/api/boletins").contentType(MediaType.APPLICATION_JSON).content(BOLETIM_ZONA3_SECAO2))
                .andExpect(status().isConflict());
    }

    @Test
    void boletimComSomaInconsistenteRetorna422() throws Exception {
        String inconsistente = BOLETIM_ZONA3_SECAO2.replace("\"quantidade\": 200", "\"quantidade\": 199");

        mockMvc.perform(post("/api/boletins").contentType(MediaType.APPLICATION_JSON).content(inconsistente))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    void candidatoSemVotosPodeSerRemovidoMasComVotosNao() throws Exception {
        mockMvc.perform(get("/api/candidatos").param("cargo", "PRESIDENTE"))
                .andExpect(jsonPath("$.totalElementos").value(5));

        Long semVotos = idDoCandidato("PRESIDENTE", 4);
        Long comVotos = idDoCandidato("PRESIDENTE", 0);

        mockMvc.perform(delete("/api/candidatos/{id}", semVotos)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/candidatos/{id}", comVotos)).andExpect(status().isConflict());
    }

    @Test
    void ordenacaoPorCampoInexistenteRetorna400() throws Exception {
        mockMvc.perform(get("/api/candidatos").param("sort", "senha"))
                .andExpect(status().isBadRequest());
    }

    private Long idDoCandidato(String cargo, int posicaoPorNumero) throws Exception {
        String json = mockMvc.perform(get("/api/candidatos").param("cargo", cargo).param("sort", "numero"))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.<Number>read(json, "$.conteudo[" + posicaoPorNumero + "].id").longValue();
    }
}
