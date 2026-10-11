package com.estudo.votacao.urna;

import java.time.Instant;

public record UrnaResponse(
        Long eleicaoId,
        int zona,
        int secao,
        EstadoUrna estado,
        boolean eleitorHabilitado,
        long comparecimento,
        int eleitoresAptos,
        Instant abertaEm,
        Instant encerradaEm) {
}
