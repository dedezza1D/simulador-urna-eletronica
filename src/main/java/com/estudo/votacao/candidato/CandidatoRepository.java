package com.estudo.votacao.candidato;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estudo.votacao.eleicao.Cargo;

public interface CandidatoRepository extends JpaRepository<Candidato, Long> {

    @Query("""
            select c from Candidato c
            where (:eleicaoId is null or c.eleicao.id = :eleicaoId)
              and (:cargo is null or c.cargo = :cargo)
            """)
    Page<Candidato> buscar(@Param("eleicaoId") Long eleicaoId, @Param("cargo") Cargo cargo, Pageable pageable);

    List<Candidato> findByEleicaoId(Long eleicaoId);

    boolean existsByEleicaoIdAndCargoAndNumero(Long eleicaoId, Cargo cargo, String numero);

    boolean existsByEleicaoIdAndCargoAndNumeroAndIdNot(Long eleicaoId, Cargo cargo, String numero, Long id);
}
