package com.estudo.votacao.urna;

/** Linha do caderno de votação: quem é o eleitor e se já votou. Nunca contém o voto. */
public record EleitorCadernoResponse(String titulo, String nome, boolean votou) {
}
