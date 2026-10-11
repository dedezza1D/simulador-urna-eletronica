package com.estudo.votacao.boletim;

import com.estudo.votacao.eleicao.Cargo;

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
public class BoletimCargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boletim_id")
    private BoletimUrna boletim;

    @Enumerated(EnumType.STRING)
    private Cargo cargo;

    private int brancos;
    private int nulos;

    protected BoletimCargo() {
    }

    BoletimCargo(BoletimUrna boletim, Cargo cargo, int brancos, int nulos) {
        this.boletim = boletim;
        this.cargo = cargo;
        this.brancos = brancos;
        this.nulos = nulos;
    }

    public Long getId() {
        return id;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public int getBrancos() {
        return brancos;
    }

    public int getNulos() {
        return nulos;
    }
}
