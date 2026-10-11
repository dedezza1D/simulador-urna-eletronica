package com.estudo.votacao.eleicao;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Eleicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;
    private int ano;
    private int turno;

    protected Eleicao() {
    }

    public Eleicao(String descricao, int ano, int turno) {
        this.descricao = descricao;
        this.ano = ano;
        this.turno = turno;
    }

    public Long getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getAno() {
        return ano;
    }

    public int getTurno() {
        return turno;
    }
}
