package com.estudo.votacao.comum;

/** O pedido está bem formado, mas viola uma regra de negócio. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
