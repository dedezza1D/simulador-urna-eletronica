package com.estudo.votacao.apuracao;

import java.math.BigDecimal;

public record ResultadoResponse(
        Long eleicaoId,
        long secoesTotalizadas,
        long secoesTotal,
        BigDecimal percentualSecoesTotalizadas,
        boolean parcial,
        ResultadoCargo resultado) {
}
