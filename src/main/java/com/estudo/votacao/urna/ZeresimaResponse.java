package com.estudo.votacao.urna;

import java.time.Instant;
import java.util.List;

import com.estudo.votacao.eleicao.Cargo;

/** Relatório emitido na abertura provando que a urna não tem nenhum voto registrado. */
public record ZeresimaResponse(
        Long eleicaoId,
        int zona,
        int secao,
        Instant emitidaEm,
        long registrosNoRdv,
        String chavePublica,
        List<CargoZerado> cargos) {

    public record CargoZerado(Cargo cargo, long brancos, long nulos, List<CandidatoZerado> candidatos) {
    }

    public record CandidatoZerado(String numero, String nome, long votos) {
    }
}
