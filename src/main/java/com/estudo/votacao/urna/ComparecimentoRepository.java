package com.estudo.votacao.urna;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComparecimentoRepository extends JpaRepository<Comparecimento, Long> {

    boolean existsByUrnaIdAndEleitorId(Long urnaId, Long eleitorId);

    long countByUrnaId(Long urnaId);
}
