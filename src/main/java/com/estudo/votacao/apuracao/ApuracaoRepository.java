package com.estudo.votacao.apuracao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.estudo.votacao.boletim.VotoCandidato;
import com.estudo.votacao.eleicao.Cargo;

/** Consultas de leitura da apuração. Projeções em records: o banco agrega, a aplicação não carrega entidades. */
public interface ApuracaoRepository extends Repository<VotoCandidato, Long> {

    // LEFT JOIN para que candidatos sem nenhum voto também apareçam (com zero).
    @Query("""
            select new com.estudo.votacao.apuracao.TotalCandidato(
                       c.numero, c.nome, c.partido, coalesce(sum(v.votos), 0L))
            from Candidato c
            left join VotoCandidato v on v.candidato = c
            where c.eleicao.id = :eleicaoId and c.cargo = :cargo
            group by c.id, c.numero, c.nome, c.partido
            """)
    List<TotalCandidato> totalizarPorCandidato(@Param("eleicaoId") Long eleicaoId, @Param("cargo") Cargo cargo);

    @Query("""
            select new com.estudo.votacao.apuracao.TotaisNaoNominais(
                       coalesce(sum(bc.brancos), 0L), coalesce(sum(bc.nulos), 0L))
            from BoletimCargo bc
            where bc.boletim.eleicao.id = :eleicaoId and bc.cargo = :cargo
            """)
    TotaisNaoNominais totalizarNaoNominais(@Param("eleicaoId") Long eleicaoId, @Param("cargo") Cargo cargo);

    boolean existsByCandidatoId(Long candidatoId);
}
