package com.estudo.votacao.urna;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UrnaRepository extends JpaRepository<Urna, Long> {

    Optional<Urna> findByEleicaoIdAndSecaoZonaAndSecaoNumero(Long eleicaoId, int zona, int numero);

    boolean existsByEleicaoIdAndSecaoId(Long eleicaoId, Long secaoId);
}
