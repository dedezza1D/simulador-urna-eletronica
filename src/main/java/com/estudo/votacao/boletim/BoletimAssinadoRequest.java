package com.estudo.votacao.boletim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Envelope de transmissão: identifica a seção (para achar a chave pública da urna) e carrega
 * o conteúdo exatamente como foi assinado.
 */
public record BoletimAssinadoRequest(
        @NotNull Long eleicaoId,
        @NotNull @Positive Integer zona,
        @NotNull @Positive Integer secao,
        @NotBlank String conteudo,
        @NotBlank String assinatura) {
}
