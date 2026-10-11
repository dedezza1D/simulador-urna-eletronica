package com.estudo.votacao.apuracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.estudo.votacao.eleicao.Cargo;

class ApuracaoTest {

    private static TotalCandidato candidato(String numero, long votos) {
        return new TotalCandidato(numero, "Candidato " + numero, "P" + numero, votos);
    }

    private static List<Situacao> situacoes(ResultadoCargo resultado) {
        return resultado.candidatos().stream().map(ResultadoCargo.Candidato::situacao).toList();
    }

    @Test
    @DisplayName("Líder com mais da metade dos válidos é eleito no 1º turno")
    void elegeNoPrimeiroTurno() {
        ResultadoCargo resultado = Apuracao.apurar(Cargo.GOVERNADOR,
                List.of(candidato("20", 40), candidato("10", 60)), 0, 0);

        assertThat(resultado.candidatos()).extracting(ResultadoCargo.Candidato::numero).containsExactly("10", "20");
        assertThat(situacoes(resultado)).containsExactly(Situacao.ELEITO, Situacao.NAO_ELEITO);
    }

    @Test
    @DisplayName("Sem maioria absoluta, os dois primeiros vão ao 2º turno")
    void levaDoisPrimeirosAoSegundoTurno() {
        ResultadoCargo resultado = Apuracao.apurar(Cargo.PRESIDENTE,
                List.of(candidato("10", 45), candidato("20", 35), candidato("30", 20)), 0, 0);

        assertThat(situacoes(resultado))
                .containsExactly(Situacao.SEGUNDO_TURNO, Situacao.SEGUNDO_TURNO, Situacao.NAO_ELEITO);
    }

    @Test
    @DisplayName("Exatamente 50% dos válidos não é maioria absoluta")
    void cinquentaPorCentoNaoElege() {
        ResultadoCargo resultado = Apuracao.apurar(Cargo.PRESIDENTE,
                List.of(candidato("10", 50), candidato("20", 30), candidato("30", 20)), 0, 0);

        assertThat(resultado.candidatos().getFirst().situacao()).isEqualTo(Situacao.SEGUNDO_TURNO);
    }

    @Test
    @DisplayName("Brancos e nulos não contam como votos válidos")
    void brancosENulosNaoSaoValidos() {
        // 60 de 100 válidos (60%). Se brancos contassem, seriam 60 de 200 (30%) e haveria 2º turno.
        ResultadoCargo resultado = Apuracao.apurar(Cargo.PRESIDENTE,
                List.of(candidato("10", 60), candidato("20", 40)), 70, 30);

        assertThat(resultado.votosValidos()).isEqualTo(100);
        assertThat(resultado.brancos()).isEqualTo(70);
        assertThat(resultado.candidatos().getFirst().percentualValidos()).isEqualByComparingTo("60.00");
        assertThat(resultado.candidatos().getFirst().situacao()).isEqualTo(Situacao.ELEITO);
    }

    @Test
    @DisplayName("Senador é eleito por maioria simples, sem 2º turno")
    void senadorPorMaioriaSimples() {
        ResultadoCargo resultado = Apuracao.apurar(Cargo.SENADOR,
                List.of(candidato("101", 30), candidato("202", 35), candidato("404", 34)), 0, 0);

        assertThat(resultado.candidatos().getFirst().numero()).isEqualTo("202");
        assertThat(situacoes(resultado)).containsExactly(Situacao.ELEITO, Situacao.NAO_ELEITO, Situacao.NAO_ELEITO);
    }

    @Test
    @DisplayName("Percentual é arredondado para duas casas (HALF_UP)")
    void arredondaPercentual() {
        ResultadoCargo resultado = Apuracao.apurar(Cargo.SENADOR,
                List.of(candidato("101", 1), candidato("202", 2)), 0, 0);

        assertThat(resultado.candidatos()).extracting(ResultadoCargo.Candidato::percentualValidos)
                .containsExactly(new BigDecimal("66.67"), new BigDecimal("33.33"));
    }

    @Test
    @DisplayName("Sem votos válidos, a situação fica indefinida")
    void semVotos() {
        ResultadoCargo resultado = Apuracao.apurar(Cargo.PRESIDENTE,
                List.of(candidato("10", 0), candidato("20", 0)), 5, 5);

        assertThat(situacoes(resultado)).containsOnly(Situacao.INDEFINIDA);
        assertThat(resultado.candidatos()).extracting(ResultadoCargo.Candidato::percentualValidos)
                .containsOnly(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("Empate é ordenado de forma estável pelo número")
    void empateOrdenadoPorNumero() {
        ResultadoCargo resultado = Apuracao.apurar(Cargo.PRESIDENTE,
                List.of(candidato("30", 40), candidato("20", 40), candidato("10", 20)), 0, 0);

        assertThat(resultado.candidatos()).extracting(ResultadoCargo.Candidato::numero).containsExactly("20", "30", "10");
    }
}
