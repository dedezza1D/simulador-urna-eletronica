package com.estudo.votacao.urna;

import com.estudo.votacao.secao.Secao;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Eleitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "secao_id")
    private Secao secao;

    protected Eleitor() {
    }

    public Eleitor(String titulo, String nome, Secao secao) {
        this.titulo = titulo;
        this.nome = nome;
        this.secao = secao;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getNome() {
        return nome;
    }

    public Secao getSecao() {
        return secao;
    }
}
