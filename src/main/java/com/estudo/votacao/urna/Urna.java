package com.estudo.votacao.urna;

import java.time.Instant;

import com.estudo.votacao.comum.ConflitoException;
import com.estudo.votacao.comum.RegraNegocioException;
import com.estudo.votacao.eleicao.Eleicao;
import com.estudo.votacao.secao.Secao;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

/** Urna de uma seção. Só um eleitor por vez pode estar habilitado. */
@Entity
public class Urna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleicao_id")
    private Eleicao eleicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "secao_id")
    private Secao secao;

    @Enumerated(EnumType.STRING)
    private EstadoUrna estado;

    /** Chave pública (X.509, Base64) usada pelo totalizador para verificar o BU. */
    private String chavePublica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eleitor_habilitado_id")
    private Eleitor eleitorHabilitado;

    private Instant abertaEm;
    private Instant encerradaEm;

    @Lob
    private String buConteudo;
    private String buAssinatura;

    // Lock otimista: duas habilitações simultâneas na mesma urna não podem ambas vencer.
    @Version
    private long versao;

    protected Urna() {
    }

    public Urna(Eleicao eleicao, Secao secao, String chavePublica, Instant abertaEm) {
        this.eleicao = eleicao;
        this.secao = secao;
        this.chavePublica = chavePublica;
        this.abertaEm = abertaEm;
        this.estado = EstadoUrna.ABERTA;
    }

    public void habilitar(Eleitor eleitor) {
        exigirAberta();
        if (eleitorHabilitado != null) {
            throw new ConflitoException("Já existe um eleitor habilitado. Aguarde o fim do voto ou cancele a habilitação.");
        }
        eleitorHabilitado = eleitor;
    }

    public void cancelarHabilitacao() {
        exigirAberta();
        if (eleitorHabilitado == null) {
            throw new RegraNegocioException("Nenhum eleitor habilitado.");
        }
        eleitorHabilitado = null;
    }

    void concluirVoto() {
        eleitorHabilitado = null;
    }

    public void exigirPodeEncerrar() {
        exigirAberta();
        if (eleitorHabilitado != null) {
            throw new ConflitoException("Há um eleitor votando. Conclua ou cancele a habilitação antes de encerrar.");
        }
    }

    void encerrar(Instant agora, String conteudo, String assinatura) {
        exigirPodeEncerrar();
        this.estado = EstadoUrna.ENCERRADA;
        this.encerradaEm = agora;
        this.buConteudo = conteudo;
        this.buAssinatura = assinatura;
    }

    public void exigirAberta() {
        if (estado != EstadoUrna.ABERTA) {
            throw new RegraNegocioException("A urna não está aberta (estado atual: " + estado + ").");
        }
    }

    public void exigirEncerrada() {
        if (estado != EstadoUrna.ENCERRADA) {
            throw new RegraNegocioException("Disponível apenas após o encerramento da votação.");
        }
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

    public EstadoUrna getEstado() {
        return estado;
    }

    public String getChavePublica() {
        return chavePublica;
    }

    public Eleitor getEleitorHabilitado() {
        return eleitorHabilitado;
    }

    public Instant getAbertaEm() {
        return abertaEm;
    }

    public Instant getEncerradaEm() {
        return encerradaEm;
    }

    public String getBuConteudo() {
        return buConteudo;
    }

    public String getBuAssinatura() {
        return buAssinatura;
    }
}
