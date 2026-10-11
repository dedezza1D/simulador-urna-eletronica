package com.estudo.votacao.boletim;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.estudo.votacao.candidato.Candidato;
import com.estudo.votacao.eleicao.Cargo;
import com.estudo.votacao.eleicao.Eleicao;
import com.estudo.votacao.secao.Secao;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/** Raiz do agregado: totais por cargo e votos por candidato só existem dentro do boletim. */
@Entity
public class BoletimUrna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleicao_id")
    private Eleicao eleicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "secao_id")
    private Secao secao;

    private int comparecimento;
    private Instant recebidoEm;

    /** SHA-256 do conteúdo assinado pela urna. Nulo quando o BU chegou sem assinatura. */
    private String hashConteudo;

    @OneToMany(mappedBy = "boletim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoletimCargo> cargos = new ArrayList<>();

    @OneToMany(mappedBy = "boletim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VotoCandidato> votos = new ArrayList<>();

    protected BoletimUrna() {
    }

    public BoletimUrna(Eleicao eleicao, Secao secao, int comparecimento, Instant recebidoEm, String hashConteudo) {
        this.eleicao = eleicao;
        this.secao = secao;
        this.comparecimento = comparecimento;
        this.recebidoEm = recebidoEm;
        this.hashConteudo = hashConteudo;
    }

    public void registrarCargo(Cargo cargo, int brancos, int nulos) {
        cargos.add(new BoletimCargo(this, cargo, brancos, nulos));
    }

    public void registrarVoto(Candidato candidato, int quantidade) {
        votos.add(new VotoCandidato(this, candidato, quantidade));
    }

    public Long getId() {
        return id;
    }

    public Eleicao getEleicao() {
        return eleicao;
    }

    public Secao getSecao() {
        return secao;
    }

    public int getComparecimento() {
        return comparecimento;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }

    public String getHashConteudo() {
        return hashConteudo;
    }

    public List<BoletimCargo> getCargos() {
        return Collections.unmodifiableList(cargos);
    }

    public List<VotoCandidato> getVotos() {
        return Collections.unmodifiableList(votos);
    }
}
