package com.estudo.votacao.secao;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Secao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int zona;
    private int numero;
    private String municipio;
    private String uf;
    private int eleitoresAptos;

    protected Secao() {
    }

    public Secao(int zona, int numero, String municipio, String uf, int eleitoresAptos) {
        this.zona = zona;
        this.numero = numero;
        this.municipio = municipio;
        this.uf = uf;
        this.eleitoresAptos = eleitoresAptos;
    }

    public Long getId() {
        return id;
    }

    public int getZona() {
        return zona;
    }

    public int getNumero() {
        return numero;
    }

    public String getMunicipio() {
        return municipio;
    }

    public String getUf() {
        return uf;
    }

    public int getEleitoresAptos() {
        return eleitoresAptos;
    }
}
