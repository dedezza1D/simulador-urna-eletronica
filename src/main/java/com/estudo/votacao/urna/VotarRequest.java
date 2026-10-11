package com.estudo.votacao.urna;

import java.util.List;

import com.estudo.votacao.eleicao.Cargo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Votos do eleitor habilitado, um por cargo. Não leva o título: a urna já sabe quem está
 * habilitado, e o pedido de voto não deve carregar a identidade de quem vota.
 */
public record VotarRequest(@NotEmpty List<@Valid @NotNull Escolha> escolhas) {

    /** {@code branco = true} sem número, ou um número; número sem candidato vira voto nulo. */
    public record Escolha(
            @NotNull Cargo cargo,
            @Pattern(regexp = "\\d{2,3}", message = "deve conter 2 ou 3 dígitos") String numero,
            boolean branco) {
    }
}
