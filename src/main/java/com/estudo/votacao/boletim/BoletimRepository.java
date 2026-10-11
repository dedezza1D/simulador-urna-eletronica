package com.estudo.votacao.boletim;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletimRepository extends JpaRepository<BoletimUrna, Long> {

    Optional<BoletimUrna> findByEleicaoIdAndSecaoId(Long eleicaoId, Long secaoId);

    boolean existsByEleicaoIdAndSecaoId(Long eleicaoId, Long secaoId);

    long countByEleicaoId(Long eleicaoId);
}
