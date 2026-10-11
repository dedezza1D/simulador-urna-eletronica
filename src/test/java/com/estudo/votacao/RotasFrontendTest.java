package com.estudo.votacao;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RotasFrontendTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
            "/, /index.html",
            "/mesario, /mesario.html",
            "/urna, /urna.html",
            "/transmissao, /transmissao.html",
            "/resultados, /resultados.html",
            "/candidatos, /candidatos.html",
    })
    void rotaEncaminhaParaAPagina(String rota, String pagina) throws Exception {
        mockMvc.perform(get(rota))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(pagina));
    }

    @Test
    void paginaEstaticaEhServida() throws Exception {
        mockMvc.perform(get("/urna.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-tecla=\"CONFIRMA\"")));
    }
}
