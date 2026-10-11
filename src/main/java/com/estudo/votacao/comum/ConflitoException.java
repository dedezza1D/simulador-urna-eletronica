package com.estudo.votacao.comum;

/** O pedido é válido, mas conflita com o estado atual (duplicidade, recurso em uso). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
