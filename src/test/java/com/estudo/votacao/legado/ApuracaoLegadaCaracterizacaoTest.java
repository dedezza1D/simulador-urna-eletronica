package com.estudo.votacao.legado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de caracterização: registram o comportamento ATUAL do legado, inclusive os defeitos,
 * para que a refatoração não mude nada sem querer. Defeitos estão marcados com "DEFEITO".
 * Ao corrigir um defeito de propósito, o teste correspondente deve ser atualizado junto.
 */
class ApuracaoLegadaCaracterizacaoTest {

    private static Map<String, Object> c(String nome, int votos) {
        Map<String, Object> m = new HashMap<>();
        m.put("nome", nome);
        m.put("v", votos);
        return m;
    }

    @SafeVarargs
    private static List<Map<String, Object>> lista(Map<String, Object>... candidatos) {
        return new ArrayList<>(List.of(candidatos));
    }

    @Test
    void elegeNoPrimeiroTurnoComMaisDaMetade() {
        assertThat(ApuracaoLegada.proc(lista(c("B", 40), c("A", 60)), 0, 0, "PRESIDENTE"))
                .containsExactly("A;ELEITO", "B;NAO_ELEITO");
    }

    @Test
    void levaDoisAoSegundoTurno() {
        assertThat(ApuracaoLegada.proc(lista(c("A", 45), c("B", 35), c("C", 20)), 0, 0, "GOVERNADOR"))
                .containsExactly("A;SEGUNDO_TURNO", "B;SEGUNDO_TURNO", "C;NAO_ELEITO");
    }

    @Test
    void senadorPorMaioriaSimples() {
        assertThat(ApuracaoLegada.proc(lista(c("A", 30), c("B", 35)), 0, 0, "SENADOR"))
                .containsExactly("B;ELEITO", "A;NAO_ELEITO");
    }

    @Test
    @DisplayName("DEFEITO: exatamente 50% elege no 1º turno (deveria ir ao 2º turno)")
    void cinquentaPorCentoElege() {
        assertThat(ApuracaoLegada.proc(lista(c("A", 50), c("B", 30), c("C", 20)), 0, 0, "PRESIDENTE"))
                .containsExactly("A;ELEITO", "B;NAO_ELEITO", "C;NAO_ELEITO");
    }

    @Test
    @DisplayName("DEFEITO: brancos entram no total e podem forçar um 2º turno indevido")
    void brancosContamComoValidos() {
        // 60 de 100 válidos (60%), mas o legado divide por 100 + 50 brancos = 40%.
        assertThat(ApuracaoLegada.proc(lista(c("A", 60), c("B", 40)), 50, 0, "PRESIDENTE"))
                .containsExactly("A;SEGUNDO_TURNO", "B;SEGUNDO_TURNO");
    }

    @Test
    @DisplayName("DEFEITO: o parâmetro de nulos é ignorado")
    void nulosIgnorados() {
        assertThat(ApuracaoLegada.proc(lista(c("A", 60), c("B", 40)), 0, 1_000, "PRESIDENTE"))
                .containsExactly("A;ELEITO", "B;NAO_ELEITO");
    }

    @Test
    @DisplayName("DEFEITO: altera a ordem da lista recebida (efeito colateral)")
    void alteraListaDoChamador() {
        List<Map<String, Object>> entrada = lista(c("B", 40), c("A", 60));

        ApuracaoLegada.proc(entrada, 0, 0, "PRESIDENTE");

        assertThat(entrada.getFirst().get("nome")).isEqualTo("A");
    }

    @Test
    @DisplayName("DEFEITO: sem votos, cargo com 2º turno devolve lista vazia; senador elege o primeiro")
    void semVotos() {
        assertThat(ApuracaoLegada.proc(lista(c("A", 0), c("B", 0)), 0, 0, "PRESIDENTE")).isEmpty();
        assertThat(ApuracaoLegada.proc(lista(c("A", 0), c("B", 0)), 0, 0, "SENADOR"))
                .containsExactly("A;ELEITO", "B;NAO_ELEITO");
    }

    @Test
    @DisplayName("DEFEITO: lista vazia com brancos estoura IndexOutOfBounds")
    void listaVaziaComBrancos() {
        assertThatThrownBy(() -> ApuracaoLegada.proc(lista(), 10, 0, "PRESIDENTE"))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("DEFEITO: cargo nulo estoura NullPointerException")
    void cargoNulo() {
        assertThatThrownBy(() -> ApuracaoLegada.proc(lista(c("A", 1)), 0, 0, null))
                .isInstanceOf(NullPointerException.class);
    }
}
