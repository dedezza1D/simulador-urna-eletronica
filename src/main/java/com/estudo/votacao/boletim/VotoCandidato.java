package com.estudo.votacao.boletim;

import com.estudo.votacao.candidato.Candidato;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class VotoCandidato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boletim_id")
    private BoletimUrna boletim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidato_id")
    private Candidato candidato;

    private int votos;

    protected VotoCandidato() {
    }

    VotoCandidato(BoletimUrna boletim, Candidato candidato, int votos) {
        this.boletim = boletim;
        this.candidato = candidato;
        this.votos = votos;
    }

    public Long getId() {
        return id;
    }

    public Candidato getCandidato() {
        return candidato;
    }

    public int getVotos() {
        return votos;
    }
}
