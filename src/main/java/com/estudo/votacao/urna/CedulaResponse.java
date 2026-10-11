package com.estudo.votacao.urna;

import java.util.List;

import com.estudo.votacao.eleicao.Cargo;

/** O que o terminal do eleitor precisa para montar as telas: cargos na ordem da urna e candidatos. */
public record CedulaResponse(List<CargoNaUrna> cargos) {

    public record CargoNaUrna(Cargo cargo, int digitos, List<CandidatoNaUrna> candidatos) {
    }

    public record CandidatoNaUrna(String numero, String nome, String partido) {
    }
}
