package com.estudo.votacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Garante que as respostas de sql/respostas.sql rodam e devolvem o que os exercícios prometem. */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConsultasSqlTest {

    private static List<String> respostas;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void carregarRespostas() throws IOException {
        String semComentarios = Files.readAllLines(Path.of("sql", "respostas.sql"), StandardCharsets.UTF_8).stream()
                .filter(linha -> !linha.strip().startsWith("--"))
                .collect(Collectors.joining("\n"));
        respostas = Arrays.stream(semComentarios.split(";"))
                .map(String::strip)
                .filter(sql -> !sql.isEmpty())
                .toList();
    }

    private List<Map<String, Object>> executar(int indice) {
        return jdbc.queryForList(respostas.get(indice));
    }

    private static long numero(Object valor) {
        return ((Number) valor).longValue();
    }

    private static BigDecimal decimal(Object valor) {
        return new BigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    @Test
    void arquivoTemNoveConsultas() {
        assertThat(respostas).hasSize(9);
    }

    @Test
    void r1TotalPorCandidatoAPresidente() {
        List<Map<String, Object>> linhas = executar(0);

        assertThat(linhas).extracting(l -> l.get("nome"), l -> numero(l.get("total")))
                .containsExactly(
                        tuple("Ana Ribeiro", 690L),
                        tuple("Bruno Tavares", 645L),
                        tuple("Carla Menezes", 330L),
                        tuple("Diego Arantes", 135L),
                        tuple("Elias Rocha", 0L));
    }

    @Test
    void r2CandidatosComMaisDe600Votos() {
        assertThat(executar(1)).extracting(l -> l.get("nome"), l -> numero(l.get("total")))
                .containsExactly(
                        tuple("Fernanda Lopes", 1070L),
                        tuple("Júlia Fontes", 725L),
                        tuple("Ana Ribeiro", 690L),
                        tuple("Igor Santana", 680L),
                        tuple("Bruno Tavares", 645L));
    }

    @Test
    void r3ZonasComComparecimentoAbaixoDe80() {
        assertThat(executar(2)).singleElement().satisfies(l -> {
            assertThat(numero(l.get("zona"))).isEqualTo(1);
            assertThat(numero(l.get("aptos"))).isEqualTo(1070);
            assertThat(numero(l.get("comparecimento"))).isEqualTo(830);
            assertThat(decimal(l.get("pct"))).isEqualByComparingTo("77.57");
        });
    }

    @Test
    void r4SecoesSemBoletimNasDuasFormas() {
        for (int indice : new int[] {3, 4}) {
            assertThat(executar(indice)).extracting(l -> numero(l.get("zona")), l -> numero(l.get("numero")))
                    .containsExactly(tuple(3L, 2L));
        }
    }

    @Test
    void r5SecoesComMuitosBrancosENulos() {
        assertThat(executar(5)).extracting(l -> numero(l.get("zona")), l -> numero(l.get("numero")), l -> decimal(l.get("pct")))
                .containsExactly(
                        tuple(2L, 2L, new BigDecimal("12.50")),
                        tuple(1L, 3L, new BigDecimal("12.00")),
                        tuple(3L, 1L, new BigDecimal("10.34")));
    }

    @Test
    void r6PrimeiroColocadoPorCargo() {
        assertThat(executar(6)).extracting(l -> l.get("cargo"), l -> l.get("nome"), l -> numero(l.get("total")))
                .containsExactly(
                        tuple("GOVERNADOR", "Fernanda Lopes", 1070L),
                        tuple("PRESIDENTE", "Ana Ribeiro", 690L),
                        tuple("SENADOR", "Júlia Fontes", 725L));
    }

    @Test
    void r7VotosPorPartido() {
        assertThat(executar(7)).extracting(l -> l.get("partido"), l -> numero(l.get("candidatos")), l -> numero(l.get("total")))
                .containsExactly(
                        tuple("PAB", 3L, 2440L),
                        tuple("PCD", 3L, 1920L),
                        tuple("PGH", 2L, 530L),
                        tuple("PEF", 2L, 510L));
    }

    @Test
    void r8TresMaioresComparecimentos() {
        assertThat(executar(8)).extracting(l -> numero(l.get("zona")), l -> numero(l.get("numero")), l -> numero(l.get("comparecimento")))
                .containsExactly(tuple(2L, 1L, 340L), tuple(2L, 3L, 330L), tuple(1L, 1L, 300L));
    }
}
