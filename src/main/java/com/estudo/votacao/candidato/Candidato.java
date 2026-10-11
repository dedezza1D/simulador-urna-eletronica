package com.estudo.votacao.candidato;

import com.estudo.votacao.eleicao.Cargo;
import com.estudo.votacao.eleicao.Eleicao;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Candidato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleicao_id")
    private Eleicao eleicao;

    @Enumerated(EnumType.STRING)
    private Cargo cargo;

    private String numero;
    private String nome;
    private String partido;

    protected Candidato() {
    }

    public Candidato(Eleicao eleicao, Cargo cargo, String numero, String nome, String partido) {
        this.eleicao = eleicao;
        this.cargo = cargo;
        this.numero = numero;
        this.nome = nome;
        this.partido = partido;
    }

    public void atualizar(Cargo cargo, String numero, String nome, String partido) {
        this.cargo = cargo;
        this.numero = numero;
        this.nome = nome;
        this.partido = partido;
    }

    public Long getId() {
        return id;
    }

    public Eleicao getEleicao() {
        return eleicao;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public String getNumero() {
        return numero;
    }

    public String getNome() {
        return nome;
    }

    public String getPartido() {
        return partido;
    }
}
