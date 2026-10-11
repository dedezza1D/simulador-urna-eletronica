package com.estudo.votacao.candidato;

import com.estudo.votacao.eleicao.Cargo;

public record CandidatoResponse(Long id, Long eleicaoId, Cargo cargo, String numero, String nome, String partido) {

    public static CandidatoResponse de(Candidato candidato) {
        return new CandidatoResponse(
                candidato.getId(),
                candidato.getEleicao().getId(),
                candidato.getCargo(),
                candidato.getNumero(),
                candidato.getNome(),
                candidato.getPartido());
    }
}
