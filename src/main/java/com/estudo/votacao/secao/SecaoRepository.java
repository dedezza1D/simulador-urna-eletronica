package com.estudo.votacao.secao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SecaoRepository extends JpaRepository<Secao, Long> {

    Optional<Secao> findByZonaAndNumero(int zona, int numero);
}
