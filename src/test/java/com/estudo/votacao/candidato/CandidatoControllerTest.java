package com.estudo.votacao.candidato;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.estudo.votacao.comum.ConflitoException;
import com.estudo.votacao.comum.RecursoNaoEncontradoException;
import com.estudo.votacao.eleicao.Cargo;

/** Testa só a camada web: roteamento, validação, status HTTP e formato dos erros. */
@WebMvcTest(CandidatoController.class)
class CandidatoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidatoService service;

    @Test
    void criaERetornaLocation() throws Exception {
        given(service.criar(any())).willReturn(new CandidatoResponse(42L, 1L, Cargo.PRESIDENTE, "10", "Ana", "PAB"));

        mockMvc.perform(post("/api/candidatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eleicaoId": 1, "cargo": "PRESIDENTE", "numero": "10", "nome": "Ana", "partido": "PAB"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/candidatos/42")))
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void retorna400ComListaDeCamposInvalidos() throws Exception {
        mockMvc.perform(post("/api/candidatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eleicaoId": 1, "cargo": "PRESIDENTE", "numero": "1A", "nome": "", "partido": "PAB"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("numero", "nome")));
        verifyNoInteractions(service);
    }

    @Test
    void retorna400ParaCargoInexistente() throws Exception {
        mockMvc.perform(post("/api/candidatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eleicaoId": 1, "cargo": "VEREADOR", "numero": "10", "nome": "Ana", "partido": "PAB"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void retorna404QuandoNaoEncontra() throws Exception {
        given(service.buscar(9L)).willThrow(new RecursoNaoEncontradoException("Candidato 9 não encontrado."));

        mockMvc.perform(get("/api/candidatos/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Candidato 9 não encontrado."));
    }

    @Test
    void retorna409EmConflito() throws Exception {
        given(service.criar(any())).willThrow(new ConflitoException("Número 10 já está em uso."));

        mockMvc.perform(post("/api/candidatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eleicaoId": 1, "cargo": "PRESIDENTE", "numero": "10", "nome": "Ana", "partido": "PAB"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"));
    }
}
