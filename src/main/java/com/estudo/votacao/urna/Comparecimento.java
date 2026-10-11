package com.estudo.votacao.urna;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Registro de que o eleitor votou nesta urna. Não guarda horário nem voto. */
@Entity
public class Comparecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "urna_id")
    private Urna urna;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleitor_id")
    private Eleitor eleitor;

    protected Comparecimento() {
    }

    public Comparecimento(Urna urna, Eleitor eleitor) {
        this.urna = urna;
        this.eleitor = eleitor;
    }

    public Long getId() {
        return id;
    }
}
