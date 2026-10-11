package com.estudo.votacao.urna;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record HabilitarRequest(
        @NotBlank @Pattern(regexp = "\\d{12}", message = "deve conter 12 dígitos") String titulo) {
}
