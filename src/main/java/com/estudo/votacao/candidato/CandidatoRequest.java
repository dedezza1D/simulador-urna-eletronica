package com.estudo.votacao.candidato;

import com.estudo.votacao.eleicao.Cargo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CandidatoRequest(
        @NotNull Long eleicaoId,
        @NotNull Cargo cargo,
        @NotBlank @Pattern(regexp = "\\d{2,3}", message = "deve conter 2 ou 3 dígitos") String numero,
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 10) String partido) {
}
