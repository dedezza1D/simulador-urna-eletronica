package com.estudo.votacao.boletim;

import java.time.Instant;

public record BoletimResponse(Long id, Long eleicaoId, int zona, int secao, int comparecimento, Instant recebidoEm) {

    public static BoletimResponse de(BoletimUrna boletim) {
        return new BoletimResponse(
                boletim.getId(),
                boletim.getEleicao().getId(),
                boletim.getSecao().getZona(),
                boletim.getSecao().getNumero(),
                boletim.getComparecimento(),
                boletim.getRecebidoEm());
    }
}
