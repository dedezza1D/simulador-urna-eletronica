package com.estudo.votacao.urna;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EleitorRepository extends JpaRepository<Eleitor, Long> {

    Optional<Eleitor> findByTitulo(String titulo);

    @Query(value = """
            select new com.estudo.votacao.urna.EleitorCadernoResponse(
                       e.titulo, e.nome, case when c.id is null then false else true end)
            from Eleitor e
            left join Comparecimento c on c.eleitor = e and c.urna.id = :urnaId
            where e.secao.id = :secaoId
              and (:somentePendentes = false or c.id is null)
            """,
            countQuery = """
            select count(e)
            from Eleitor e
            left join Comparecimento c on c.eleitor = e and c.urna.id = :urnaId
            where e.secao.id = :secaoId
              and (:somentePendentes = false or c.id is null)
            """)
    Page<EleitorCadernoResponse> caderno(@Param("secaoId") Long secaoId,
                                         @Param("urnaId") Long urnaId,
                                         @Param("somentePendentes") boolean somentePendentes,
                                         Pageable pageable);
}
