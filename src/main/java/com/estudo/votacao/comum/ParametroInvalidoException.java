package com.estudo.votacao.comum;

/** Parâmetro de consulta inválido (filtro, ordenação). Vira HTTP 400. */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
