package com.estudo.votacao.apuracao;

import java.math.BigDecimal;
import java.util.List;

import com.estudo.votacao.eleicao.Cargo;

public record ResultadoCargo(Cargo cargo, long votosValidos, long brancos, long nulos, List<Candidato> candidatos) {

    public record Candidato(String numero, String nome, String partido, long votos,
                            BigDecimal percentualValidos, Situacao situacao) {
    }
}
