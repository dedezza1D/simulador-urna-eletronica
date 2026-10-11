package com.estudo.votacao.boletim;

import java.util.List;

import com.estudo.votacao.eleicao.Cargo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RegistrarBoletimRequest(
        @NotNull Long eleicaoId,
        @NotNull @Positive Integer zona,
        @NotNull @Positive Integer secao,
        @NotNull @PositiveOrZero Integer comparecimento,
        @NotEmpty List<@Valid @NotNull VotacaoCargo> cargos) {

    public record VotacaoCargo(
            @NotNull Cargo cargo,
            @NotNull @PositiveOrZero Integer brancos,
            @NotNull @PositiveOrZero Integer nulos,
            @NotNull List<@Valid @NotNull Voto> votos) {
    }

    public record Voto(
            @NotBlank @Pattern(regexp = "\\d{2,3}", message = "deve conter 2 ou 3 dígitos") String numero,
            @NotNull @PositiveOrZero Integer quantidade) {
    }
}
