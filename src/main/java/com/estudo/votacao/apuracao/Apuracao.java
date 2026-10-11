package com.estudo.votacao.apuracao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.estudo.votacao.eleicao.Cargo;

/**
 * Regra de apuração de cargos majoritários. Função pura: sem banco, sem Spring, fácil de testar.
 * <ul>
 *   <li>Votos válidos são apenas os nominais; brancos e nulos não entram no cálculo.</li>
 *   <li>Cargo com 2º turno: eleito no 1º turno quem tiver mais da metade dos válidos;
 *       senão, os dois mais votados disputam o 2º turno.</li>
 *   <li>Cargo sem 2º turno: maioria simples.</li>
 * </ul>
 * Empates são desfeitos pelo número do candidato apenas para manter a ordem estável.
 * Na legislação, o critério de desempate é a idade (o mais idoso).
 */
public final class Apuracao {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private Apuracao() {
    }

    public static ResultadoCargo apurar(Cargo cargo, List<TotalCandidato> totais, long brancos, long nulos) {
        List<TotalCandidato> ordenados = totais.stream()
                .sorted(Comparator.comparingLong(TotalCandidato::votos).reversed()
                        .thenComparing(TotalCandidato::numero))
                .toList();
        long validos = ordenados.stream().mapToLong(TotalCandidato::votos).sum();

        List<ResultadoCargo.Candidato> candidatos = new ArrayList<>();
        for (int posicao = 0; posicao < ordenados.size(); posicao++) {
            TotalCandidato total = ordenados.get(posicao);
            Situacao situacao = situacao(cargo, posicao, ordenados.getFirst().votos(), validos);
            candidatos.add(new ResultadoCargo.Candidato(total.numero(), total.nome(), total.partido(), total.votos(),
                    percentual(total.votos(), validos), situacao));
        }
        return new ResultadoCargo(cargo, validos, brancos, nulos, List.copyOf(candidatos));
    }

    private static Situacao situacao(Cargo cargo, int posicao, long votosDoLider, long validos) {
        if (validos == 0) {
            return Situacao.INDEFINIDA;
        }
        if (!cargo.temSegundoTurno()) {
            return posicao == 0 ? Situacao.ELEITO : Situacao.NAO_ELEITO;
        }
        // "Mais da metade": exatamente 50% não elege. Comparação em inteiros evita erro de arredondamento.
        boolean liderTemMaioriaAbsoluta = votosDoLider * 2 > validos;
        if (liderTemMaioriaAbsoluta) {
            return posicao == 0 ? Situacao.ELEITO : Situacao.NAO_ELEITO;
        }
        return posicao < 2 ? Situacao.SEGUNDO_TURNO : Situacao.NAO_ELEITO;
    }

    private static BigDecimal percentual(long votos, long validos) {
        if (validos == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(votos).multiply(CEM).divide(BigDecimal.valueOf(validos), 2, RoundingMode.HALF_UP);
    }
}
